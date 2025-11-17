# Stats Service - Usage Examples

## Complete Workflow Examples

### 1. Add a Player to Watchlist

#### Step 1: Search for a player
```bash
curl -X GET "http://localhost:8081/api/players/search?name=LeBron"
```

**Response:**
```json
[
  {
    "id": 265,
    "fullName": "LeBron James"
  }
]
```

#### Step 2: Add player to watchlist using the ID
```bash
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 265}'
```

**Response:**
```json
{
  "id": 1,
  "playerId": 265,
  "userId": 1,
  "message": "Player added to watchlist successfully"
}
```

### 2. View Your Watchlist with Player Details

#### Step 1: Get your watchlist (player IDs)
```bash
curl -X GET "http://localhost:8081/api/watchlists?userId=1"
```

**Response:**
```json
[
  {
    "id": 1,
    "playerId": 265,
    "userId": 1
  },
  {
    "id": 2,
    "playerId": 237,
    "userId": 1
  }
]
```

#### Step 2: Fetch player details for watchlist
```bash
curl -X GET "http://localhost:8081/api/players/batch?playerIds=265,237"
```

**Response:**
```json
[
  {
    "id": 265,
    "fullName": "LeBron James"
  },
  {
    "id": 237,
    "fullName": "Stephen Curry"
  }
]
```

### 3. Remove Player from Watchlist

```bash
curl -X DELETE "http://localhost:8081/api/watchlists/265?userId=1"
```

**Response:**
```json
{
  "playerId": 265,
  "userId": 1,
  "message": "Player removed from watchlist successfully"
}
```

### 4. Check if Player is in Watchlist

```bash
curl -X GET "http://localhost:8081/api/watchlists/check/265?userId=1"
```

**Response:**
```json
{
  "playerId": 265,
  "userId": 1,
  "message": "Player is in watchlist"
}
```

## Frontend Integration Example (React/JavaScript)

### Search and Add Player

```javascript
// 1. Search for player
async function searchPlayer(name) {
  const response = await fetch(
    `http://localhost:8081/api/players/search?name=${encodeURIComponent(name)}`
  );
  return await response.json();
}

// 2. Add player to watchlist
async function addToWatchlist(playerId, userId) {
  const response = await fetch(
    `http://localhost:8081/api/watchlists?userId=${userId}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ playerId })
    }
  );
  return await response.json();
}

// Complete flow
async function searchAndAddPlayer() {
  // User searches for "LeBron"
  const players = await searchPlayer("LeBron");
  
  // Display results to user, user selects player with id 265
  const selectedPlayer = players[0]; // LeBron James
  
  // Add to watchlist
  const result = await addToWatchlist(selectedPlayer.id, 1);
  console.log(result.message); // "Player added to watchlist successfully"
}
```

### Display Watchlist with Player Details

```javascript
async function getWatchlistWithDetails(userId) {
  // 1. Get watchlist (just IDs)
  const watchlistResponse = await fetch(
    `http://localhost:8081/api/watchlists?userId=${userId}`
  );
  const watchlist = await watchlistResponse.json();
  
  if (watchlist.length === 0) {
    return [];
  }
  
  // 2. Extract player IDs
  const playerIds = watchlist.map(item => item.playerId).join(',');
  
  // 3. Fetch player details
  const playersResponse = await fetch(
    `http://localhost:8081/api/players/batch?playerIds=${playerIds}`
  );
  const players = await playersResponse.json();
  
  // 4. Combine data
  return watchlist.map(item => ({
    watchlistId: item.id,
    player: players.find(p => p.id === item.playerId)
  }));
}

// Usage
getWatchlistWithDetails(1).then(watchlist => {
  watchlist.forEach(item => {
    console.log(`${item.player.fullName}`);
  });
});
```

### Remove from Watchlist

```javascript
async function removeFromWatchlist(playerId, userId) {
  const response = await fetch(
    `http://localhost:8081/api/watchlists/${playerId}?userId=${userId}`,
    {
      method: 'DELETE'
    }
  );
  return await response.json();
}

// Usage
removeFromWatchlist(265, 1).then(result => {
  console.log(result.message); // "Player removed from watchlist successfully"
});
```

## Error Handling Examples

### Player Already in Watchlist

```bash
# Try to add same player twice
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 265}'
```

**Response (400 Bad Request):**
```json
{
  "message": "Player already in watchlist"
}
```

### Player Not Found

```bash
curl -X GET "http://localhost:8081/api/players/99999"
```

**Response (404 Not Found)**

### Remove Non-Existent Player from Watchlist

```bash
curl -X DELETE "http://localhost:8081/api/watchlists/99999?userId=1"
```

**Response (404 Not Found):**
```json
{
  "message": "Player not found in watchlist"
}
```

## Testing with cURL

### Complete Test Sequence

```bash
# 1. Search for LeBron
curl -X GET "http://localhost:8081/api/players/search?name=LeBron"

# 2. Add to watchlist (assuming playerId is 265)
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 265}'

# 3. Search for Curry
curl -X GET "http://localhost:8081/api/players/search?name=Curry"

# 4. Add Curry to watchlist (assuming playerId is 237)
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 237}'

# 5. View watchlist
curl -X GET "http://localhost:8081/api/watchlists?userId=1"

# 6. Get player details
curl -X GET "http://localhost:8081/api/players/batch?playerIds=265,237"

# 7. Remove LeBron from watchlist
curl -X DELETE "http://localhost:8081/api/watchlists/265?userId=1"

# 8. Verify removal
curl -X GET "http://localhost:8081/api/watchlists?userId=1"
```

## Notes

- **userId parameter is temporary**: In production, this will be extracted from JWT token
- **Player search is case-insensitive**: Searching for "lebron", "LeBron", or "LEBRON" all work
- **Player details are always fresh**: Fetched in real-time from NBA API
- **Watchlist is persistent**: Stored in PostgreSQL database

