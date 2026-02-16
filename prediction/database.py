"""
Database configuration and models
Manages PostgreSQL connection and defines all table schemas.
"""
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import logging

from config import DATABASE_URL

logger = logging.getLogger(__name__)

# Create database engine with connection pooling
engine = create_engine(DATABASE_URL, pool_pre_ping=True)  # pool_pre_ping tests connections before using them
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)  # Session factory for creating DB sessions

Base = declarative_base()


class PlayerPrediction(Base):
    """
    Table to store player predictions
    Main table - stores all prediction results with timestamps
    """
    __tablename__ = "player_predictions"

    id = Column(Integer, primary_key=True, index=True)  # Unique ID for each prediction
    player_name = Column(String(255), nullable=False, index=True)  # Player name (indexed for fast lookup)
    predicted_stats = Column(JSON, nullable=True)  # JSON object with predicted points, assists, rebounds
    confidence = Column(Float, nullable=True)  # Model's confidence score (0.0 - 1.0)
    created_at = Column(DateTime, default=datetime.utcnow)  # When this prediction was created

    def to_dict(self):
        """Convert database record to dictionary for JSON response"""
        return {
            "id": self.id,
            "player_name": self.player_name,
            "predicted_stats": self.predicted_stats,
            "confidence": self.confidence,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }


def init_db():
    """Initialize database tables - called once at app startup"""
    try:
        Base.metadata.create_all(bind=engine)  # Creates all tables defined in models
        logger.info("✅ Database tables created successfully")
    except Exception as e:
        logger.error(f"❌ Error creating database tables: {e}")
        raise


def get_db():
    """Get database session - used as dependency in FastAPI endpoints"""
    db = SessionLocal()  # Create new session
    try:
        yield db  # Provide session to the endpoint
    finally:
        db.close()  # Clean up - close connection after endpoint finishes
