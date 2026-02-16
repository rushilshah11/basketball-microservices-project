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
  const stats = data.predicted_stats || {};

  return {
    playerName: data.player_name || data.playerName || "",
    // FIXED: Backend sends predicted_stats as {pts, ast, reb} not {points, assists, rebounds}
    predictedPoints: stats.pts ?? 0,
    predictedAssists: stats.ast ?? 0,
    predictedRebounds: stats.reb ?? 0,
    confidence: data.confidence ?? 0,
    createdAt: data.created_at || data.createdAt || new Date().toISOString(),
  };
}

export async function getPrediction(playerName: string): Promise<Prediction> {
  try {
    // Try to get existing prediction first
    const data = await fetchAPI<any>(
      `predictions/${encodeURIComponent(playerName)}`,
    );
    return transformPredictionResponse(data);
  } catch (error: any) {
    // Check if this is a 404 or "not found" error
    const errorStr = (error.message || "").toLowerCase();
    const is404 =
      errorStr.includes("404") ||
      errorStr.includes("not found") ||
      errorStr.includes("no prediction");

    if (is404) {
      console.log(`No prediction found for ${playerName}, creating one...`);

      try {
        // Get player stats first
        const stats = await getPlayerStats(playerName);

        // Create prediction using POST /predict endpoint
        const predictionRequest = {
          playerName,
          currentStats: {
            ppg: stats.pointsPerGame,
            apg: stats.assistsPerGame,
            rpg: stats.reboundsPerGame,
            fgPct: stats.fieldGoalPercentage / 100,
            ftPct: stats.freeThrowPercentage / 100,
            gamesPlayed: stats.gamesPlayed,
            minutesPerGame: 30.0,
            stealsPerGame: 1.0,
            blocksPerGame: 0.5,
            turnoversPerGame: 2.0,
          },
          homeGame: true,
        };

        console.log(`Creating prediction for ${playerName}...`);
        // Call POST /predict to create the prediction
        const newPrediction = await fetchAPI<any>("predict", {
          method: "POST",
          body: JSON.stringify(predictionRequest),
        });

        console.log(`✅ Prediction created for ${playerName}`);
        return transformPredictionResponse(newPrediction);
      } catch (createError: any) {
        console.error(
          `Failed to create prediction for ${playerName}:`,
          createError,
        );
        // Return a placeholder if creation fails
        return {
          playerName,
          predictedPoints: 0,
          predictedAssists: 0,
          predictedRebounds: 0,
          confidence: 0,
          createdAt: new Date().toISOString(),
        };
      }
    }
    // If it's not a 404 error, return a placeholder
    console.error(`Error fetching prediction for ${playerName}:`, error);
    return {
      playerName,
      predictedPoints: 0,
      predictedAssists: 0,
      predictedRebounds: 0,
      confidence: 0,
      createdAt: new Date().toISOString(),
    };
  }
}

export async function getPredictionsBatch(
  playerNames: string[],
): Promise<Record<string, Prediction>> {
  const predictions: Record<string, Prediction> = {};

  // Process in smaller batches to avoid timeout
  for (let i = 0; i < playerNames.length; i += 3) {
    const batch = playerNames.slice(i, i + 3);

    try {
      const results = await Promise.all(
        batch.map((name) =>
          getPrediction(name).catch((err) => {
            console.warn(`Failed to get prediction for ${name}:`, err);
            return {
              playerName: name,
              predictedPoints: 0,
              predictedAssists: 0,
              predictedRebounds: 0,
              confidence: 0,
              createdAt: new Date().toISOString(),
            };
          }),
        ),
      );

      batch.forEach((name, idx) => {
        if (results[idx]) {
          predictions[name] = results[idx];
        }
      });

      // Small delay between batches
      if (i + 3 < playerNames.length) {
        await new Promise((resolve) => setTimeout(resolve, 200));
      }
    } catch (err) {
      console.error("Batch prediction failed:", err);
    }
  }

  return predictions;
}
