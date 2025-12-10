"""
Model training module - trains the neural network on collected data
"""
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
import logging
from typing import List, Dict, Tuple, Optional
from datetime import datetime
import os
from sqlalchemy.orm import Session

from database import TrainingMetadata
from data_collector import DataCollector

logger = logging.getLogger("prediction-service")

# Model save path
MODEL_SAVE_PATH = "/app/models"
MODEL_FILE = "player_performance_net.pth"


class GameDataset(Dataset):
    """PyTorch Dataset for game logs"""
    
    def __init__(self, game_logs: List[Dict]):
        """
        Prepare training data from game logs
        Each sample includes recent game stats to predict next game performance
        """
        self.samples = []
        
        # Group by player
        player_games = {}
        for game in game_logs:
            player = game["player_name"]
            if player not in player_games:
                player_games[player] = []
            player_games[player].append(game)
        
        # Create training samples: use recent N games to predict next game
        for player, games in player_games.items():
            # Sort by date (oldest first)
            games.sort(key=lambda x: x.get("game_date", ""))
            
            # Need at least 2 games (1 for features, 1 for target)
            if len(games) < 2:
                continue
            
            # Use sliding window: each game becomes a target, previous games are features
            for i in range(1, len(games)):
                # Features: average of all previous games
                prev_games = games[:i]
                target_game = games[i]
                
                # Calculate averages from previous games
                avg_pts = np.mean([g["points"] for g in prev_games])
                avg_ast = np.mean([g["assists"] for g in prev_games])
                avg_reb = np.mean([g["rebounds"] for g in prev_games])
                avg_min = np.mean([g["minutes"] for g in prev_games])
                
                # Calculate shooting percentages
                total_fgm = sum([g["fgm"] for g in prev_games])
                total_fga = sum([g["fga"] for g in prev_games])
                total_ftm = sum([g["ftm"] for g in prev_games])
                total_fta = sum([g["fta"] for g in prev_games])
                
                fg_pct = total_fgm / total_fga if total_fga > 0 else 0.45
                ft_pct = total_ftm / total_fta if total_fta > 0 else 0.75
                
                avg_stl = np.mean([g["steals"] for g in prev_games])
                avg_blk = np.mean([g["blocks"] for g in prev_games])
                avg_tov = np.mean([g["turnovers"] for g in prev_games])
                
                # Input features (10 features)
                features = np.array([
                    avg_pts / 30.0,  # Normalized
                    avg_ast / 10.0,
                    avg_reb / 12.0,
                    fg_pct,
                    ft_pct,
                    len(prev_games) / 82.0,  # Games played
                    avg_min / 48.0,
                    avg_stl / 3.0,
                    avg_blk / 3.0,
                    avg_tov / 5.0
                ], dtype=np.float32)
                
                # Target: actual performance in next game (normalized)
                target = np.array([
                    target_game["points"] / 30.0,
                    target_game["assists"] / 10.0,
                    target_game["rebounds"] / 12.0
                ], dtype=np.float32)
                
                self.samples.append((features, target))
        
        logger.info(f"📊 Created {len(self.samples)} training samples from {len(game_logs)} game logs")
    
    def __len__(self):
        return len(self.samples)
    
    def __getitem__(self, idx):
        features, target = self.samples[idx]
        return torch.FloatTensor(features), torch.FloatTensor(target)


