// API Service Functions
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface AuthResponse {
  token: string;
}

export interface Player {
  id: number;
  name: string;
  team: string;
  position: string;
  [key: string]: any;
}

export interface WatchlistItem {
  id: number;
  playerId: number;
  userId: number;
}

export interface Prediction {
  playerName: string;
  predictedPoints?: number;
  predictedAssists?: number;
  predictedRebounds?: number;
  [key: string]: any;
}

export interface GameStats {
  points: number;
  assists: number;
  rebounds: number;
  [key: string]: any;
}

// Get auth token from cookies (client-side)
export function getAuthToken(): string | null {
  if (typeof document === 'undefined') return null;
  const cookies = document.cookie.split(';');
  const tokenCookie = cookies.find(c => c.trim().startsWith('token='));
  return tokenCookie ? tokenCookie.split('=')[1] : null;
}

// Auth API
export async function register(name: string, email: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  });
  if (!response.ok) throw new Error('Registration failed');
  return response.json();
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/authenticate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error('Authentication failed');
  return response.json();
}

// Stats API
export async function searchPlayers(name: string): Promise<Player[]> {
  const token = getAuthToken();
  const headers: HeadersInit = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  
  const response = await fetch(`${API_BASE_URL}/api/players/search?name=${encodeURIComponent(name)}`, {
    headers,
  });
  if (!response.ok) throw new Error('Search failed');
  return response.json();
}

export async function getPlayersBatch(playerIds: number[]): Promise<Player[]> {
  const token = getAuthToken();
  const headers: HeadersInit = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  
  const response = await fetch(`${API_BASE_URL}/api/players/batch?playerIds=${playerIds.join(',')}`, {
    headers,
  });
  if (!response.ok) throw new Error('Failed to fetch players');
  return response.json();
}

export async function addToWatchlist(playerId: number, userId: number = 1): Promise<void> {
  const token = getAuthToken();
  if (!token) throw new Error('Not authenticated');
  
  const response = await fetch(`${API_BASE_URL}/api/watchlists?userId=${userId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ playerId }),
  });
  if (!response.ok) throw new Error('Failed to add to watchlist');
}

export async function removeFromWatchlist(playerId: number, userId: number = 1): Promise<void> {
  const token = getAuthToken();
  if (!token) throw new Error('Not authenticated');
  
  const response = await fetch(`${API_BASE_URL}/api/watchlists/${playerId}?userId=${userId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  if (!response.ok) throw new Error('Failed to remove from watchlist');
}

export async function checkWatchlist(playerId: number, userId: number = 1): Promise<boolean> {
  const token = getAuthToken();
  if (!token) return false;
  
  try {
    const response = await fetch(`${API_BASE_URL}/api/watchlists/check/${playerId}?userId=${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    if (!response.ok) return false;
    const data = await response.json();
    return data.isInWatchlist === true || data === true;
  } catch {
    return false;
  }
}

export async function getWatchlist(userId: number = 1): Promise<WatchlistItem[]> {
  const token = getAuthToken();
  if (!token) return [];
  
  try {
    const response = await fetch(`${API_BASE_URL}/api/watchlists?userId=${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    if (!response.ok) return [];
    const data = await response.json();
    // Handle both array and object responses
    if (Array.isArray(data)) return data;
    // If backend returns { playerIds: [...] }, extract them
    if (data.playerIds && Array.isArray(data.playerIds)) {
      return data.playerIds.map((id: number, index: number) => ({
        id: index + 1,
        playerId: id,
        userId,
      }));
    }
    return [];
  } catch {
    return [];
  }
}

// Prediction API
export async function getPrediction(playerName: string): Promise<Prediction> {
  const token = getAuthToken();
  const headers: HeadersInit = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  
  const response = await fetch(`${API_BASE_URL}/api/predictions/${encodeURIComponent(playerName)}`, {
    headers,
  });
  if (!response.ok) throw new Error('Failed to get prediction');
  return response.json();
}

