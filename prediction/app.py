from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import torch
import torch.nn as nn
import numpy as np
import logging

import os
import redis
import threading
import json

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

# 1. Setup Redis Connection
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0)

# 2. Define the Subscriber Logic
def redis_listener():
    pubsub = redis_client.pubsub()
    pubsub.subscribe('watchlist-events')

    logger.info(f"Listening for events on 'watchlist-events' at {REDIS_HOST}:{REDIS_PORT}...")

    for message in pubsub.listen():
        if message['type'] == 'message':
            try:
                data = json.loads(message['data'])
                event_type = data.get('eventType')
                player_name = data.get('playerName')

                logger.info(f"Received Event: {event_type} for {player_name}")

                if event_type == 'PLAYER_ADDED':
                    # TODO: Trigger a prediction update or pre-cache the prediction here
                    logger.info(f"Triggering background prediction for {player_name}")
                    # You could call your predict logic here and save to DB

            #         ASHWINNNNNNN, ADD THE LOGIC TO RUN PREDICTION MODEL AND STORE IN POSTGRES DB HEREEEEEEEE

            except Exception as e:
                logger.error(f"Error processing Redis message: {e}")

# ============================================
# API Endpoints
# ============================================

# 3. Start Listener in Background Thread on Startup
@app.on_event("startup")
async def startup_event():
    listener_thread = threading.Thread(target=redis_listener, daemon=True)
    listener_thread.start()

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

@app.get("/")
def root():
    """Root endpoint"""
    return {
        "service": "Basketball Prediction Service",
        "version": "1.0.0",
        "endpoints": {
            "predict": "/predict",
            "batch": "/predict/batch",
            "health": "/health",
            "info": "/model/info"
        }
    }

if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=5002)

