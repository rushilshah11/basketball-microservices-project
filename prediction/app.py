from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Optional
import torch
import torch.nn as nn
import numpy as np
import logging
import py_eureka_client.eureka_client as eureka_client
import os
import redis
import threading
import json
from sqlalchemy.orm import Session

# Import our new modules
from database import init_db, get_db, TrainingMetadata
from cache import PredictionCache, test_redis_connection
from prediction_service import PredictionService
from stats_client import stats_client
from data_collector import DataCollector
from model_trainer import ModelTrainer

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("prediction-service")

app = FastAPI(title="Basketball Prediction Service")

# ============================================
# Neural Network Model
# ============================================

class PlayerPerformanceNet(nn.Module):
    """
    Simple feedforward neural network for player performance prediction.
    Can be extended to GNN in the future.
    """
    def __init__(self, input_size=10, hidden_size=64, output_size=3):
        super(PlayerPerformanceNet, self).__init__()
        self.fc1 = nn.Linear(input_size, hidden_size)
        self.relu1 = nn.ReLU()
        self.dropout1 = nn.Dropout(0.3)
        self.fc2 = nn.Linear(hidden_size, hidden_size // 2)
        self.relu2 = nn.ReLU()
        self.dropout2 = nn.Dropout(0.3)
        self.fc3 = nn.Linear(hidden_size // 2, output_size)
        
    def forward(self, x):
        x = self.fc1(x)
        x = self.relu1(x)
        x = self.dropout1(x)
        x = self.fc2(x)
        x = self.relu2(x)
        x = self.dropout2(x)
        x = self.fc3(x)
        return x

# ============================================
# Model Initialization
# ============================================

# Initialize model
model = PlayerPerformanceNet()
model.eval()  # Set to evaluation mode

logger.info("Neural network model initialized")

# Initialize prediction service and trainer
prediction_service = PredictionService(model)
model_trainer = ModelTrainer(model)

# ============================================
# Request/Response Models
# ============================================

class PlayerStats(BaseModel):
    """Input features for prediction"""
    ppg: float  # Points per game
    apg: float  # Assists per game
    rpg: float  # Rebounds per game
    fgPct: Optional[float] = 0.45  # Field goal percentage
    ftPct: Optional[float] = 0.75  # Free throw percentage
    gamesPlayed: int
    minutesPerGame: Optional[float] = 30.0
    stealsPerGame: Optional[float] = 1.0
    blocksPerGame: Optional[float] = 0.5
    turnoversPerGame: Optional[float] = 2.0

class PredictionRequest(BaseModel):
    playerName: str
    currentStats: PlayerStats
    opponent: Optional[str] = "Average"
    homeGame: bool = True

class PredictionResponse(BaseModel):
    playerName: str
    predictedPoints: float
    predictedAssists: float
    predictedRebounds: float
    confidence: float
    model: str = "basic_nn"

class BatchPredictionRequest(BaseModel):
    predictions: List[PredictionRequest]

# ============================================
# Helper Functions
# ============================================

def normalize_stats(stats: PlayerStats) -> np.ndarray:
    """
    Normalize player stats for model input.
    In production, use pre-computed mean/std from training data.
    """
    features = np.array([
        stats.ppg / 30.0,  # Normalize to typical max
        stats.apg / 10.0,
        stats.rpg / 12.0,
        stats.fgPct,
        stats.ftPct,
        stats.gamesPlayed / 82.0,  # NBA season games
        stats.minutesPerGame / 48.0,  # Max game minutes
        stats.stealsPerGame / 3.0,
        stats.blocksPerGame / 3.0,
        stats.turnoversPerGame / 5.0
    ])
    return features

def predict_performance(stats: PlayerStats, home_game: bool = True) -> tuple:
    """
    Predict player performance using the neural network.
    Returns: (predicted_points, predicted_assists, predicted_rebounds, confidence)
    """
    try:
        # Normalize input
        features = normalize_stats(stats)
        
        # Add home/away factor
        home_factor = 1.05 if home_game else 0.95
        
        # Convert to tensor
        x = torch.tensor(features, dtype=torch.float32).unsqueeze(0)
        
        # Make prediction
        with torch.no_grad():
            output = model(x)
            predictions = output.squeeze().numpy()
        
        # Apply home factor and denormalize
        predicted_points = float(predictions[0] * 30.0 * home_factor)
        predicted_assists = float(predictions[1] * 10.0 * home_factor)
        predicted_rebounds = float(predictions[2] * 12.0 * home_factor)
        
        # Simple confidence based on current form
        confidence = min(0.95, 0.7 + (stats.gamesPlayed / 82.0) * 0.25)
        
        return predicted_points, predicted_assists, predicted_rebounds, confidence
        
    except Exception as e:
        logger.error(f"Prediction error: {e}")
        raise

# 1. Setup Redis Connection for Events (db=0)
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
redis_event_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0)

# 2. Define the Subscriber Logic
def redis_listener():
    """
    Listen to watchlist events and trigger predictions
    When a player is added to watchlist, generate and store prediction
    """
    pubsub = redis_event_client.pubsub()
    pubsub.subscribe('watchlist-events')

    logger.info(f"🎧 Listening for events on 'watchlist-events' at {REDIS_HOST}:{REDIS_PORT}...")

    for message in pubsub.listen():
        if message['type'] == 'message':
            try:
                data = json.loads(message['data'])
                event_type = data.get('eventType')
                player_name = data.get('playerName')

                logger.info(f"📨 Received Event: {event_type} for {player_name}")

                if event_type == 'PLAYER_ADDED':
                    # Trigger prediction generation and storage
                    logger.info(f"🚀 Triggering background prediction for {player_name}")
                    
                    # Run async prediction in background
                    import asyncio
                    from database import SessionLocal
                    
                    try:
                        # Create new event loop for this thread
                        loop = asyncio.new_event_loop()
                        asyncio.set_event_loop(loop)
                        
                        # Get database session
                        db = SessionLocal()
                        
                        # Generate prediction
                        result = loop.run_until_complete(
                            prediction_service.generate_prediction_for_player(
                                player_name=player_name,
                                db=db,
                                force_refresh=True
                            )
                        )
                        
                        if result:
                            logger.info(f"✅ Successfully generated prediction for {player_name}")
                        else:
                            logger.warning(f"⚠️ Could not generate prediction for {player_name}")
                        
                        db.close()
                        loop.close()
                        
                    except Exception as e:
                        logger.error(f"❌ Error generating prediction in background: {e}")

            except Exception as e:
                logger.error(f"❌ Error processing Redis message: {e}")

# ============================================
# API Endpoints
# ============================================

# 3. Start Listener in Background Thread on Startup
@app.on_event("startup")
async def startup_event():
    """Initialize database, cache, and start Redis listener"""
    
    # Initialize database
    logger.info("🔧 Initializing database...")
    init_db()
    
    # Try to load existing trained model
    logger.info("🔧 Loading trained model (if available)...")
    if model_trainer.load_model():
        logger.info("✅ Loaded pre-trained model")
    else:
        logger.info("⚠️ No pre-trained model found - using randomly initialized model")
    
    # Test Redis connection
    logger.info("🔧 Testing Redis connection...")
    test_redis_connection()
    
    # Start Redis listener in background thread
    listener_thread = threading.Thread(target=redis_listener, daemon=True)
    listener_thread.start()
    logger.info("✅ Redis listener started")

    # Register with Eureka
    await eureka_client.init_async(
        eureka_server="http://eureka-server:8761/eureka",
        app_name="PREDICTION-SERVICE",
        instance_port=5002
    )
    logger.info("✅ Registered with Eureka")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    await stats_client.close()
    logger.info("✅ Shutdown complete")

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {
        "status": "UP",
        "service": "prediction-service",
        "model": "PlayerPerformanceNet",
        "framework": "PyTorch"
    }

@app.post("/predict", response_model=PredictionResponse)
def predict_player_performance(request: PredictionRequest):
    """
    Predict player performance for next game.
    """
    logger.info(f"Prediction request for: {request.playerName}")
    
    try:
        predicted_pts, predicted_ast, predicted_reb, confidence = predict_performance(
            request.currentStats,
            request.homeGame
        )
        
        return PredictionResponse(
            playerName=request.playerName,
            predictedPoints=round(predicted_pts, 1),
            predictedAssists=round(predicted_ast, 1),
            predictedRebounds=round(predicted_reb, 1),
            confidence=round(confidence, 2)
        )
        
    except Exception as e:
        logger.error(f"Error predicting for {request.playerName}: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/predict/batch")
def predict_batch(request: BatchPredictionRequest):
    """
    Batch prediction for multiple players.
    """
    logger.info(f"Batch prediction for {len(request.predictions)} players")
    
    results = []
    for pred_req in request.predictions:
        try:
            predicted_pts, predicted_ast, predicted_reb, confidence = predict_performance(
                pred_req.currentStats,
                pred_req.homeGame
            )
            
            results.append(PredictionResponse(
                playerName=pred_req.playerName,
                predictedPoints=round(predicted_pts, 1),
                predictedAssists=round(predicted_ast, 1),
                predictedRebounds=round(predicted_reb, 1),
                confidence=round(confidence, 2)
            ))
        except Exception as e:
            logger.error(f"Error in batch prediction for {pred_req.playerName}: {e}")
            # Continue with other predictions
            
    return results

@app.get("/model/info")
def model_info():
    """
    Get information about the current model.
    """
    return {
        "modelType": "FeedForward Neural Network",
        "framework": "PyTorch",
        "inputFeatures": 10,
        "outputFeatures": 3,
        "hiddenLayers": 2,
        "parameters": sum(p.numel() for p in model.parameters()),
        "futureEnhancements": ["GNN", "Attention Mechanism", "Player Embeddings"]
    }

@app.get("/predictions/{player_name}")
async def get_player_prediction(player_name: str, db: Session = Depends(get_db)):
    """
    Get stored prediction for a player (from cache or database)
    If not found, generates a new prediction
    """
    logger.info(f"📊 Fetching prediction for: {player_name}")
    
    try:
        # Try to get stored prediction
        prediction = prediction_service.get_stored_prediction(player_name, db)
        
        if prediction:
            logger.info(f"✅ Found stored prediction for {player_name}")
            return prediction
        
        # If not found, generate new prediction
        logger.info(f"🔄 Generating new prediction for {player_name}")
        prediction = await prediction_service.generate_prediction_for_player(
            player_name=player_name,
            db=db,
            force_refresh=False
        )
        
        if prediction:
            return prediction
        else:
            raise HTTPException(
                status_code=404,
                detail=f"Could not generate prediction for {player_name}. Stats may be unavailable."
            )
    
    except Exception as e:
        logger.error(f"Error fetching prediction for {player_name}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/predictions/{player_name}/refresh")
async def refresh_player_prediction(player_name: str, db: Session = Depends(get_db)):
    """
    Force refresh prediction for a player
    Invalidates cache and fetches fresh data
    """
    logger.info(f"🔄 Force refreshing prediction for: {player_name}")
    
    try:
        # Invalidate cache
        PredictionCache.delete(player_name)
        
        # Generate new prediction
        prediction = await prediction_service.generate_prediction_for_player(
            player_name=player_name,
            db=db,
            force_refresh=True
        )
        
        if prediction:
            return {
                "message": f"Prediction refreshed for {player_name}",
                "prediction": prediction
            }
        else:
            raise HTTPException(
                status_code=404,
                detail=f"Could not refresh prediction for {player_name}. Stats may be unavailable."
            )
    
    except Exception as e:
        logger.error(f"Error refreshing prediction for {player_name}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/predictions")
async def get_all_predictions(limit: int = 50, db: Session = Depends(get_db)):
    """
    Get all stored predictions (most recent first)
    """
    logger.info(f"📋 Fetching all predictions (limit: {limit})")
    
    try:
        predictions = prediction_service.get_all_predictions(db, limit=limit)
        return {
            "count": len(predictions),
            "predictions": predictions
        }
    except Exception as e:
        logger.error(f"Error fetching all predictions: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/cache/invalidate")
async def invalidate_cache():
    """
    Clear all prediction caches
    Useful for maintenance or when you want fresh predictions
    """
    logger.info("🗑️ Invalidating all prediction caches")
    
    try:
        success = PredictionCache.invalidate_all()
        if success:
            return {"message": "All prediction caches invalidated"}
        else:
            raise HTTPException(status_code=500, detail="Failed to invalidate cache")
    except Exception as e:
        logger.error(f"Error invalidating cache: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/training/collect/{player_name}")
async def collect_player_data(player_name: str, limit: int = 10, db: Session = Depends(get_db)):
    """
    Collect and store game logs for a specific player
    This builds the training dataset
    """
    logger.info(f"📥 Collecting game data for: {player_name}")
    
    try:
        stored_count = await DataCollector.collect_and_store_game_logs(
            player_name=player_name,
            db=db,
            limit=limit
        )
        
        return {
            "message": f"Collected data for {player_name}",
            "games_stored": stored_count,
            "player_name": player_name
        }
    except Exception as e:
        logger.error(f"Error collecting data: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/training/train")
async def train_model(
    epochs: int = 50,
    batch_size: int = 32,
    learning_rate: float = 0.001,
    db: Session = Depends(get_db)
):
    """
    Train the model on collected game data
    Requires sufficient training data in the database
    """
    logger.info(f"🎓 Starting model training (epochs={epochs})")
    
    try:
        # Get training data
        game_logs = DataCollector.get_player_training_data(db)
        
        if len(game_logs) < 50:
            return {
                "error": "Insufficient training data",
                "message": f"Need at least 50 game logs, found {len(game_logs)}",
                "suggestion": "Use /training/collect/{player_name} to collect more data"
            }
        
        # Train model
        result = model_trainer.train(
            game_logs=game_logs,
            db=db,
            epochs=epochs,
            batch_size=batch_size,
            learning_rate=learning_rate
        )
        
        return result
        
    except Exception as e:
        logger.error(f"Error training model: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/training/status")
async def get_training_status(db: Session = Depends(get_db)):
    """
    Get status of training data and recent training runs
    """
    try:
        # Get data statistics
        data_stats = DataCollector.get_training_stats(db)
        
        # Get recent training runs
        recent_trainings = db.query(TrainingMetadata).order_by(
            TrainingMetadata.started_at.desc()
        ).limit(5).all()
        
        return {
            "data_stats": data_stats,
            "recent_trainings": [t.to_dict() for t in recent_trainings],
            "model_status": "trained" if model_trainer.load_model() else "untrained"
        }
    except Exception as e:
        logger.error(f"Error getting training status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
def root():
    """Root endpoint"""
    return {
        "service": "Basketball Prediction Service",
        "version": "3.0.0",
        "features": [
            "Neural Network Predictions",
            "Redis Caching",
            "PostgreSQL Storage",
            "Stats Service Integration",
            "Watchlist Event Listener",
            "Model Training & Persistence",
            "Historical Game Data Collection"
        ],
        "endpoints": {
            "predict": "/predict",
            "batch": "/predict/batch",
            "get_prediction": "/predictions/{player_name}",
            "refresh_prediction": "/predictions/{player_name}/refresh",
            "all_predictions": "/predictions",
            "invalidate_cache": "/cache/invalidate",
            "collect_data": "/training/collect/{player_name}",
            "train_model": "/training/train",
            "training_status": "/training/status",
            "health": "/health",
            "info": "/model/info"
        }
    }

if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=5002)

