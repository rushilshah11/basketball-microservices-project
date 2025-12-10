"""
Redis caching layer for predictions
"""
import redis
import json
import logging
from typing import Optional, Dict
import os

logger = logging.getLogger("prediction-service")

# Redis configuration
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
CACHE_TTL = 3600  # 1 hour cache expiration

# Redis client
redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    db=1,  # Use db=1 for predictions cache (db=0 for events)
    decode_responses=True
)


class PredictionCache:
    """Redis cache manager for predictions"""
    
    CACHE_PREFIX = "prediction:"
    
    @staticmethod
    def _get_cache_key(player_name: str) -> str:
        """Generate cache key for player"""
        return f"{PredictionCache.CACHE_PREFIX}{player_name.lower()}"
    
    @staticmethod
    def get(player_name: str) -> Optional[Dict]:
        """Get prediction from cache"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            cached = redis_client.get(cache_key)
            
            if cached:
                logger.info(f"✅ Cache HIT for {player_name}")
                return json.loads(cached)
            else:
                logger.info(f"❌ Cache MISS for {player_name}")
                return None
        except Exception as e:
            logger.error(f"Error reading from cache: {e}")
            return None
    
    @staticmethod
    def set(player_name: str, prediction_data: Dict, ttl: int = CACHE_TTL) -> bool:
        """Set prediction in cache with TTL"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            redis_client.setex(
                cache_key,
                ttl,
                json.dumps(prediction_data)
            )
            logger.info(f"✅ Cached prediction for {player_name} (TTL: {ttl}s)")
            return True
        except Exception as e:
            logger.error(f"Error writing to cache: {e}")
            return False
    
    @staticmethod
    def delete(player_name: str) -> bool:
        """Delete prediction from cache"""
        try:
            cache_key = PredictionCache._get_cache_key(player_name)
            redis_client.delete(cache_key)
            logger.info(f"🗑️ Deleted cache for {player_name}")
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
        logger.info("✅ Redis connection successful")
        return True
    except Exception as e:
        logger.error(f"❌ Redis connection failed: {e}")
        return False
