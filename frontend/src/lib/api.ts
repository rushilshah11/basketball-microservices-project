// API Service Functions
import { redirect } from "next/navigation";
// Configuration
const SERVER_API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
const CLIENT_PROXY_URL = "/api/proxy";

// --- Interfaces ---

export interface AuthResponse {
  token: string;
}

export interface Player {
  id: number;
  fullName: string; // UPDATED: Matches backend
  teamName: string; // UPDATED: Matches backend
  position?: string;
  [key: string]: any;
}

export interface WatchlistItem {
  id: number;
  playerName: string;
  // playerId: number;
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
async function fetchAPI<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> {
  const isServer = typeof window === "undefined";
  let url = "";
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (isServer) {
    // SERVER: Direct call to Java Backend
    url = `${SERVER_API_URL}/api/${endpoint}`;

    // Dynamically import headers to prevent Client Component build errors
    const { cookies } = await import("next/headers");
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (token) {
      // @ts-ignore
      headers["Authorization"] = `Bearer ${token}`;
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

  if (response.status === 403) {
    if (isServer) {
      // Server-side redirect (throws an internal error to handle navigation)
      redirect("/login");
    } else {
      // Client-side redirect
      window.location.href = "/login";
      // Return a dummy value to satisfy TypeScript while the page redirects
      return null as T;
    }
  }

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

export async function register(
  name: string,
  email: string,
  password: string,
): Promise<AuthResponse> {
  // Endpoint: /api/v1/auth/register
  // Split full name into first and last name
  const [firstname, ...lastnameParts] = name.split(" ");
  const lastname = lastnameParts.join(" ") || ""; // Handle case where no last name is provided

  // Endpoint: /api/v1/auth/register
  return fetchAPI<AuthResponse>("v1/auth/register", {
    method: "POST",
    // Update the JSON body to match RegisterRequest.java
    body: JSON.stringify({ firstname, lastname, email, password }),
  });
}

export async function login(
  email: string,
  password: string,
): Promise<AuthResponse> {
  // Endpoint: /api/v1/auth/authenticate
  return fetchAPI<AuthResponse>("v1/auth/authenticate", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

// --- Stats API ---

export async function searchPlayers(name: string): Promise<Player[]> {
  // Endpoint: /api/players/search
  return fetchAPI<Player[]>(`players/search?name=${encodeURIComponent(name)}`);
}

export async function getPlayersBatch(names: string[]): Promise<Player[]> {
  // Endpoint: /api/players/batch
  return fetchAPI<Player[]>("players/batch", {
    method: "POST",
    body: JSON.stringify(names),
  });
}

export async function getTrendingPlayers(): Promise<Player[]> {
  // Endpoint: /api/players/trending
  return fetchAPI<Player[]>("players/trending");
}

export async function getPlayerStats(name: string): Promise<PlayerStats> {
  // Endpoint: /api/players/stats
  return fetchAPI<PlayerStats>(
    `players/stats?name=${encodeURIComponent(name)}`,
  );
}

export async function getPlayerGames(name: string): Promise<GameLog[]> {
  // Endpoint: /api/players/games
  return fetchAPI<GameLog[]>(
    `players/games?name=${encodeURIComponent(name)}&limit=5`,
  );
}

// --- Watchlist API ---

// CHANGE: Accept string playerName instead of number id
export async function addToWatchlist(
  playerName: string,
  userId: number = 1,
): Promise<void> {
  // Endpoint: /api/watchlists
  await fetchAPI("watchlists", {
    method: "POST",
    body: JSON.stringify({ playerName }), // Send object with playerName
  });
}

// CHANGE: Accept string playerName
export async function removeFromWatchlist(
  playerName: string,
  userId: number = 1,
): Promise<void> {
  // Endpoint: /api/watchlists/{name}
  await fetchAPI(`watchlists/${encodeURIComponent(playerName)}`, {
    method: "DELETE",
  });
}

// CHANGE: Accept string playerName
export async function checkWatchlist(
  playerName: string,
  userId: number = 1,
): Promise<boolean> {
  try {
    // Endpoint: /api/watchlists/check/{name}
    const data = await fetchAPI<any>(
      `watchlists/check/${encodeURIComponent(playerName)}`,
    );
    return data.message === "Player is in watchlist";
  } catch {
    return false;
  }
}

export async function getWatchlist(
  userId: number = 1,
): Promise<WatchlistItem[]> {
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

function transformPredictionResponse(data: any): Prediction {
  // Handle both camelCase (direct endpoint) and snake_case (database format)
  const predictedStats = data.predicted_stats || {};

  return {
    playerName: data.playerName || data.player_name || "",
    predictedPoints: data.predictedPoints ?? predictedStats.pts ?? 0,
    predictedAssists: data.predictedAssists ?? predictedStats.ast ?? 0,
    predictedRebounds: data.predictedRebounds ?? predictedStats.reb ?? 0,
    confidence: data.confidence ?? 0,
    createdAt: data.createdAt || data.created_at || new Date().toISOString(),
  };
}

export async function getPrediction(playerName: string): Promise<Prediction> {
  // Endpoint: /api/predictions/{name}
  const data = await fetchAPI<any>(
    `predictions/${encodeURIComponent(playerName)}`,
  );
  return transformPredictionResponse(data);
}

export async function getPredictionsBatch(
  playerNames: string[],
): Promise<Record<string, Prediction>> {
  const predictions: Record<string, Prediction> = {};

  // Batch requests in groups of 10 to avoid overwhelming the service
  for (let i = 0; i < playerNames.length; i += 10) {
    const batch = playerNames.slice(i, i + 10);

    try {
      const results = await Promise.all(
        batch.map((name) =>
          fetchAPI<any>(`predictions/${encodeURIComponent(name)}`).catch(
            (err) => {
              console.warn(`Failed to get prediction for ${name}:`, err);
              return null;
            },
          ),
        ),
      );

      batch.forEach((name, idx) => {
        if (results[idx]) {
          predictions[name] = transformPredictionResponse(results[idx]);
        }
      });

      // Small delay between batches to respect rate limits
      if (i + 10 < playerNames.length) {
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
    } catch (err) {
      console.error("Batch prediction failed:", err);
    }
  }

  return predictions;
}
