"""
Prediction service - orchestrates stats fetching, prediction generation, and storage
"""
import logging
from typing import Optional, Dict
from sqlalchemy.orm import Session
from datetime import datetime

from database import PlayerPrediction
from cache import PredictionCache
from stats_client import stats_client
from data_collector import DataCollector
from pydantic import BaseModel

logger = logging.getLogger("prediction-service")


class PlayerStats(BaseModel):
    """Input features for prediction"""
    ppg: float
    apg: float
    rpg: float
    fgPct: float = 0.45
    ftPct: float = 0.75
    gamesPlayed: int
    minutesPerGame: float = 30.0
    stealsPerGame: float = 1.0
    blocksPerGame: float = 0.5
    turnoversPerGame: float = 2.0


class PredictionService:
    """Main service for generating and managing predictions"""
    
    def __init__(self, model):
        """Initialize with the neural network model"""
        self.model = model
    
    async def generate_prediction_for_player(
        self,
        player_name: str,
        db: Session,
        force_refresh: bool = False,
        collect_training_data: bool = True
    ) -> Optional[Dict]:
        """
        Main function to generate prediction for a player
        
        1. Check cache first (unless force_refresh)
        2. Fetch stats from Stats Service
        3. **Collect training data (game logs) in background**
        4. Handle empty response (fallback case)
        5. Generate prediction using ML model
        6. Store in database
        7. Cache the result
        8. Return prediction
        """
        
        # Step 1: Check cache
        if not force_refresh:
            cached = PredictionCache.get(player_name)
            if cached:
                logger.info(f"📦 Returning cached prediction for {player_name}")
                return cached
        
        # Step 2: Fetch stats from Stats Service
        stats_data = await stats_client.get_player_stats(player_name)
        
        # Step 2b: Collect training data in background (don't wait for it)
        if collect_training_data:
            try:
                import asyncio
                # Fire and forget - collect game logs for future training
                asyncio.create_task(
                    DataCollector.collect_and_store_game_logs(player_name, db, limit=10)
                )
                logger.info(f"📥 Triggered background data collection for {player_name}")
            except Exception as e:
                logger.warning(f"⚠️ Could not trigger data collection: {e}")
        
        # Step 3: Handle fallback case (empty response)
        if not stats_data:
            logger.warning(f"⚠️ Could not fetch stats for {player_name}. Skipping prediction.")
            return None
        
        # Step 4: Convert stats to model input format
        try:
            player_stats = self._convert_stats_to_model_input(stats_data)
        except Exception as e:
            logger.error(f"❌ Error converting stats for {player_name}: {e}")
            return None
        
        # Step 5: Generate prediction using ML model
        try:
            predicted_pts, predicted_ast, predicted_reb, confidence = self._predict_performance(
                player_stats,
                home_game=True  # Default to home game
            )
        except Exception as e:
            logger.error(f"❌ Error generating prediction for {player_name}: {e}")
            return None
        
        # Step 6: Prepare prediction data
        prediction_data = {
            "player_name": player_name,
            "predicted_stats": {
                "pts": round(predicted_pts, 1),
                "ast": round(predicted_ast, 1),
                "reb": round(predicted_reb, 1)
            },
            "confidence": round(confidence, 2),
            "created_at": datetime.utcnow().isoformat()
        }
        
        # Step 7: Store in database
        self._save_to_database(db, prediction_data)
        
        # Step 8: Cache the result
        PredictionCache.set(player_name, prediction_data)
        
        logger.info(f"✅ Generated and stored prediction for {player_name}")
        return prediction_data
    
    def _convert_stats_to_model_input(self, stats_data: Dict) -> PlayerStats:
        """
        Convert Stats Service response to PlayerStats model
        Stats Service returns: { season, gamesPlayed, ppg, apg, rpg }
        """
        return PlayerStats(
            ppg=stats_data.get("ppg", 0.0),
            apg=stats_data.get("apg", 0.0),
            rpg=stats_data.get("rpg", 0.0),
            gamesPlayed=stats_data.get("gamesPlayed", 0),
            # Use defaults for fields not provided by Stats Service
            fgPct=0.45,
            ftPct=0.75,
            minutesPerGame=30.0,
            stealsPerGame=1.0,
            blocksPerGame=0.5,
            turnoversPerGame=2.0
        )
    
    def _predict_performance(self, stats: PlayerStats, home_game: bool = True) -> tuple:
        """
        Predict player performance using the neural network
        Returns: (predicted_points, predicted_assists, predicted_rebounds, confidence)
        """
        import torch
        import numpy as np
        
        # Normalize input
        features = np.array([
            stats.ppg / 30.0,
            stats.apg / 10.0,
            stats.rpg / 12.0,
            stats.fgPct,
            stats.ftPct,
            stats.gamesPlayed / 82.0,
            stats.minutesPerGame / 48.0,
            stats.stealsPerGame / 3.0,
            stats.blocksPerGame / 3.0,
            stats.turnoversPerGame / 5.0
        ])
        
        # Add home/away factor
        home_factor = 1.05 if home_game else 0.95
        
        # Convert to tensor
        x = torch.tensor(features, dtype=torch.float32).unsqueeze(0)
        
        # Make prediction
        with torch.no_grad():
            output = self.model(x)
            predictions = output.squeeze().numpy()
        
        # Apply home factor and denormalize
        predicted_points = float(predictions[0] * 30.0 * home_factor)
        predicted_assists = float(predictions[1] * 10.0 * home_factor)
        predicted_rebounds = float(predictions[2] * 12.0 * home_factor)
        
        # Simple confidence based on current form
        confidence = min(0.95, 0.7 + (stats.gamesPlayed / 82.0) * 0.25)
        
        return predicted_points, predicted_assists, predicted_rebounds, confidence
    
    def _save_to_database(self, db: Session, prediction_data: Dict):
        """Save prediction to PostgreSQL database"""
        try:
            prediction = PlayerPrediction(
                player_name=prediction_data["player_name"],
                predicted_stats=prediction_data["predicted_stats"],
                confidence=prediction_data["confidence"]
            )
            db.add(prediction)
            db.commit()
            db.refresh(prediction)
            logger.info(f"💾 Saved prediction to database (ID: {prediction.id})")
        except Exception as e:
            db.rollback()
            logger.error(f"❌ Error saving to database: {e}")
            raise
    
    def get_stored_prediction(self, player_name: str, db: Session) -> Optional[Dict]:
        """
        Get most recent stored prediction from database
        Also checks cache first
        """
        # Check cache first
        cached = PredictionCache.get(player_name)
        if cached:
            return cached
        
        # Query database for most recent prediction
        try:
            prediction = db.query(PlayerPrediction).filter(
                PlayerPrediction.player_name == player_name
            ).order_by(PlayerPrediction.created_at.desc()).first()
            
            if prediction:
                result = prediction.to_dict()
                # Cache the result
                PredictionCache.set(player_name, result)
                return result
            else:
                return None
        except Exception as e:
            logger.error(f"❌ Error fetching from database: {e}")
            return None
    
    def get_all_predictions(self, db: Session, limit: int = 50) -> list:
        """Get all recent predictions"""
        try:
            predictions = db.query(PlayerPrediction).order_by(
                PlayerPrediction.created_at.desc()
            ).limit(limit).all()
            
            return [p.to_dict() for p in predictions]
        except Exception as e:
            logger.error(f"❌ Error fetching predictions: {e}")
            return []
