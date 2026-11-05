"use client";

import { useState } from "react";
import {
  Player,
  GameLog,
  SeasonAverages,
  addToTeam,
  removeFromTeam,
  getLastGames,
  getSeasonAverages,
} from "@/lib/api";

interface PlayerCardProps {
  player: Player;
  initialOnTeam?: boolean;
  onTeamChange?: (playerId: number, onTeam: boolean) => void;
}

export default function PlayerCard({ player, initialOnTeam = false, onTeamChange }: PlayerCardProps) {
  const [loading, setLoading] = useState(false);
  const [onTeam, setOnTeam] = useState(initialOnTeam);

  const [last5Games, setLast5Games] = useState<GameLog[] | null>(null);
  const [seasonStats, setSeasonStats] = useState<SeasonAverages | null>(null);
  const [showLast5Games, setShowLast5Games] = useState(false);
  const [showSeasonStats, setShowSeasonStats] = useState(false);
  const [loadingGames, setLoadingGames] = useState(false);
  const [loadingStats, setLoadingStats] = useState(false);

  const handleTeamToggle = async () => {
    setLoading(true);
    try {
      if (onTeam) await removeFromTeam(player.id);
      else await addToTeam(player.id);
      setOnTeam(!onTeam);
      onTeamChange?.(player.id, !onTeam);
    } catch (error) {
      console.error(error);
      alert("Failed to update your team.");
    } finally {
      setLoading(false);
    }
  };

  const handleViewLast5Games = async () => {
    if (!showLast5Games && last5Games === null) {
      setLoadingGames(true);
      try {
        setLast5Games(await getLastGames(player.id, 5));
      } catch (err) {
        console.error(err);
      } finally {
        setLoadingGames(false);
      }
    }
    setShowLast5Games(!showLast5Games);
  };

  const handleViewSeasonStats = async () => {
    if (!showSeasonStats && seasonStats === null) {
      setLoadingStats(true);
      try {
        setSeasonStats(await getSeasonAverages(player.id));
      } catch (err) {
        console.error(err);
      } finally {
        setLoadingStats(false);
      }
    }
    setShowSeasonStats(!showSeasonStats);
  };

  return (
    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border-2 border-white/20 hover:border-orange-500/50 transition-all duration-300 shadow-lg hover:shadow-xl">
      {/* Player Header */}
      <div className="mb-4">
        <h3 className="text-2xl font-bold text-white mb-1">{player.full_name}</h3>
        <div className="flex items-center space-x-4 text-gray-300">
          <span className="flex items-center">
            <span className="mr-2">🏀</span>
            {player.team || "Free Agent"}
          </span>
            <span className="flex items-center">
                <span className="mr-2">👤</span>
                {player.position || "N/A"}
          </span>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={handleTeamToggle}
          disabled={loading}
          className={`flex-1 px-4 py-2 rounded-lg font-semibold transition-all duration-200 ${
            onTeam
              ? "bg-red-600 hover:bg-red-700 text-white"
              : "bg-green-600 hover:bg-green-700 text-white"
          } disabled:opacity-50 shadow-md`}
        >
          {loading
            ? "..."
            : onTeam
            ? "Remove from Team"
            : "Add to Team"}
        </button>
      </div>

      <div className="flex flex-col gap-2">
        <button
          onClick={handleViewLast5Games}
          className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md"
        >
          {loadingGames
            ? "Loading..."
            : showLast5Games
            ? "Hide Last 5 Games"
            : "View Last 5 Games"}
        </button>

        <button
          onClick={handleViewSeasonStats}
          className="w-full px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-lg shadow-md"
        >
          {loadingStats
            ? "Loading..."
            : showSeasonStats
            ? "Hide Season Stats"
            : "View Season Stats"}
        </button>
      </div>

      {/* Last 5 Games Section - Real Data Render */}
      {showLast5Games && (
        <div className="mt-4 p-4 bg-black/20 rounded-lg border border-white/10">
          <h4 className="text-lg font-semibold text-white mb-3">
            Last 5 Games
          </h4>
          <div className="space-y-2">
            {last5Games && last5Games.length > 0 ? (
              last5Games.map((game) => (
                <div
                  key={game.game_id}
                  className="flex justify-between items-center p-2 bg-white/5 rounded text-sm"
                >
                  <div>
                    <div className="text-white font-medium">
                      {game.matchup}
                    </div>
                    <div className="text-gray-400 text-xs">{game.game_date}</div>
                  </div>
                  <div className="flex space-x-3 text-gray-300">
                    <span>
                      PTS:{" "}
                      <span className="text-orange-400 font-bold">
                        {game.pts}
                      </span>
                    </span>
                    <span>
                      AST:{" "}
                      <span className="text-blue-400 font-bold">
                        {game.ast}
                      </span>
                    </span>
                    <span>
                      REB:{" "}
                      <span className="text-green-400 font-bold">
                        {game.reb}
                      </span>
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-gray-400 text-sm">No game data available.</p>
            )}
          </div>
        </div>
      )}

      {/* Season Stats Section - Real Data Render */}
      {showSeasonStats && !loadingStats && !seasonStats && (
        <p className="mt-4 text-gray-400 text-sm">No games played this season.</p>
      )}

      {showSeasonStats && seasonStats && (
        <div className="mt-4 p-4 bg-black/20 rounded-lg border border-white/10">
          <h4 className="text-lg font-semibold text-white mb-3">
            Season Statistics
          </h4>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">PPG</div>
              <div className="text-orange-400 font-bold text-lg">
                {seasonStats.ppg}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">APG</div>
              <div className="text-blue-400 font-bold text-lg">
                {seasonStats.apg}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">RPG</div>
              <div className="text-green-400 font-bold text-lg">
                {seasonStats.rpg}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">FG%</div>
              <div className="text-yellow-400 font-bold text-lg">
                {seasonStats.fg_pct ?? "-"}%
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">3P%</div>
              <div className="text-purple-400 font-bold text-lg">
                {seasonStats.fg3_pct ?? "-"}%
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">FT%</div>
              <div className="text-pink-400 font-bold text-lg">
                {seasonStats.ft_pct ?? "-"}%
              </div>
            </div>
          </div>
          <div className="mt-3 text-center text-gray-400 text-xs">
            Games Played: {seasonStats.games_played}
          </div>
        </div>
      )}
    </div>
  );
}
