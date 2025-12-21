'use client';

import { useState, useEffect, useRef } from 'react';
import { useDebounce } from 'use-debounce';
import { searchPlayers } from '@/lib/api';
import { Player } from '@/lib/api';
import SearchBar from './SearchBar';
import PlayerCard from './PlayerCard';

// Popular player search terms to fetch top players
const POPULAR_PLAYERS_SEARCH_TERMS = [
  'LeBron', 'Curry', 'Durant', 'Giannis', 'Luka', 
  'Tatum', 'Embiid', 'Jokic', 'Booker', 'Lillard'
];

export default function StatsDashboard() {
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery] = useDebounce(searchQuery, 400);
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [showTopPlayers, setShowTopPlayers] = useState(true);
  const [topPlayersLoaded, setTopPlayersLoaded] = useState(false);
  const mountedRef = useRef(true);

  // Load top players on mount
  useEffect(() => {
    const loadTopPlayers = async () => {
      if (topPlayersLoaded) return;
      
      try {
        setLoading(true);
        const allPlayers: Player[] = [];
        const seenIds = new Set<number>();

        // Fetch multiple popular players and combine results
        for (const term of POPULAR_PLAYERS_SEARCH_TERMS.slice(0, 5)) {
          try {
            const results = await searchPlayers(term);
            if (results && Array.isArray(results)) {
              // Add unique players only
              results.forEach(player => {
                if (player.id && !seenIds.has(player.id)) {
                  seenIds.add(player.id);
                  allPlayers.push(player);
                }
              });
              // Stop if we have enough
              if (allPlayers.length >= 10) break;
            }
          } catch (err) {
            // Continue with next search term if one fails
            console.warn(`Failed to search for ${term}:`, err);
          }
        }

        if (mountedRef.current) {
          setPlayers(allPlayers.slice(0, 10));
          setTopPlayersLoaded(true);
        }
      } catch (error) {
        console.error('Failed to load top players:', error);
        if (mountedRef.current) {
          setPlayers([]);
        }
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
      }
    };

    loadTopPlayers();

    return () => {
      mountedRef.current = false;
    };
  }, [topPlayersLoaded]);

  // Handle search
  useEffect(() => {
    const performSearch = async () => {
      if (!debouncedQuery.trim()) {
        // If search is cleared, show top players again
        if (!topPlayersLoaded) {
          setShowTopPlayers(true);
          return;
        }
        // Reload top players if we had them before
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
        console.error('Search failed:', error);
        if (mountedRef.current) {
          setPlayers([]);
        }
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
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

