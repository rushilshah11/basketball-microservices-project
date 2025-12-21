'use client';

import { useState, useEffect, useRef } from 'react';
import { useDebounce } from 'use-debounce';
import { searchPlayers, getTrendingPlayers, Player } from '@/lib/api'; // Import getTrendingPlayers
import SearchBar from './SearchBar';
import PlayerCard from './PlayerCard';

export default function StatsDashboard() {
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedQuery] = useDebounce(searchQuery, 400);
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [showTopPlayers, setShowTopPlayers] = useState(true);
  const [topPlayersLoaded, setTopPlayersLoaded] = useState(false);

  // Use a ref to prevent state updates on unmounted component
  const mountedRef = useRef(true);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // LOAD TOP PLAYERS (Efficiently)
  useEffect(() => {
    const loadTopPlayers = async () => {
      if (topPlayersLoaded) return;

      try {
        setLoading(true);
        // Call the backend endpoint dedicated to this list
        const trending = await getTrendingPlayers();

        if (mountedRef.current) {
          setPlayers(trending);
          setTopPlayersLoaded(true);
        }
      } catch (error) {
        console.error("Failed to load top players:", error);
      } finally {
        if (mountedRef.current) setLoading(false);
      }
    };

    loadTopPlayers();
  }, [topPlayersLoaded]);

  // Handle search (Existing logic is fine, just ensure it toggles correctly)
  useEffect(() => {
    const performSearch = async () => {
      if (!debouncedQuery.trim()) {
        if (topPlayersLoaded) {
          // Re-fetch trending if we cleared search, or just rely on state if you persisted it
          const trending = await getTrendingPlayers();
          if (mountedRef.current) setPlayers(trending);
        }
        setShowTopPlayers(true);
        return;
      }

      setShowTopPlayers(false);
      setLoading(true);
      try {
        const results = await searchPlayers(debouncedQuery);
        if (mountedRef.current) {
          setPlayers(Array.isArray(results) ? results : []);
        }
      } catch (error) {
        console.error("Search failed:", error);
        if (mountedRef.current) setPlayers([]);
      } finally {
        if (mountedRef.current) setLoading(false);
      }
    };

    performSearch();
  }, [debouncedQuery, topPlayersLoaded]);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-2">
          NBA Stats Dashboard
        </h1>
        <p className="text-gray-300 text-lg">
          Search for players and explore their statistics
        </p>
      </div>

      <SearchBar value={searchQuery} onChange={setSearchQuery} />

      {loading && (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
          <p className="mt-4 text-gray-300">Loading...</p>
        </div>
      )}

      {!loading && (
        <>
          {showTopPlayers && players.length > 0 && (
            <div className="mt-8">
              <h2 className="text-2xl font-bold text-white mb-4 flex items-center">
                <span className="mr-2">⭐</span>
                Top Players
              </h2>
              <p className="text-gray-400 text-sm mb-4">
                Start typing to search for specific players
              </p>
            </div>
          )}
          
          {!showTopPlayers && debouncedQuery && (
            <div className="mt-8">
              <h2 className="text-2xl font-bold text-white mb-4">
                Search Results for "{debouncedQuery}"
              </h2>
              <p className="text-gray-400 text-sm mb-4">
                Found {players.length} {players.length === 1 ? 'player' : 'players'}
              </p>
            </div>
          )}

          {players.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
              {players.map((player) => (
                <PlayerCard key={player.id} player={player} />
              ))}
            </div>
          ) : (
            !loading && (
              <div className="text-center py-12 bg-white/5 rounded-xl border border-white/10">
                {debouncedQuery ? (
                  <>
                    <div className="text-6xl mb-4">🔍</div>
                    <p className="text-gray-300 text-lg mb-2">No players found</p>
                    <p className="text-gray-400">
                      No results matching "{debouncedQuery}". Try a different search term.
                    </p>
                  </>
                ) : (
                  <>
                    <div className="text-6xl mb-4">🏀</div>
                    <p className="text-gray-300 text-lg">Loading top players...</p>
                  </>
                )}
              </div>
            )
          )}
        </>
      )}
    </div>
  );
}

