'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { getMyTeam, Player } from '@/lib/api';
import { useSession } from '@/lib/useSession';
import PlayerCard from '@/components/PlayerCard';

export default function TeamPage() {
  const session = useSession();
  const router = useRouter();
  const [players, setPlayers] = useState<Player[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (session === null) router.push('/login');
    if (!session) return;
    getMyTeam()
      .then(setPlayers)
      .catch((err) => {
        console.error(err);
        setError('Failed to load your team.');
      });
  }, [session, router]);

  // Drop a card as soon as the player is removed
  const handleTeamChange = (playerId: number, onTeam: boolean) => {
    if (!onTeam) setPlayers((current) => current?.filter((p) => p.id !== playerId) ?? null);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-2">My Team</h1>
        <p className="text-gray-300 text-lg">Next-game predictions are coming soon</p>
      </div>

      {error ? (
        <div className="bg-red-500/20 border border-red-500 rounded-lg p-6 text-center text-red-200">{error}</div>
      ) : players === null ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
        </div>
      ) : players.length === 0 ? (
        <div className="text-center py-12 bg-white/10 backdrop-blur-sm rounded-xl border border-white/20">
          <div className="text-6xl mb-4">🏀</div>
          <h2 className="text-2xl font-semibold text-white mb-2">No players on your team yet</h2>
          <Link
            href="/"
            className="inline-block mt-4 px-6 py-3 bg-orange-600 text-white font-semibold rounded-lg hover:bg-orange-700 transition-colors"
          >
            Browse Players
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {players.map((player) => (
            <PlayerCard key={player.id} player={player} initialOnTeam onTeamChange={handleTeamChange} />
          ))}
        </div>
      )}
    </div>
  );
}
