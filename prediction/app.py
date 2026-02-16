"""
Basketball Prediction Service - Simple Version
Returns 0.0 for predictions until XGBoost is implemented.
This is the main FastAPI application that handles all prediction requests.
"""
import asyncio
import logging
import os
import requests
from fastapi import FastAPI, HTTPException, Depends
from sqlalchemy.orm import Session
import py_eureka_client.eureka_client as eureka_client
eureka_url = os.getenv('EUREKA_CLIENT_SERVICEURL_DEFAULTZONE')

# Import configuration and models
from config import LOG_LEVEL
from database import init_db, get_db, PlayerPrediction
from cache import PredictionCache, test_redis_connection
from schemas import (
    PredictionRequest, 
    PredictionResponse,
    BatchPredictionRequest,
)

# Setup logging based on LOG_LEVEL from config
logging.basicConfig(level=LOG_LEVEL)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Basketball Prediction Service",
    description="Simple prediction service - placeholder until XGBoost implementation",
    version="1.0.0"
)


# ============================================
# Prediction Logic
# ============================================

def predict_performance(ppg: float, apg: float, rpg: float) -> tuple:
    """
    Predict player performance.
    Currently returns 0.0 placeholder values.
    Will be replaced with XGBoost implementation.
    
    Args:
        ppg: Points per game
        apg: Assists per game
        rpg: Rebounds per game
    
    Returns: 
        Tuple of (predicted_points, predicted_assists, predicted_rebounds, confidence)
    """
    # TODO: Implement XGBoost model here - for now return placeholder values
    return 0.0, 0.0, 0.0, 0.0


# ============================================
# Startup/Shutdown Events
# ============================================

@app.on_event("startup")
async def startup():
    """Called when app starts - initialize database and test cache connection"""
    logger.info("Starting prediction service...")
    init_db()  # Create database tables if they don't exist
    test_redis_connection()  # Test if Redis is accessible
    
    # Register with Eureka service discovery
    asyncio.create_task(register_with_eureka())
    
    logger.info("Service started successfully")


async def register_with_eureka():
    """
    Attempts to register with Eureka in a loop until successful.
    This runs in the background so it doesn't crash the app on startup.
    """
    while True:
        try:
            logger.info("Attempting to register with Eureka...")
            await eureka_client.init_async(
                eureka_server=eureka_url,
                app_name="PREDICTION-SERVICE",
                instance_port=5002
            )
            logger.info("✅ Successfully registered with Eureka!")
            break  # Exit loop on success
        except Exception as e:
            logger.warning(f"❌ Eureka not ready yet ({e}). Retrying in 5 seconds...")
            await asyncio.sleep(5)


@app.on_event("shutdown")
async def shutdown():
    """Called when app shuts down - cleanup resources"""
    logger.info("Shutting down...")


# ============================================
# Health Check & Info Endpoints
# ============================================

@app.get("/health")
def health_check():
    """Health check endpoint - used by load balancers and monitoring"""
    return {
        "status": "UP",
        "service": "prediction-service",
        "version": "1.0.0"
    }


@app.get("/")
def root():
    """Root endpoint - provides service info and available endpoints"""
    return {
        "service": "Basketball Prediction Service",
        "version": "1.0.0",
        "status": "Placeholder - XGBoost coming soon",
        "endpoints": {
            "health": "/health",
            "predict": "/predict",
            "predict_batch": "/predict/batch",
            "get_prediction": "/predictions/{player_name}",
            "all_predictions": "/predictions",
            "clear_cache": "/cache/invalidate"
        }
    }


# ============================================
# Prediction Endpoints - Main API
# ============================================

@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    """Get a prediction for a single player"""
    logger.info(f"Prediction request for: {request.playerName}")
    
    try:
        # Call prediction function with player stats
        predicted_pts, predicted_ast, predicted_reb, confidence = predict_performance(
            request.currentStats.ppg,
            request.currentStats.apg,
            request.currentStats.rpg
        )
        
        # Return prediction response with validated data
        return PredictionResponse(
            playerName=request.playerName,
            predictedPoints=predicted_pts,
            predictedAssists=predicted_ast,
            predictedRebounds=predicted_reb,
            confidence=confidence
        )
    except Exception as e:
        logger.error(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/predict/batch")
def predict_batch(request: BatchPredictionRequest):
    """Get predictions for multiple players at once"""
    logger.info(f"Batch prediction for {len(request.predictions)} players")
    
    results = []
    # Process each player prediction request
    for pred_req in request.predictions:
        try:
            predicted_pts, predicted_ast, predicted_reb, confidence = predict_performance(
                pred_req.currentStats.ppg,
                pred_req.currentStats.apg,
                pred_req.currentStats.rpg
            )
            
            results.append(PredictionResponse(
                playerName=pred_req.playerName,
                predictedPoints=predicted_pts,
                predictedAssists=predicted_ast,
                predictedRebounds=predicted_reb,
                confidence=confidence
            ))
        except Exception as e:
            logger.error(f"Error predicting for {pred_req.playerName}: {e}")
            # Continue processing other predictions even if one fails
    
    return results


# ============================================
# Database Query Endpoints
# ============================================

@app.get("/predictions")
def get_all_predictions(limit: int = 50, db: Session = Depends(get_db)):
    """Get all stored predictions from database (most recent first)"""
    logger.info(f"Fetching predictions (limit: {limit})")
    
    try:
        # Query database with limit
        predictions = db.query(PlayerPrediction).order_by(
            PlayerPrediction.created_at.desc()  # Most recent first
        ).limit(limit).all()
        
        # Return count and list of predictions
        return {
            "count": len(predictions),
            "predictions": [p.to_dict() for p in predictions]
        }
    except Exception as e:
        logger.error(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/predictions/{player_name}")
def get_prediction(player_name: str, db: Session = Depends(get_db)):
    """Get stored prediction for a specific player - checks cache first, then database"""
    logger.info(f"Fetching prediction for: {player_name}")
    
    try:
        # Try Redis cache first (faster)
        cached = PredictionCache.get(player_name)
        if cached:
            logger.info(f"Cache hit for {player_name}")
            return cached
        
        # If not in cache, query database
        prediction = db.query(PlayerPrediction).filter(
            PlayerPrediction.player_name == player_name
        ).order_by(PlayerPrediction.created_at.desc()).first()
        
        if prediction:
            result = prediction.to_dict()
            # Cache the result for future requests
            PredictionCache.set(player_name, result)
            return result
        
        # Not found anywhere
        raise HTTPException(status_code=404, detail=f"No prediction found for {player_name}")
    
    except HTTPException:
        raise  # Re-raise HTTP exceptions
    except Exception as e:
        logger.error(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================
# Cache Management Endpoints
# ============================================

@app.delete("/cache/invalidate")
def invalidate_cache():
    """Clear all cached predictions - useful for testing or maintenance"""
    logger.info("Invalidating cache...")
    
    try:
        success = PredictionCache.invalidate_all()  # Clear entire cache
        if success:
            return {"message": "Cache cleared"}
        else:
            raise HTTPException(status_code=500, detail="Failed to clear cache")
    except Exception as e:
        logger.error(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # Run the app with uvicorn when file is executed directly
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5002)  # Listen on all interfaces, port 5002

