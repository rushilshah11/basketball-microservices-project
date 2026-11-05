-- Run this once in the Supabase dashboard: SQL Editor -> New query -> paste -> Run.
-- Safe to re-run.

-- NBA players (filled by pipeline/seed.py)
create table if not exists players (
  id         bigint primary key,          -- NBA's own player id
  full_name  text not null,
  team       text,                        -- e.g. "New York Knicks", null = free agent
  position   text,                        -- e.g. "G", "F-C"
  updated_at timestamptz default now()
);

-- One row per player per game (filled by pipeline/seed.py)
create table if not exists game_logs (
  player_id  bigint references players(id) on delete cascade,
  game_id    text,
  game_date  date not null,
  season     text not null,               -- e.g. "2026-27"
  matchup    text,                        -- e.g. "NYK vs. BOS" or "NYK @ BOS"
  opponent   text,                        -- e.g. "BOS"
  is_home    boolean,
  minutes    numeric,
  pts int, reb int, ast int, stl int, blk int, tov int,
  fgm int, fga int, fg3m int, fg3a int, ftm int, fta int,
  primary key (player_id, game_id)
);

-- The players each user has added to their team
create table if not exists team_players (
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  player_id  bigint not null references players(id) on delete cascade,
  added_at   timestamptz default now(),
  primary key (user_id, player_id)
);

-- Season averages, computed from game_logs (no extra table to keep in sync)
create or replace view player_season_averages with (security_invoker = true) as
select
  p.id as player_id, p.full_name, p.team, p.position, g.season,
  count(*)                                                  as games_played,
  round(avg(g.pts), 1)                                      as ppg,
  round(avg(g.reb), 1)                                      as rpg,
  round(avg(g.ast), 1)                                      as apg,
  round(avg(g.minutes), 1)                                  as mpg,
  round(100.0 * sum(g.fgm)  / nullif(sum(g.fga), 0), 1)     as fg_pct,
  round(100.0 * sum(g.fg3m) / nullif(sum(g.fg3a), 0), 1)    as fg3_pct,
  round(100.0 * sum(g.ftm)  / nullif(sum(g.fta), 0), 1)     as ft_pct
from game_logs g
join players p on p.id = g.player_id
group by p.id, p.full_name, p.team, p.position, g.season;

-- Row Level Security: who can read/write what
alter table players      enable row level security;
alter table game_logs    enable row level security;
alter table team_players enable row level security;

drop policy if exists "anyone can read players" on players;
create policy "anyone can read players" on players for select using (true);

drop policy if exists "anyone can read game logs" on game_logs;
create policy "anyone can read game logs" on game_logs for select using (true);

drop policy if exists "users manage their own team" on team_players;
create policy "users manage their own team" on team_players
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- (The pipeline writes players/game_logs with a direct DB connection, which bypasses RLS.)
