"use client";

import { useState, useEffect } from "react";
import {
  Player,
  addToWatchlist,
  removeFromWatchlist,
  checkWatchlist,
  getPlayerGames, // Import these
  getPlayerStats, // Import these
  GameLog,
  PlayerStats,
} from "@/lib/api";

interface PlayerCardProps {
  player: Player;
}

export default function PlayerCard({ player }: PlayerCardProps) {
  const [isInWatchlist, setIsInWatchlist] = useState(false);
  const [loading, setLoading] = useState(false);

  // Data States
  const [last5Games, setLast5Games] = useState<GameLog[]>([]);
  const [seasonStats, setSeasonStats] = useState<PlayerStats | null>(null);

  // Toggle States
  const [showLast5Games, setShowLast5Games] = useState(false);
  const [showSeasonStats, setShowSeasonStats] = useState(false);
  const [loadingGames, setLoadingGames] = useState(false);
  const [loadingStats, setLoadingStats] = useState(false);

  // ... (Keep your existing useEffect for checkWatchlist) ...
  useEffect(() => {
    const checkStatus = async () => {
      try {
        const status = await checkWatchlist(player.fullName);
        setIsInWatchlist(status);
      } catch (error) {
        console.error("Failed to check watchlist status:", error);
      }
    };
    checkStatus();
  }, [player.fullName]);

  // ... (Keep your existing handleWatchlistToggle) ...
  const handleWatchlistToggle = async () => {
    setLoading(true);
    try {
      if (isInWatchlist) {
        await removeFromWatchlist(player.fullName);
        setIsInWatchlist(false);
      } else {
        await addToWatchlist(player.fullName);
        setIsInWatchlist(true);
      }
    } catch (error) {
      alert("Failed to update watchlist.");
    } finally {
      setLoading(false);
    }
  };

  const handleViewLast5Games = async () => {
    if (showLast5Games) {
      setShowLast5Games(false);
      return;
    }

    // Only fetch if we haven't already
    if (last5Games.length === 0) {
      setLoadingGames(true);
      try {
        const games = await getPlayerGames(player.fullName);
        setLast5Games(games);
      } catch (err) {
        console.error(err);
      } finally {
        setLoadingGames(false);
      }
    }
    setShowLast5Games(true);
  };

  const handleViewSeasonStats = async () => {
    if (showSeasonStats) {
      setShowSeasonStats(false);
      return;
    }

    if (!seasonStats) {
      setLoadingStats(true);
      try {
        const stats = await getPlayerStats(player.fullName);
        setSeasonStats(stats);
      } catch (err) {
        console.error(err);
      } finally {
        setLoadingStats(false);
      }
    }
    setShowSeasonStats(true);
  };

  return (
    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border-2 border-white/20 hover:border-orange-500/50 transition-all duration-300 shadow-lg hover:shadow-xl">
      {/* Player Header */}
      <div className="mb-4">
        <h3 className="text-2xl font-bold text-white mb-1">{player.fullName}</h3>
        <div className="flex items-center space-x-4 text-gray-300">
          <span className="flex items-center">
            <span className="mr-2">🏀</span>
            {player.teamName || "N/A"}
          </span>
          {/* Add Position if available in your API response, otherwise optional */}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={handleWatchlistToggle}
          disabled={loading}
          className={`flex-1 px-4 py-2 rounded-lg font-semibold transition-all duration-200 ${
            isInWatchlist
              ? "bg-red-600 hover:bg-red-700 text-white"
              : "bg-green-600 hover:bg-green-700 text-white"
          } disabled:opacity-50 shadow-md`}
        >
          {loading
            ? "..."
            : isInWatchlist
            ? "Remove Watchlist"
            : "Add Watchlist"}
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
            {last5Games.length > 0 ? (
              last5Games.map((game, idx) => (
                <div
                  key={idx}
                  className="flex justify-between items-center p-2 bg-white/5 rounded text-sm"
                >
                  <div>
                    <div className="text-white font-medium">
                      {game.opponent}
                    </div>
                    <div className="text-gray-400 text-xs">{game.date}</div>
                  </div>
                  <div className="flex space-x-3 text-gray-300">
                    <span>
                      PTS:{" "}
                      <span className="text-orange-400 font-bold">
                        {game.points}
                      </span>
                    </span>
                    <span>
                      AST:{" "}
                      <span className="text-blue-400 font-bold">
                        {game.assists}
                      </span>
                    </span>
                    <span>
                      REB:{" "}
                      <span className="text-green-400 font-bold">
                        {game.rebounds}
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
      {showSeasonStats && seasonStats && (
        <div className="mt-4 p-4 bg-black/20 rounded-lg border border-white/10">
          <h4 className="text-lg font-semibold text-white mb-3">
            Season Statistics
          </h4>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">PPG</div>
              <div className="text-orange-400 font-bold text-lg">
                {seasonStats.pointsPerGame}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">APG</div>
              <div className="text-blue-400 font-bold text-lg">
                {seasonStats.assistsPerGame}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">RPG</div>
              <div className="text-green-400 font-bold text-lg">
                {seasonStats.reboundsPerGame}
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">FG%</div>
              <div className="text-yellow-400 font-bold text-lg">
                {seasonStats.fieldGoalPercentage}%
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">3P%</div>
              <div className="text-purple-400 font-bold text-lg">
                {seasonStats.threePointPercentage}%
              </div>
            </div>
            <div className="bg-white/5 p-2 rounded">
              <div className="text-gray-400">FT%</div>
              <div className="text-pink-400 font-bold text-lg">
                {seasonStats.freeThrowPercentage}%
              </div>
            </div>
          </div>
          <div className="mt-3 text-center text-gray-400 text-xs">
            Games Played: {seasonStats.gamesPlayed}
          </div>
        </div>
      )}
    </div>
  );
}
