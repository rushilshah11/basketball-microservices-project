'use client';

import { useEffect, useState } from 'react';
import { searchPlayers, getTopPlayers, getMyTeam, Player } from '@/lib/api';
import SearchBar from './SearchBar';
import PlayerCard from './PlayerCard';

export default function StatsDashboard() {
  const [searchQuery, setSearchQuery] = useState('');
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [teamIds, setTeamIds] = useState<Set<number> | null>(null);

  // Which players are already on my team (so cards show "Remove" instead of "Add")
  useEffect(() => {
    getMyTeam().then((team) => setTeamIds(new Set(team.map((p) => p.id))));
  }, []);

  // Empty search box -> top scorers. Otherwise -> search results.
  useEffect(() => {
    let cancelled = false; // ignore responses from older keystrokes
    const query = searchQuery.trim();

    (query ? searchPlayers(query) : getTopPlayers())
      .then((results) => !cancelled && setPlayers(results))
      .catch((err) => {
        console.error('Failed to load players:', err);
        if (!cancelled) setPlayers([]);
      })
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [searchQuery]);

  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    setLoading(true);
  };

  const query = searchQuery.trim();

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-2">NBA Stats Dashboard</h1>
        <p className="text-gray-300 text-lg">Search for players and add them to your team</p>
      </div>

      <SearchBar value={searchQuery} onChange={handleSearchChange} />

      <div className="mt-8">
        {query ? (
          <h2 className="text-2xl font-bold text-white mb-4">Search Results for &quot;{query}&quot;</h2>
        ) : (
          <>
            <h2 className="text-2xl font-bold text-white mb-1">⭐ Top Scorers This Season</h2>
            <p className="text-gray-400 text-sm mb-4">Start typing to search for specific players</p>
          </>
        )}
      </div>

      {loading || teamIds === null ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
        </div>
      ) : players.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
          {players.map((player) => (
            <PlayerCard key={player.id} player={player} initialOnTeam={teamIds.has(player.id)} />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-white/5 rounded-xl border border-white/10">
          <div className="text-6xl mb-4">{query ? '🔍' : '🏀'}</div>
          <p className="text-gray-300 text-lg">
            {query ? `No players matching "${query}".` : 'No players yet. Run pipeline/seed.py to load data.'}
          </p>
        </div>
      )}
    </div>
  );
}
