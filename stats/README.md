# Stats Service

The Stats Service is a microservice that manages NBA player data and user watchlists. It integrates with the [API-NBA from RapidAPI](https://rapidapi.com/api-sports/api/api-nba) to provide real-time player information.

## Features

- 🔍 **Player Search**: Search for NBA players by name
- 📊 **Player Details**: Get detailed information about specific players
- ⭐ **Watchlist Management**: Save favorite players to a personal watchlist
- 🔄 **Real-time Data**: Always fetches fresh player data from NBA API
- 🏗️ **Microservices Architecture**: Registers with Eureka, ready for API Gateway integration

## Architecture

### Services

1. **PlayerService** - Handles NBA API integration
   - Search players by name
   - Get player by ID
   - Batch fetch multiple players

2. **WatchlistService** - Manages user watchlists
   - Add/remove players from watchlist
   - Get user's watchlist
   - Check if player is in watchlist

### Data Model

**Watchlist Table:**
```sql
CREATE TABLE watchlist (
    id          BIGSERIAL PRIMARY KEY,
    player_id   BIGINT NOT NULL,
    user_id     INTEGER NOT NULL
);
```

**Note:** Player details are NOT stored in the database - they're fetched fresh from the NBA API.

## Setup

### Prerequisites

- Java 17
- PostgreSQL
- Maven
- RapidAPI Account with API-NBA subscription

### Configuration

1. **Get RapidAPI Key:**
   - Sign up at [RapidAPI](https://rapidapi.com)
   - Subscribe to [API-NBA](https://rapidapi.com/api-sports/api/api-nba)
   - Copy your API key

2. **Configure application.yml:**
   ```yaml
   nba:
     api:
       base-url: https://api-nba-v1.p.rapidapi.com
       api-key: YOUR_RAPIDAPI_KEY
       api-host: api-nba-v1.p.rapidapi.com
   
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/bball-stats
       username: YOUR_USERNAME
       password: YOUR_PASSWORD
   ```

3. **Create Database:**
   ```bash
   createdb bball-stats
   ```

4. **Configure IntelliJ for Java 17:**
   - File → Project Structure → Project → SDK: Java 17
   - File → Project Structure → Modules → stats → Language level: 17
   - Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE: Java 17

### Running the Service

1. **Start Eureka Server** (port 8761)
2. **Start Stats Service:**
   ```bash
   cd stats
   mvn spring-boot:run
   ```
   
   Or run `StatsApplication.java` from IntelliJ

3. **Service runs on port 8081**

## API Endpoints

### Player Endpoints

#### Search Players
```http
GET /api/players/search?name={playerName}
```
Returns list of players matching the search term (ID and full name only).

#### Get Player by ID
```http
GET /api/players/{playerId}
```
Returns player with ID and full name.

#### Get Multiple Players
```http
GET /api/players/batch?playerIds={id1,id2,id3}
```
Returns player names for multiple IDs (useful for watchlist display).

### Watchlist Endpoints

#### Get User's Watchlist
```http
GET /api/watchlists?userId={userId}
```
Returns list of player IDs in the user's watchlist.

#### Add Player to Watchlist
```http
POST /api/watchlists?userId={userId}
Content-Type: application/json

{
  "playerId": 265
}
```

#### Remove Player from Watchlist
```http
DELETE /api/watchlists/{playerId}?userId={userId}
```

#### Check if Player is in Watchlist
```http
GET /api/watchlists/check/{playerId}?userId={userId}
```

## Project Structure

```
stats/
├── src/main/java/com/
│   ├── bball/stats/
│   │   ├── StatsApplication.java
│   │   ├── config/
│   │   │   ├── NbaApiConfig.java
│   │   │   └── RestTemplateConfig.java
│   │   ├── player/
│   │   │   ├── PlayerController.java
│   │   │   ├── PlayerService.java
│   │   │   ├── PlayerResponse.java
│   │   │   ├── NbaApiResponse.java
│   │   │   └── NbaPlayerDto.java
│   │   └── watchlist/
│   │       ├── WatchlistController.java
│   │       ├── WatchlistService.java
│   │       ├── WatchlistRepository.java
│   │       ├── WatchlistRequest.java
│   │       └── WatchlistResponse.java
│   └── watchlist/
│       └── Watchlist.java (Entity)
├── src/main/resources/
│   ├── application.yml
│   └── application.yml.example
├── pom.xml
├── README.md
├── NBA_API_INTEGRATION.md
└── USAGE_EXAMPLES.md
```

## Workflow Example

### User adds a player to their watchlist:

1. **Frontend:** User searches for "LeBron"
   ```
   GET /api/players/search?name=LeBron
   ```

2. **Stats Service → NBA API:** Fetches matching players
   ```
   Response: [{ id: 265, fullName: "LeBron James" }]
   ```

3. **Frontend:** User clicks "Add to Watchlist"
   ```
   POST /api/watchlists?userId=1
   Body: { "playerId": 265 }
   ```

4. **Stats Service:** Saves relationship to database
   ```
   INSERT INTO watchlist (user_id, player_id) VALUES (1, 265);
   ```

5. **Frontend:** Display watchlist
   ```
   GET /api/watchlists?userId=1  → Returns [{ playerId: 265 }]
   GET /api/players/batch?playerIds=265 → Returns [{ id: 265, fullName: "LeBron James" }]
   ```

## Layer Responsibilities

### Controller Layer
- HTTP request/response mapping
- HTTP status codes
- Exception handling
- Parameter validation

### Service Layer
- Business logic
- Data validation
- API integration
- DTO mapping
- Transaction management

### Repository Layer
- Database operations
- JPA queries

## Dependencies

- **Spring Boot 3.4.1**
- **Spring Cloud 2024.0.0**
- **Spring Data JPA** - Database operations
- **PostgreSQL** - Data persistence
- **Lombok** - Reduce boilerplate
- **Eureka Client** - Service discovery
- **RestTemplate** - HTTP client for NBA API

## Future Enhancements

- [ ] Add Redis caching for frequently searched players
- [ ] Implement player statistics endpoints
- [ ] Add team information
- [ ] Game schedules and live scores
- [ ] JWT token extraction for userId
- [ ] Rate limiting for API calls
- [ ] WebClient for async API calls

## Documentation

- **[NBA_API_INTEGRATION.md](NBA_API_INTEGRATION.md)** - Detailed NBA API setup and architecture
- **[USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)** - API usage examples and frontend integration

## Integration with Other Services

- **Security Service (port 8082)**: Provides JWT authentication (to be integrated)
- **Eureka Server (port 8761)**: Service discovery
- **API Gateway (planned)**: Single entry point for all services

## Database

- **Name:** `bball-stats`
- **Tables:** `watchlist`
- **ORM:** Hibernate (via Spring Data JPA)
- **DDL:** Auto-generated from entities

## Logging

Service uses SLF4J with Logback for logging:
- Player searches
- API calls to NBA
- Error conditions

Check logs for debugging and monitoring.

## Notes

- **userId is temporary:** Currently passed as query parameter, will be extracted from JWT token
- **Minimal data:** Player responses only include ID and full name - keeping it simple
- **Player data is ephemeral:** Not stored in database, always fetched fresh from NBA API
- **Watchlist is persistent:** Only stores user-player relationships in PostgreSQL
- **API rate limits:** Check your RapidAPI subscription limits

