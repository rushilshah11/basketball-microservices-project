"""
Configuration and environment variables
Centralized place for all settings - database, cache, logging, etc.
"""
import os
from typing import Optional

# Database configuration - required to be set in environment
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise ValueError(
        "DATABASE_URL environment variable is required. "
        "Example: postgresql://user:password@localhost/predictions_db"
    )

# Redis configuration - for caching predictions
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
REDIS_SSL = os.getenv("REDIS_SSL", "false").lower() == "true"
CACHE_TTL = 3600  # Cache expiration time in seconds (1 hour)

# External service URLs - for fetching stats from other microservices
STATS_SERVICE_URL = os.getenv("STATS_SERVICE_URL", "http://stats-service:8081")
REQUEST_TIMEOUT = 10.0  # Timeout for HTTP requests in seconds

# Logging level - controls verbosity of log output
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
