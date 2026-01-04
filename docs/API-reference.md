# API Reference

This document provides a comprehensive list of all public REST endpoints available in the Basketball Microservices Project. The system uses an API Gateway as the single entry point for all requests.

## 1. Authentication Service

**Base Path:** `/api/v1/auth`

Handles user registration, login, and secure logout. These endpoints are the only ones that do not require an existing token.

| Endpoint | Method | Description | Request Body Example |
|----------|--------|-------------|---------------------|
| `/register` | POST | Creates a new user account and returns a JWT token. | `{ "firstname": "Rushil", "lastname": "Shah", "email": "rushil@example.com", "password": "password123" }` |
| `/authenticate` | POST | Validates credentials and returns a JWT token. | `{ "email": "rushil@example.com", "password": "password123" }` |
| `/logout` | POST | Invalidates the current JWT token by adding it to a Redis blacklist. | (None - requires Authorization header) |

## 2. Stats Service (NBA Data & Watchlists)

The Stats Service handles player data and personal user lists. All endpoints (except some search functions, depending on configuration) typically require a valid `Authorization: Bearer <token>` header.

### Player Endpoints

**Base Path:** `/api/players`

| Endpoint | Method | Description | Parameters |
|----------|--------|-------------|------------|
| `/search` | GET | Search for players by name. | `name` (required) |
| `/{playerId}` | GET | Retrieve a specific player by their ID. | `playerId` (path) |
| `/stats` | GET | Get current season averages for a player. | `name` (required) |
| `/batch` | POST | Search for multiple players at once. | `["Name1", "Name2"]` |
| `/trending` | GET | Returns a list of pre-defined top stars. | (None) |
| `/games` | GET | Retrieve recent game logs for a specific player. | `name` (required), `limit` (default: 5) |

### Watchlist Endpoints

**Base Path:** `/api/watchlists`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Get all players in the authenticated user's watchlist. |
| `/details` | GET | Get watchlist players with full stats (utilizes Redis caching). |
| `/` | POST | Add a player to your watchlist. Triggers a background prediction event. |
| `/{playerName}` | DELETE | Remove a player from your watchlist. |
| `/check/{playerName}` | GET | Quickly check if a player is already in your watchlist. |

## 3. Prediction Service

**Base Path:** (Directly accessible or routed via Gateway)

The Prediction Service uses a neural network to estimate player performance for upcoming games.

| Endpoint | Method | Description | Request Body Example |
|----------|--------|-------------|---------------------|
| `/predict` | POST | Generate a prediction based on provided stats. | `{ "playerName": "LeBron James", "currentStats": { "ppg": 25.0, ... }, "homeGame": true }` |
| `/predictions/{player_name}` | GET | Get a stored prediction for a player (or generate one if missing). | (None) |
| `/predictions/{player_name}/refresh` | POST | Force a refresh of a player's prediction data. | (None) |
| `/predictions` | GET | Retrieve all stored predictions in the system. | (None) |
| `/model/info` | GET | Get metadata about the underlying PyTorch model. | (None) |
| `/cache/invalidate` | DELETE | Admin endpoint to clear all cached predictions. | (None) |
| `/health` | GET | Check the health status of the service and model. | (None) |

## 4. Behind the Scenes (gRPC)

While not exposed to the public internet, these services communicate internally via high-speed gRPC calls.

* **Auth Service (Server):** Implements `VerifyToken` to check if a JWT is valid and return the `userId`.
* **Stats Service (Client):** Calls the Auth Service on every protected request to identify the user.