class ModelTrainer:
    """Trainer for the neural network model"""
    
    def __init__(self, model):
        self.model = model
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)
        
        # Create models directory if it doesn't exist
        os.makedirs(MODEL_SAVE_PATH, exist_ok=True)
    
    def train(
        self,
        game_logs: List[Dict],
        db: Session,
        epochs: int = 50,
        batch_size: int = 32,
        learning_rate: float = 0.001,
        validation_split: float = 0.2
    ) -> Dict:
        """
        Train the model on collected game data
        
        Returns training results and metadata
        """
        try:
            logger.info(f"🚀 Starting model training with {len(game_logs)} game logs")
            
            # Create training metadata record
            model_version = f"v_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            training_record = TrainingMetadata(
                model_version=model_version,
                training_samples=len(game_logs),
                epochs=epochs,
                status="training",
                started_at=datetime.utcnow()
            )
            db.add(training_record)
            db.commit()
            
            # Create dataset
            dataset = GameDataset(game_logs)
            
            if len(dataset) == 0:
                logger.error("❌ No valid training samples created")
                training_record.status = "failed"
                training_record.notes = "No valid training samples"
                training_record.completed_at = datetime.utcnow()
                db.commit()
                return {"error": "No valid training samples"}
            
            # Split into train/validation
            val_size = int(len(dataset) * validation_split)
            train_size = len(dataset) - val_size
            train_dataset, val_dataset = torch.utils.data.random_split(
                dataset, [train_size, val_size]
            )
            
            train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
            val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False)
            
            # Setup training
            criterion = nn.MSELoss()
            optimizer = optim.Adam(self.model.parameters(), lr=learning_rate)
            
            best_val_loss = float('inf')
            training_history = []
            
            # Training loop
            for epoch in range(epochs):
                # Train
                self.model.train()
                train_loss = 0.0
                for features, targets in train_loader:
                    features = features.to(self.device)
                    targets = targets.to(self.device)
                    
                    optimizer.zero_grad()
                    outputs = self.model(features)
                    loss = criterion(outputs, targets)
                    loss.backward()
                    optimizer.step()
                    
                    train_loss += loss.item()
                
                train_loss /= len(train_loader)
                
                # Validate
                self.model.eval()
                val_loss = 0.0
                with torch.no_grad():
                    for features, targets in val_loader:
                        features = features.to(self.device)
                        targets = targets.to(self.device)
                        
                        outputs = self.model(features)
                        loss = criterion(outputs, targets)
                        val_loss += loss.item()
                
                val_loss /= len(val_loader)
                
                training_history.append({
                    "epoch": epoch + 1,
                    "train_loss": train_loss,
                    "val_loss": val_loss
                })
                
                # Save best model
                if val_loss < best_val_loss:
                    best_val_loss = val_loss
                    self.save_model(model_version)
                
                if (epoch + 1) % 10 == 0:
                    logger.info(f"Epoch {epoch+1}/{epochs} - Train Loss: {train_loss:.4f}, Val Loss: {val_loss:.4f}")
            
            # Update training record
            training_record.status = "completed"
            training_record.training_loss = train_loss
            training_record.validation_loss = best_val_loss
            training_record.completed_at = datetime.utcnow()
            training_record.notes = f"Successfully trained on {len(dataset)} samples"
            db.commit()
            
            logger.info(f"✅ Training completed! Best Val Loss: {best_val_loss:.4f}")
            
            return {
                "status": "success",
                "model_version": model_version,
                "training_samples": len(dataset),
                "epochs": epochs,
                "final_train_loss": train_loss,
                "best_val_loss": best_val_loss,
                "history": training_history[-5:]  # Last 5 epochs
            }
            
        except Exception as e:
            logger.error(f"❌ Training failed: {e}")
            if 'training_record' in locals():
                training_record.status = "failed"
                training_record.notes = str(e)
                training_record.completed_at = datetime.utcnow()
                db.commit()
            return {"error": str(e)}
    
    def save_model(self, version: str):
        """Save model weights to disk"""
        try:
            save_path = os.path.join(MODEL_SAVE_PATH, f"{version}_{MODEL_FILE}")
            torch.save(self.model.state_dict(), save_path)
            
            # Also save as latest
            latest_path = os.path.join(MODEL_SAVE_PATH, MODEL_FILE)
            torch.save(self.model.state_dict(), latest_path)
            
            logger.info(f"💾 Model saved: {save_path}")
        except Exception as e:
            logger.error(f"❌ Error saving model: {e}")
    
    def load_model(self, version: Optional[str] = None):
        """Load model weights from disk"""
        try:
            if version:
                load_path = os.path.join(MODEL_SAVE_PATH, f"{version}_{MODEL_FILE}")
            else:
                load_path = os.path.join(MODEL_SAVE_PATH, MODEL_FILE)
            
            if os.path.exists(load_path):
                self.model.load_state_dict(torch.load(load_path, map_location=self.device))
                self.model.eval()
                logger.info(f"✅ Model loaded from: {load_path}")
                return True
            else:
                logger.warning(f"⚠️ Model file not found: {load_path}")
                return False
        except Exception as e:
            logger.error(f"❌ Error loading model: {e}")
            return False
