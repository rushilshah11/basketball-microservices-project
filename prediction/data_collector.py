"""
Data collection module - stores game logs for training
"""
import logging
from typing import List, Dict, Optional
from sqlalchemy.orm import Session
from datetime import datetime

from database import GameLog
from stats_client import stats_client

logger = logging.getLogger("prediction-service")


class DataCollector:
    """Collects and stores historical game data for model training"""
    
    @staticmethod
    async def collect_and_store_game_logs(
        player_name: str,
        db: Session,
        limit: int = 10
    ) -> int:
        """
        Fetch recent game logs from Stats Service and store in database
        Returns number of games stored
        """
        try:
            # Fetch game logs from Stats Service
            game_logs = await stats_client.get_player_game_log(player_name, limit)
            
            if not game_logs:
                logger.warning(f"No game logs available for {player_name}")
                return 0
            
            stored_count = 0
            
            for game in game_logs:
                # Check if game already exists
                existing = db.query(GameLog).filter(
                    GameLog.player_name == player_name,
                    GameLog.game_date == game.get("date")
                ).first()
                
                if existing:
                    continue  # Skip duplicates
                
                # Create new game log entry
                game_log = GameLog(
                    player_name=player_name,
                    game_date=game.get("date"),
                    opponent=game.get("opponent", "Unknown"),
                    points=game.get("pts", 0.0),
                    assists=game.get("ast", 0.0),
                    rebounds=game.get("reb", 0.0),
                    minutes=game.get("min", 0.0),
                    fgm=game.get("fgm", 0.0),
                    fga=game.get("fga", 0.0),
                    ftm=game.get("ftm", 0.0),
                    fta=game.get("fta", 0.0),
                    steals=game.get("stl", 0.0),
                    blocks=game.get("blk", 0.0),
                    turnovers=game.get("tov", 0.0),
                    is_home=1  # Default to home (would need Stats Service to provide this)
                )
                
                db.add(game_log)
                stored_count += 1
            
            db.commit()
            logger.info(f"✅ Stored {stored_count} game logs for {player_name}")
            return stored_count
            
        except Exception as e:
            logger.error(f"❌ Error collecting game logs for {player_name}: {e}")
            db.rollback()
            return 0
    
    @staticmethod
    def get_player_training_data(
        db: Session,
        player_name: Optional[str] = None,
        min_games: int = 5
    ) -> List[Dict]:
        """
        Get training data from database
        Returns list of game logs suitable for training
        """
        try:
            query = db.query(GameLog)
            
            if player_name:
                query = query.filter(GameLog.player_name == player_name)
            
            # Get all games
            games = query.order_by(GameLog.game_date.desc()).all()
            
            # Convert to training format
            training_data = []
            for game in games:
                # Skip incomplete data
                if game.points is None or game.assists is None or game.rebounds is None:
                    continue
                
                training_data.append({
                    "player_name": game.player_name,
                    "game_date": game.game_date,
                    "opponent": game.opponent,
                    "points": game.points,
                    "assists": game.assists,
                    "rebounds": game.rebounds,
                    "minutes": game.minutes or 30.0,
                    "fgm": game.fgm or 0.0,
                    "fga": game.fga or 1.0,
                    "ftm": game.ftm or 0.0,
                    "fta": game.fta or 1.0,
                    "steals": game.steals or 0.0,
                    "blocks": game.blocks or 0.0,
                    "turnovers": game.turnovers or 0.0,
                    "is_home": game.is_home or 1
                })
            
            logger.info(f"📊 Retrieved {len(training_data)} training samples")
            return training_data
            
        except Exception as e:
            logger.error(f"❌ Error retrieving training data: {e}")
            return []
    
    @staticmethod
    def get_training_stats(db: Session) -> Dict:
        """Get statistics about collected training data"""
        try:
            total_games = db.query(GameLog).count()
            unique_players = db.query(GameLog.player_name).distinct().count()
            
            # Get latest game date
            latest_game = db.query(GameLog).order_by(
                GameLog.game_date.desc()
            ).first()
            
            return {
                "total_games": total_games,
                "unique_players": unique_players,
                "latest_game_date": latest_game.game_date if latest_game else None,
                "database_status": "ready" if total_games >= 100 else "needs_more_data"
            }
        except Exception as e:
            logger.error(f"❌ Error getting training stats: {e}")
            return {"error": str(e)}
