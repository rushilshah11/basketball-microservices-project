"""
Client for calling Stats Service with fallback handling
"""
import httpx
import logging
from typing import Optional, Dict
import os

logger = logging.getLogger("prediction-service")

# Stats service configuration
STATS_SERVICE_URL = os.getenv("STATS_SERVICE_URL", "http://stats-service:8081")
REQUEST_TIMEOUT = 10.0  # 10 seconds timeout


class StatsServiceClient:
    """Client to fetch player stats from Stats Service"""

    def __init__(self):
        self.base_url = STATS_SERVICE_URL
        # We removed self.client here to prevent event loop conflicts

    async def get_player_stats(self, player_name: str, retries: int = 3) -> Optional[Dict]:
        """
        Fetch player stats from Stats Service with retry logic
        """
        for attempt in range(retries):
            try:
                url = f"{self.base_url}/api/players/stats"
                params = {"name": player_name}

                async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
                    response = await client.get(url, params=params)

                    if response.status_code == 200:
                        data = response.json()
                        if not data or data == {}:
                            logger.warning(f"⚠️ Stats Service returned empty data for {player_name}")
                            return None
                        return data
                    elif response.status_code == 429:  # Rate limited
                        wait_time = (2 ** attempt) * 0.5  # Exponential backoff
                        logger.warning(f"⏳ Rate limited. Retrying in {wait_time}s (attempt {attempt + 1}/{retries})")
                        await asyncio.sleep(wait_time)
                    else:
                        logger.error(f"❌ Stats Service error: {response.status_code}")
                        return None

            except httpx.TimeoutException:
                logger.error(f"⏱️ Timeout calling Stats Service for {player_name}")
                if attempt < retries - 1:
                    await asyncio.sleep(1)
                else:
                    return None
            except Exception as e:
                logger.error(f"❌ Error calling Stats Service: {e}")
                return None

        return None

    async def get_player_game_log(self, player_name: str, limit: int = 5) -> Optional[list]:
        """
        Fetch player recent game log from Stats Service
        Returns None if service is unavailable or returns empty data
        """
        try:
            url = f"{self.base_url}/api/players/games"
            params = {"name": player_name, "limit": limit}

            logger.info(f"📞 Calling Stats Service for game log: {player_name}")

            # Create a fresh client for this request
            async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
                response = await client.get(url, params=params)

                if response.status_code == 200:
                    data = response.json()

                    # Handle empty array (fallback activated)
                    if not data or data == []:
                        logger.warning(f"⚠️ Stats Service returned empty game log for {player_name}")
                        return None

                    logger.info(f"✅ Received {len(data)} games for {player_name}")
                    return data
                else:
                    logger.error(f"❌ Stats Service error: {response.status_code}")
                    return None

        except httpx.TimeoutException:
            logger.error(f"⏱️ Timeout calling Stats Service for {player_name}")
            return None
        except Exception as e:
            logger.error(f"❌ Error calling Stats Service: {e}")
            return None

    async def close(self):
        """Close HTTP client - No-op now as we use context managers"""
        pass


# Global client instance
stats_client = StatsServiceClient()