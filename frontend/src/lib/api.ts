// API Service Functions

// Configuration
const SERVER_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const CLIENT_PROXY_URL = '/api/proxy';

// --- Interfaces ---

export interface AuthResponse {
    token: string;
}

export interface Player {
    id: number;
    fullName: string;      // UPDATED: Matches backend
    teamName: string;      // UPDATED: Matches backend
    position?: string;
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

export interface PlayerStats {
    pointsPerGame: number;
    assistsPerGame: number;
    reboundsPerGame: number;
    fieldGoalPercentage: number;
    threePointPercentage: number;
    freeThrowPercentage: number;
    gamesPlayed: number;
}

export interface GameLog {
    date: string;
    opponent: string;
    points: number;
    assists: number;
    rebounds: number;
}

// --- Helper: Smart Fetch ---

/**
 * intelligently routes requests:
 * - Server: Direct to Java Backend (reads cookie manually)
 * - Client: Via Next.js Proxy (browser handles cookie)
 */
async function fetchAPI<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const isServer = typeof window === 'undefined';
    let url = '';
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    if (isServer) {
        // SERVER: Direct call to Java Backend
        url = `${SERVER_API_URL}/api/${endpoint}`;

        // Dynamically import headers to prevent Client Component build errors
        const { cookies } = await import('next/headers');
        const cookieStore = await cookies();
        const token = cookieStore.get('token')?.value;

        if (token) {
            // @ts-ignore
            headers['Authorization'] = `Bearer ${token}`;
        }
    } else {
        // CLIENT: Call Next.js Proxy
        // The browser will automatically attach the HttpOnly cookie
        url = `${CLIENT_PROXY_URL}/${endpoint}`;
    }

    const response = await fetch(url, {
        ...options,
        headers,
    });

    if (!response.ok) {
        let errorMessage = `API request failed: ${response.statusText}`;
        try {
            const errorData = await response.json();
            if (errorData.message) errorMessage = errorData.message;
        } catch (e) {
            // ignore JSON parse error
        }
        throw new Error(errorMessage);
    }

    return response.json();
}

// --- Auth API ---

// These are public, so we can use fetchAPI (which will just not add a token if none exists)
// or call direct. using fetchAPI ensures consistency.

export async function register(name: string, email: string, password: string): Promise<AuthResponse> {
    // Endpoint: /api/v1/auth/register
    return fetchAPI<AuthResponse>('v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
    });
}

export async function login(email: string, password: string): Promise<AuthResponse> {
    // Endpoint: /api/v1/auth/authenticate
    return fetchAPI<AuthResponse>('v1/auth/authenticate', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
    });
}

// --- Stats API ---

export async function searchPlayers(name: string): Promise<Player[]> {
    // Endpoint: /api/players/search
    return fetchAPI<Player[]>(`players/search?name=${encodeURIComponent(name)}`);
}

export async function getPlayersBatch(playerIds: number[]): Promise<Player[]> {
    // Endpoint: /api/players/batch
    return fetchAPI<Player[]>(`players/batch?playerIds=${playerIds.join(',')}`);
}

export async function getTrendingPlayers(): Promise<Player[]> {
    // Endpoint: /api/players/trending
    return fetchAPI<Player[]>('players/trending');
}

export async function getPlayerStats(name: string): Promise<PlayerStats> {
    // Endpoint: /api/players/stats
    return fetchAPI<PlayerStats>(`players/stats?name=${encodeURIComponent(name)}`);
}

export async function getPlayerGames(name: string): Promise<GameLog[]> {
    // Endpoint: /api/players/games
    return fetchAPI<GameLog[]>(`players/games?name=${encodeURIComponent(name)}&limit=5`);
}

// --- Watchlist API ---

export async function addToWatchlist(playerId: number, userId: number = 1): Promise<void> {
    // Endpoint: /api/watchlists
    await fetchAPI('watchlists?userId=' + userId, {
        method: 'POST',
        body: JSON.stringify({ playerId }),
    });
}

export async function removeFromWatchlist(playerId: number, userId: number = 1): Promise<void> {
    // Endpoint: /api/watchlists/{id}
    await fetchAPI(`watchlists/${playerId}?userId=${userId}`, {
        method: 'DELETE',
    });
}

export async function checkWatchlist(playerId: number, userId: number = 1): Promise<boolean> {
    try {
        const data = await fetchAPI<any>(`watchlists/check/${playerId}?userId=${userId}`);
        return data.isInWatchlist === true || data === true;
    } catch {
        return false;
    }
}

export async function getWatchlist(userId: number = 1): Promise<WatchlistItem[]> {
    try {
        const data = await fetchAPI<any>(`watchlists?userId=${userId}`);

        if (Array.isArray(data)) return data;

        // Handle nested object response if necessary
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

// --- Prediction API ---

export async function getPrediction(playerName: string): Promise<Prediction> {
    // Endpoint: /api/predictions/{name}
    return fetchAPI<Prediction>(`predictions/${encodeURIComponent(playerName)}`);
}