// All data access goes straight to Supabase. No backend in between.
import { supabase } from "./supabase";

export interface Player {
  id: number;
  full_name: string;
  team: string | null;
  position: string | null;
}

export interface GameLog {
  game_id: string;
  game_date: string;
  matchup: string;
  pts: number;
  reb: number;
  ast: number;
  minutes: number;
}

export interface SeasonAverages {
  season: string;
  games_played: number;
  ppg: number;
  rpg: number;
  apg: number;
  mpg: number;
  fg_pct: number | null;
  fg3_pct: number | null;
  ft_pct: number | null;
}

/** "2026-27" for any date from Oct 2026 through Sep 2027. */
export function currentSeason(today = new Date()): string {
  const start = today.getMonth() >= 9 ? today.getFullYear() : today.getFullYear() - 1;
  return `${start}-${String((start + 1) % 100).padStart(2, "0")}`;
}

function check<T>({ data, error }: { data: T | null; error: unknown }): T {
  if (error) throw error;
  return data as T;
}

export async function searchPlayers(query: string): Promise<Player[]> {
  return check(
    await supabase
      .from("players")
      .select("id, full_name, team, position")
      .ilike("full_name", `%${query}%`)
      .not("team", "is", null) // only players currently on a roster
      .order("full_name")
      .limit(24),
  );
}

/** Highest scorers this season, shown before the user searches. */
export async function getTopPlayers(): Promise<Player[]> {
  const rows = check(
    await supabase
      .from("player_season_averages")
      .select("player_id, full_name, team, position")
      .eq("season", currentSeason())
      .gte("games_played", 3)
      .order("ppg", { ascending: false })
      .limit(12),
  );
  return rows.map((r) => ({ id: r.player_id, full_name: r.full_name, team: r.team, position: r.position }));
}

export async function getMyTeam(): Promise<Player[]> {
  const rows = check(
    await supabase
      .from("team_players")
      .select("players(id, full_name, team, position)")
      .order("added_at"),
  );
  return rows.map((r) => r.players as unknown as Player);
}

export async function addToTeam(playerId: number) {
  check(await supabase.from("team_players").insert({ player_id: playerId }));
}

export async function removeFromTeam(playerId: number) {
  check(await supabase.from("team_players").delete().eq("player_id", playerId));
}

export async function getLastGames(playerId: number, limit = 5): Promise<GameLog[]> {
  return check(
    await supabase
      .from("game_logs")
      .select("game_id, game_date, matchup, pts, reb, ast, minutes")
      .eq("player_id", playerId)
      .order("game_date", { ascending: false })
      .limit(limit),
  );
}

export async function getSeasonAverages(playerId: number): Promise<SeasonAverages | null> {
  return check(
    await supabase
      .from("player_season_averages")
      .select("season, games_played, ppg, rpg, apg, mpg, fg_pct, fg3_pct, ft_pct")
      .eq("player_id", playerId)
      .eq("season", currentSeason())
      .maybeSingle(),
  );
}
