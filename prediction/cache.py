"""
Redis caching layer for predictions
Provides fast in-memory caching to avoid repeated predictions.
"""
import redis
import json
import logging
from typing import Optional, Dict
import os

logger = logging.getLogger("prediction-service")

# Redis connection settings
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")  # Redis server hostname
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))  # Redis port number
REDIS_SSL = os.getenv("REDIS_SSL", "false").lower() == "true"  # Whether to use SSL
CACHE_TTL = 3600  # Cache expiration time in seconds (1 hour)

# Initialize Redis client connection
redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    db=1,  # Using database 1 for predictions (database 0 reserved for other services)
    ssl=REDIS_SSL,  # SSL support for cloud-hosted Redis
    decode_responses=True  # Automatically decode responses to strings
)

class PredictionCache:
    """Redis cache manager - handles storing and retrieving cached predictions"""

    CACHE_PREFIX = "prediction:"  # Prefix for all cache keys to avoid conflicts

    @staticmethod
    def _get_cache_key(player_name: str) -> str:
        """Generate cache key for player - ensures consistent key format"""
        return f"{PredictionCache.CACHE_PREFIX}{player_name.lower()}"  # Lowercase for consistency

    @staticmethod
    def get(player_name: str) -> Optional[Dict]:
        """Get prediction from cache - returns None if not found or expired"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            cached = redis_client.get(cache_key)  # Redis returns None if key doesn't exist or expired

            if cached:
                logger.info(f"Cache HIT for {player_name}")  # Found cached prediction
                return json.loads(cached)  # Convert JSON string back to dict
            else:
                logger.info(f"Cache MISS for {player_name}")  # Not found in cache
                return None
        except Exception as e:
            logger.error(f"Error reading from cache: {e}")
            return None  # Return None on error so app continues

    @staticmethod
    def set(player_name: str, prediction_data: Dict, ttl: int = CACHE_TTL) -> bool:
        """Set prediction in cache with time-to-live (TTL)"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            redis_client.setex(
                cache_key,
                ttl,  # Expiration time in seconds
                json.dumps(prediction_data)  # Convert dict to JSON string for storage
            )
            logger.info(f"Cached prediction for {player_name} (TTL: {ttl}s)")
            return True
        except Exception as e:
            logger.error(f"Error writing to cache: {e}")
            return False  # Return False on error so app continues

    @staticmethod
    def delete(player_name: str) -> bool:
        """Delete specific prediction from cache"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            redis_client.delete(cache_key)  # Redis delete silently succeeds if key doesn't exist
            logger.info(f"Deleted cache for {player_name}")
            return True
        except Exception as e:
            logger.error(f"Error deleting from cache: {e}")
            return False

    @staticmethod
    def invalidate_all() -> bool:
        """Clear all prediction caches"""
        try:
            pattern = f"{PredictionCache.CACHE_PREFIX}*"
            keys = redis_client.keys(pattern)
            if keys:
                redis_client.delete(*keys)
                logger.info(f"🗑️ Invalidated {len(keys)} cached predictions")
            return True
        except Exception as e:
            logger.error(f"Error invalidating cache: {e}")
            return False


def test_redis_connection() -> bool:
    """Test Redis connection"""
    try:
        redis_client.ping()
        logger.info(f"✅ Redis connection successful (SSL: {REDIS_SSL})")
        return True
    except Exception as e:
        logger.error(f"❌ Redis connection failed: {e}")
        return False