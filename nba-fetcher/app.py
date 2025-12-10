import asyncio
from fastapi import FastAPI, HTTPException, Query
from nba_api.stats.static import players
from nba_api.stats.endpoints import playercareerstats, playergamelog, commonplayerinfo
from pydantic import BaseModel
from typing import List, Optional
import logging
import concurrent.futures
import py_eureka_client.eureka_client as eureka_client

from opentelemetry import trace
from opentelemetry.exporter.zipkin.json import ZipkinExporter
from opentelemetry.sdk.resources import SERVICE_NAME, Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor

# --- NEW: Configure Tracer ---
resource = Resource(attributes={
    SERVICE_NAME: "nba-fetcher"  # This name will show up in Zipkin
})

zipkin_exporter = ZipkinExporter(
    endpoint="http://zipkin:9411/api/v2/spans"
)

provider = TracerProvider(resource=resource)
processor = BatchSpanProcessor(zipkin_exporter)
provider.add_span_processor(processor)
trace.set_tracer_provider(provider)

# Instrument outgoing HTTP calls (nba_api uses 'requests' under the hood)
RequestsInstrumentor().instrument()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("nba-fetcher")

app = FastAPI(title="NBA Fetcher Microservice")
FastAPIInstrumentor.instrument_app(app)

# --- Helper Functions ---
def get_player_id_by_name(full_name: str):
    matches = players.find_players_by_full_name(full_name)
    if not matches:
        return None
    return matches[0]['id']

def fetch_player_data_with_team(name: str):
    """
    Helper to fetch both ID and Team Name for a single player.
    Used by the batch endpoint in parallel.
    """
    try:
        matches = players.find_players_by_full_name(name)
        if not matches:
            return None

        data = matches[0]
        player_id = data['id']

        # Fetch Team Info
        # This is the slow part, so we run it in parallel threads
        team_name = "Free Agent"
        try:
            info = commonplayerinfo.CommonPlayerInfo(player_id=player_id)
            df = info.get_data_frames()[0]
            if not df.empty:
                city = str(df.iloc[0]['TEAM_CITY'])
                nickname = str(df.iloc[0]['TEAM_NAME'])
                # Combine city + name (e.g., "Los Angeles Lakers")
                # Check if 'city' is empty or null to avoid "nan Lakers"
                if city and city.lower() != 'nan':
                    team_name = f"{city} {nickname}"
                else:
                    team_name = nickname
        except Exception:
            logger.warning(f"Could not fetch team for {name}")

        return {
            "id": player_id,
            "fullName": data['full_name'],
            "teamName": team_name
        }
    except Exception as e:
        logger.error(f"Error processing {name}: {e}")
        return None

async def register_with_eureka():
    """
    Attempts to register with Eureka in a loop until successful.
    This runs in the background so it doesn't crash the app on startup.
    """
    while True:
        try:
            logger.info("Attempting to register with Eureka...")
            await eureka_client.init_async(
                eureka_server="http://eureka-server:8761/eureka",
                app_name="NBA-FETCHER",
                instance_port=5000
            )
            logger.info("✅ Successfully registered with Eureka!")
            break  # Exit loop on success
        except Exception as e:
            logger.warning(f"❌ Eureka not ready yet ({e}). Retrying in 5 seconds...")
            await asyncio.sleep(5)

@app.on_event("startup")
async def startup_event():
    # Schedule the registration to run in the background
    asyncio.create_task(register_with_eureka())

@app.get("/health")
def health_check():
    return {"status": "UP", "service": "nba-fetcher"}

# 1. Get Player Basic Info
@app.get("/player/{full_name}")
def get_player(full_name: str):
    logger.info(f"Searching for player: {full_name}")
    matches = players.find_players_by_full_name(full_name)
    if not matches:
        raise HTTPException(status_code=404, detail="Player not found")

    data = matches[0]
    return {
        "id": data['id'],
        "fullName": data['full_name'],
        "firstName": data['first_name'],
        "lastName": data['last_name'],
        "isActive": data['is_active']
    }

# 2. Batch Endpoint (UPDATED with Parallel Execution)
class BatchRequest(BaseModel):
    names: List[str]

@app.post("/players/batch")
def get_players_batch(request: BatchRequest):
    results = []
    # Use ThreadPoolExecutor to run API calls in parallel
    # This makes fetching 10 players take ~1 second instead of ~10 seconds
    with concurrent.futures.ThreadPoolExecutor() as executor:
        # Submit all tasks
        future_to_name = {
            executor.submit(fetch_player_data_with_team, name): name
            for name in request.names
        }

        # Collect results as they finish
        for future in concurrent.futures.as_completed(future_to_name):
            data = future.result()
            if data:
                results.append(data)

    return results

# 3. Get Player Team
@app.get("/player/{full_name}/team")
def get_player_team(full_name: str):
    player_id = get_player_id_by_name(full_name)
    if not player_id:
        raise HTTPException(status_code=404, detail="Player not found")

    try:
        info = commonplayerinfo.CommonPlayerInfo(player_id=player_id)
        df = info.get_data_frames()[0]
        if df.empty:
            raise HTTPException(status_code=404, detail="No team info found")

        return {
            "teamId": int(df.iloc[0]['TEAM_ID']),
            "teamName": str(df.iloc[0]['TEAM_NAME']),
            "teamCity": str(df.iloc[0]['TEAM_CITY']),
            "teamAbbreviation": str(df.iloc[0]['TEAM_ABBREVIATION'])
        }
    except Exception as e:
        logger.error(f"Error fetching team: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# 4. Get Season Averages
@app.get("/player/{full_name}/stats")
def get_player_season_averages(full_name: str):
    player_id = get_player_id_by_name(full_name)
    if not player_id:
        raise HTTPException(status_code=404, detail="Player not found")

    try:
        career = playercareerstats.PlayerCareerStats(player_id=player_id)
        df = career.get_data_frames()[0]

        if df.empty:
            raise HTTPException(status_code=404, detail="No stats found")

        latest = df.iloc[-1]
        games = int(latest['GP'])

        if games == 0:
            return {"season": str(latest['SEASON_ID']), "gamesPlayed": 0, "ppg": 0.0, "apg": 0.0, "rpg": 0.0}

        return {
            "season": str(latest['SEASON_ID']),
            "gamesPlayed": games,
            "ppg": round(float(latest['PTS']) / games, 1),
            "apg": round(float(latest['AST']) / games, 1),
            "rpg": round(float(latest['REB']) / games, 1)
        }
    except Exception as e:
        logger.error(f"Error fetching stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# 5. Get Last N Games
@app.get("/player/{full_name}/games")
def get_player_game_log(full_name: str, limit: int = Query(5, ge=1, le=82)):
    player_id = get_player_id_by_name(full_name)
    if not player_id:
        raise HTTPException(status_code=404, detail="Player not found")

    try:
        gamelog = playergamelog.PlayerGameLog(player_id=player_id)
        df = gamelog.get_data_frames()[0]

        if df.empty:
            return []

        last_n = df.head(limit)
        games = []
        for index, row in last_n.iterrows():
            games.append({
                "gameId": str(row['Game_ID']),
                "gameDate": str(row['GAME_DATE']),
                "matchup": str(row['MATCHUP']),
                "wl": str(row['WL']),
                "points": int(row['PTS']),
                "assists": int(row['AST']),
                "rebounds": int(row['REB']),
                "steals": int(row['STL']),
                "blocks": int(row['BLK']),
                "turnovers": int(row['TOV']),
                "fgPct": float(row['FG_PCT'])
            })

        return games
    except Exception as e:
        logger.error(f"Error fetching game log: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=5000)