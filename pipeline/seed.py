"""
Pull NBA data from stats.nba.com (via nba_api) and load it into Supabase.

    python pipeline/seed.py                 # current season + previous 2
    python pipeline/seed.py --seasons 1     # current season only (faster)

Only two NBA API calls per season, so this takes about a minute:
  - PlayerIndex   -> every rostered player with team + position
  - LeagueGameLog -> every player's box score for every game that season

Safe to re-run: rows are upserted, never duplicated.
"""
import argparse
import os
from datetime import date

import pandas as pd
import psycopg
from dotenv import load_dotenv
from nba_api.stats.endpoints import leaguegamelog, playerindex

load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))
DATABASE_URL = os.environ["DATABASE_URL"]
TIMEOUT = 60  # stats.nba.com can be slow


def records(df: pd.DataFrame) -> list[dict]:
    """DataFrame -> plain Python dicts (psycopg can't store numpy types or NaN)."""
    return df.astype(object).where(df.notna(), None).to_dict("records")


def season_string(start_year: int) -> str:
    """2026 -> '2026-27'"""
    return f"{start_year}-{(start_year + 1) % 100:02d}"


def current_season_start(today: date) -> int:
    # The NBA season starts in October, so Jan-Sep belongs to last year's season.
    return today.year if today.month >= 10 else today.year - 1


def fetch_players(season: str) -> list[tuple]:
    df = playerindex.PlayerIndex(season=season, timeout=TIMEOUT).get_data_frames()[0]
    rows = []
    for r in records(df):
        team = f"{r['TEAM_CITY']} {r['TEAM_NAME']}".strip() if r["TEAM_NAME"] else None
        rows.append((r["PERSON_ID"], f"{r['PLAYER_FIRST_NAME']} {r['PLAYER_LAST_NAME']}".strip(),
                     team, r["POSITION"] or None))
    return rows


def fetch_game_logs(season: str) -> list[tuple]:
    df = leaguegamelog.LeagueGameLog(
        season=season, player_or_team_abbreviation="P", timeout=TIMEOUT
    ).get_data_frames()[0]
    rows = []
    for r in records(df):
        # MATCHUP looks like "NYK vs. BOS" (home) or "NYK @ BOS" (away)
        matchup = r["MATCHUP"]
        rows.append((
            r["PLAYER_ID"], r["PLAYER_NAME"], r["GAME_ID"], r["GAME_DATE"], season, matchup,
            matchup.split()[-1], "vs." in matchup, r["MIN"],
            *(r[c] for c in ("PTS", "REB", "AST", "STL", "BLK", "TOV",
                             "FGM", "FGA", "FG3M", "FG3A", "FTM", "FTA")),
        ))
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seasons", type=int, default=3, help="how many seasons back to load")
    args = parser.parse_args()

    start = current_season_start(date.today())
    seasons = [season_string(start - i) for i in range(args.seasons)]
    current = seasons[0]

    with psycopg.connect(DATABASE_URL) as conn, conn.cursor() as cur:
        for season in reversed(seasons):  # oldest first
            print(f"[{season}] fetching game logs...")
            logs = fetch_game_logs(season)
            # Make sure every player in the logs exists (older seasons include retired players)
            cur.executemany(
                "insert into players (id, full_name) values (%s, %s) on conflict (id) do nothing",
                list({(row[0], row[1]) for row in logs}),
            )
            cur.executemany(
                """insert into game_logs (player_id, game_id, game_date, season, matchup, opponent,
                       is_home, minutes, pts, reb, ast, stl, blk, tov, fgm, fga, fg3m, fg3a, ftm, fta)
                   values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                   on conflict (player_id, game_id) do update set
                       minutes = excluded.minutes, pts = excluded.pts, reb = excluded.reb,
                       ast = excluded.ast, stl = excluded.stl, blk = excluded.blk, tov = excluded.tov""",
                [(row[0],) + row[2:] for row in logs],
            )
            conn.commit()
            print(f"[{season}] {len(logs)} game rows loaded")

        print(f"[{current}] fetching current rosters...")
        players = fetch_players(current)
        cur.executemany(
            """insert into players (id, full_name, team, position) values (%s, %s, %s, %s)
               on conflict (id) do update set full_name = excluded.full_name, team = excluded.team,
                   position = excluded.position, updated_at = now()""",
            players,
        )
        conn.commit()
        print(f"[{current}] {len(players)} players loaded. Done.")


if __name__ == "__main__":
    main()
