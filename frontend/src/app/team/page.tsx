'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  getWatchlist,
  getPlayersBatch,
  getPrediction,
  Player,
  WatchlistItem,
  Prediction,
} from '@/lib/api';
import { isAuthenticated } from '@/lib/auth';

interface PlayerWithPrediction extends Player {
  prediction?: Prediction;
  loading?: boolean;
}

export default function TeamPage() {
  const [players, setPlayers] = useState<PlayerWithPrediction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated()) {
      router.push('/login');
      return;
    }

    loadTeamData();
  }, [router]);

  const loadTeamData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Step 1: Fetch user's watchlist
      const watchlist: WatchlistItem[] = await getWatchlist(1);
      
      if (watchlist.length === 0) {
        setPlayers([]);
        setLoading(false);
        return;
      }

      const playerNames = watchlist.map((item) => item.playerName)
            .filter((name): name is string => typeof name === 'string' && name.length > 0);

      if (playerNames.length === 0) {
          setPlayers([]);
          setLoading(false);
          return;
      }
      // Step 3: Fetch player details
      const playerDetails: Player[] = await getPlayersBatch(playerNames);

      // Step 4: Fetch predictions for each player
      const playersWithPredictions = await Promise.all(
        playerDetails.map(async (player) => {
          try {
            const prediction = await getPrediction(player.fullName);
            return { ...player, prediction, loading: false };
          } catch (err) {
            console.error(`Failed to get prediction for ${player.fullName}:`, err);
            return { ...player, loading: false };
          }
        })
      );

      setPlayers(playersWithPredictions);
    } catch (err) {
      console.error('Failed to load team data:', err);
      setError('Failed to load team data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
          <p className="mt-4 text-gray-300">Loading your team...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-red-500/20 border border-red-500 rounded-lg p-6 text-center">
          <p className="text-red-200">{error}</p>
          <button
            onClick={loadTeamData}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-2">
          My Team Predictions
        </h1>
        <p className="text-gray-300 text-lg">
          AI-powered predictions for your watchlist players
        </p>
      </div>

      {players.length === 0 ? (
        <div className="text-center py-12 bg-white/10 backdrop-blur-sm rounded-xl border border-white/20">
          <div className="text-6xl mb-4">📋</div>
          <h2 className="text-2xl font-semibold text-white mb-2">No Players in Watchlist</h2>
          <p className="text-gray-300 mb-6">
            Add players to your watchlist to see predictions here
          </p>
          <a
            href="/"
            className="inline-block px-6 py-3 bg-orange-600 text-white font-semibold rounded-lg hover:bg-orange-700 transition-colors"
          >
            Browse Players
          </a>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {players.map((player) => (
            <PlayerPredictionCard key={player.id} player={player} />
          ))}
        </div>
      )}
    </div>
  );
}

function PlayerPredictionCard({ player }: { player: PlayerWithPrediction }) {
  return (
    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border-2 border-white/20 hover:border-orange-500/50 transition-all duration-300 shadow-lg hover:shadow-xl">
      <div className="mb-4">
        <h3 className="text-2xl font-bold text-white mb-1">{player.fullName}</h3>
        <div className="flex items-center space-x-4 text-gray-300 text-sm">
          <span>🏀 {player.teamName || 'N/A'}</span>
          <span>📍 {player.position || 'N/A'}</span>
        </div>
      </div>

      {player.prediction ? (
        <div className="space-y-4">
          <div className="bg-gradient-to-r from-orange-600/20 to-red-600/20 rounded-lg p-4 border border-orange-500/30">
            <h4 className="text-sm font-semibold text-orange-300 mb-3 uppercase tracking-wide">
              🔮 AI Predictions
            </h4>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-gray-300">Points</span>
                <span className="text-orange-400 font-bold text-xl">
                  {player.prediction.predictedPoints?.toFixed(1) || 'N/A'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-300">Assists</span>
                <span className="text-blue-400 font-bold text-xl">
                  {player.prediction.predictedAssists?.toFixed(1) || 'N/A'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-300">Rebounds</span>
                <span className="text-green-400 font-bold text-xl">
                  {player.prediction.predictedRebounds?.toFixed(1) || 'N/A'}
                </span>
              </div>
            </div>
          </div>

          {/* Current Stats Comparison (if available) */}
          {player.prediction.currentPoints && (
            <div className="bg-black/20 rounded-lg p-4 border border-white/10">
              <h4 className="text-sm font-semibold text-gray-400 mb-2 uppercase">
                Current Season Avg
              </h4>
              <div className="grid grid-cols-3 gap-2 text-xs">
                <div>
                  <div className="text-gray-400">PTS</div>
                  <div className="text-white font-semibold">
                    {player.prediction.currentPoints?.toFixed(1)}
                  </div>
                </div>
                <div>
                  <div className="text-gray-400">AST</div>
                  <div className="text-white font-semibold">
                    {player.prediction.currentAssists?.toFixed(1)}
                  </div>
                </div>
                <div>
                  <div className="text-gray-400">REB</div>
                  <div className="text-white font-semibold">
                    {player.prediction.currentRebounds?.toFixed(1)}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="text-center py-4 text-gray-400">
          <p>Prediction unavailable</p>
        </div>
      )}
    </div>
  );
}

