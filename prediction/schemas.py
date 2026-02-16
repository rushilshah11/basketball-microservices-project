"""
Pydantic request/response models
Defines all data structures for API requests and responses.
These models ensure data validation and provide API documentation.
"""
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class PlayerStats(BaseModel):
    """Input player stats - the features we use for predictions"""
    ppg: float  # Points per game
    apg: float  # Assists per game
    rpg: float  # Rebounds per game
    fgPct: Optional[float] = 0.45  # Field goal percentage (optional)
    ftPct: Optional[float] = 0.75  # Free throw percentage (optional)
    gamesPlayed: int  # Number of games player has played
    minutesPerGame: Optional[float] = 30.0  # Average minutes per game (optional)
    stealsPerGame: Optional[float] = 1.0  # Steals per game (optional)
    blocksPerGame: Optional[float] = 0.5  # Blocks per game (optional)
    turnoversPerGame: Optional[float] = 2.0  # Turnovers per game (optional)


class PredictionRequest(BaseModel):
    """Request for prediction - what client sends to /predict endpoint"""
    playerName: str  # Name of player to predict
    currentStats: PlayerStats  # Player's current season stats
    homeGame: bool = True  # Whether the game is at home (defaults to true)


class PredictionResponse(BaseModel):
    """Response with prediction - standardized format for both POST and GET endpoints"""
    id: Optional[int] = None  # Database record ID (only for stored predictions)
    player_name: str  # Player name
    predicted_stats: dict  # JSON object with predicted values (pts, ast, reb)
    confidence: float  # Model confidence score
    created_at: Optional[datetime] = None  # When prediction was created


class BatchPredictionRequest(BaseModel):
    """Batch prediction request - predict multiple players at once"""
    predictions: List[PredictionRequest]  # List of prediction requests


class StoredPredictionResponse(BaseModel):
    """Stored prediction from database - what's returned from /predictions endpoints"""
    id: int  # Database record ID
    player_name: str  # Player name
    predicted_stats: dict  # JSON object with predicted values
    confidence: float  # Model confidence score
    created_at: datetime  # When prediction was created
