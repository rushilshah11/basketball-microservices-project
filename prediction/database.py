"""
Database configuration and models for prediction service
"""
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import os
import logging

logger = logging.getLogger("prediction-service")

# Database configuration
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://user:password@localhost:5433/bball-prediction"
)

# Create engine
engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


class PlayerPrediction(Base):
    """
    Table to store player predictions
    """
    __tablename__ = "player_predictions"

    id = Column(Integer, primary_key=True, index=True)
    player_name = Column(String(255), nullable=False, index=True)
    predicted_stats = Column(JSON, nullable=True)  # {"pts": 25.5, "ast": 6.2, "reb": 4.1}
    confidence = Column(Float, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    def to_dict(self):
        """Convert model to dictionary"""
        return {
            "id": self.id,
            "player_name": self.player_name,
            "predicted_stats": self.predicted_stats,
            "confidence": self.confidence,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }


class GameLog(Base):
    """
    Table to store historical game data for training
    """
    __tablename__ = "game_logs"

    id = Column(Integer, primary_key=True, index=True)
    player_name = Column(String(255), nullable=False, index=True)
    game_date = Column(String(50), nullable=True)  # e.g., "2024-01-15"
    opponent = Column(String(100), nullable=True)
    
    # Game stats
    points = Column(Float, nullable=True)
    assists = Column(Float, nullable=True)
    rebounds = Column(Float, nullable=True)
    minutes = Column(Float, nullable=True)
    fgm = Column(Float, nullable=True)  # Field goals made
    fga = Column(Float, nullable=True)  # Field goals attempted
    ftm = Column(Float, nullable=True)  # Free throws made
    fta = Column(Float, nullable=True)  # Free throws attempted
    steals = Column(Float, nullable=True)
    blocks = Column(Float, nullable=True)
    turnovers = Column(Float, nullable=True)
    
    # Context
    is_home = Column(Integer, default=1)  # 1 for home, 0 for away
    
    created_at = Column(DateTime, default=datetime.utcnow)

    def to_dict(self):
        """Convert model to dictionary"""
        return {
            "id": self.id,
            "player_name": self.player_name,
            "game_date": self.game_date,
            "opponent": self.opponent,
            "points": self.points,
            "assists": self.assists,
            "rebounds": self.rebounds,
            "minutes": self.minutes,
            "fgm": self.fgm,
            "fga": self.fga,
            "ftm": self.ftm,
            "fta": self.fta,
            "steals": self.steals,
            "blocks": self.blocks,
            "turnovers": self.turnovers,
            "is_home": self.is_home,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }


class TrainingMetadata(Base):
    """
    Table to track model training history
    """
    __tablename__ = "training_metadata"

    id = Column(Integer, primary_key=True, index=True)
    model_version = Column(String(50), nullable=False)
    training_samples = Column(Integer, nullable=True)
    training_loss = Column(Float, nullable=True)
    validation_loss = Column(Float, nullable=True)
    epochs = Column(Integer, nullable=True)
    status = Column(String(50), nullable=True)  # 'training', 'completed', 'failed'
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime, nullable=True)
    notes = Column(String(500), nullable=True)

    def to_dict(self):
        """Convert model to dictionary"""
        return {
            "id": self.id,
            "model_version": self.model_version,
            "training_samples": self.training_samples,
            "training_loss": self.training_loss,
            "validation_loss": self.validation_loss,
            "epochs": self.epochs,
            "status": self.status,
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "notes": self.notes
        }


def init_db():
    """Initialize database tables"""
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("✅ Database tables created successfully")
    except Exception as e:
        logger.error(f"❌ Error creating database tables: {e}")
        raise


def get_db():
    """Get database session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
