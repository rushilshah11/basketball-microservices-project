# Stats Service - Testing Guide

## Prerequisites Checklist

### ✅ Already Done
- [x] Database created (`bball-stats`)
- [x] Code structure complete
- [x] Maven dependencies configured
- [x] Watchlist entity and repository set up
- [x] Player service with NBA API integration

### ⚠️ Still Need to Configure

#### 1. **Get RapidAPI Key**
   - Go to [https://rapidapi.com/api-sports/api/api-nba](https://rapidapi.com/api-sports/api/api-nba)
   - Sign up / Log in
   - Subscribe to API-NBA (Free tier available: 100 requests/day)
   - Copy your API key from the dashboard

#### 2. **Update application.yml**
   Edit: `stats/src/main/resources/application.yml`
   
   ```yaml
   nba:
     api:
       api-key: YOUR_ACTUAL_RAPIDAPI_KEY  # Replace this!
   ```

#### 3. **Configure Java 17 in IntelliJ**
   (Same as you did for security service)
   - File → Project Structure → Project → SDK: Java 17
   - File → Project Structure → Modules → stats → Language level: 17
   - Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE: Java 17

## Testing Strategy

### Phase 1: Test Without NBA API (Watchlist Only)

You can test the watchlist endpoints WITHOUT an NBA API key by manually providing player IDs.

### Phase 2: Test With NBA API (Full Integration)

Once you have the API key, you can test player search → watchlist flow.

---

## Phase 1: Watchlist Testing (No API Key Required)

### Step 1: Start the Service

**Option A: From Terminal**
```bash
cd /Users/ashwi/Desktop/Personal\ Projects/basketball-microservices-project/stats
mvn spring-boot:run
```

**Option B: From IntelliJ**
- Right-click `StatsApplication.java`
- Click "Run 'StatsApplication'"

**Expected Output:**
```
Started StatsApplication in X seconds
Tomcat started on port(s): 8081
```

### Step 2: Test Watchlist Endpoints

#### 2.1 Add a Player to Watchlist (Using Fake Player ID)
```bash
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 999}'
```

**Expected Response:**
```json
{
  "id": 1,
  "playerId": 999,
  "userId": 1,
  "message": "Player added to watchlist successfully"
}
```

#### 2.2 Get User's Watchlist
```bash
curl -X GET "http://localhost:8081/api/watchlists?userId=1"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "playerId": 999,
    "userId": 1
  }
]
```

#### 2.3 Check if Player is in Watchlist
```bash
curl -X GET "http://localhost:8081/api/watchlists/check/999?userId=1"
```

**Expected Response:**
```json
{
  "playerId": 999,
  "userId": 1,
  "message": "Player is in watchlist"
}
```

#### 2.4 Try Adding Duplicate
```bash
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 999}'
```

**Expected Response (400 Bad Request):**
```json
{
  "message": "Player already in watchlist"
}
```

#### 2.5 Remove Player from Watchlist
```bash
curl -X DELETE "http://localhost:8081/api/watchlists/999?userId=1"
```

**Expected Response:**
```json
{
  "playerId": 999,
  "userId": 1,
  "message": "Player removed from watchlist successfully"
}
```

#### 2.6 Verify Removal
```bash
curl -X GET "http://localhost:8081/api/watchlists?userId=1"
```

**Expected Response:**
```json
[]
```

### ✅ Phase 1 Results

If all these tests pass, your **watchlist service is fully functional**! The database, JPA, repository, service, and controller layers are all working correctly.

---

## Phase 2: Full Integration Testing (Requires API Key)

### Step 1: Configure API Key

Update `stats/src/main/resources/application.yml`:
```yaml
nba:
  api:
    api-key: your-actual-key-here  # Replace with your RapidAPI key
```

### Step 2: Restart the Service

Stop and restart the stats service to pick up the new configuration.

### Step 3: Test Player Search

#### 3.1 Search for LeBron
```bash
curl -X GET "http://localhost:8081/api/players/search?name=LeBron"
```

**Expected Response:**
```json
[
  {
    "id": 265,
    "fullName": "LeBron James"
  }
]
```

#### 3.2 Search for Curry
```bash
curl -X GET "http://localhost:8081/api/players/search?name=Curry"
```

**Expected Response:**
```json
[
  {
    "id": 115,
    "fullName": "Seth Curry"
  },
  {
    "id": 124,
    "fullName": "Stephen Curry"
  },
  {
    "id": 2565,
    "fullName": "Eddy Curry"
  }
]
```

#### 3.3 Get Player by ID
```bash
curl -X GET "http://localhost:8081/api/players/265"
```

**Expected Response:**
```json
{
  "id": 265,
  "fullName": "LeBron James"
}
```

### Step 4: Complete Workflow Test

#### 4.1 Search for Player
```bash
curl -X GET "http://localhost:8081/api/players/search?name=Durant"
```

**Response:** Note the player ID (e.g., 237)

#### 4.2 Add to Watchlist
```bash
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 237}'
```

#### 4.3 Add Another Player
```bash
# Search for another player
curl -X GET "http://localhost:8081/api/players/search?name=Giannis"

# Add to watchlist (assuming ID is 155)
curl -X POST "http://localhost:8081/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 155}'
```

#### 4.4 View Watchlist
```bash
curl -X GET "http://localhost:8081/api/watchlists?userId=1"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "playerId": 237,
    "userId": 1
  },
  {
    "id": 2,
    "playerId": 155,
    "userId": 1
  }
]
```

#### 4.5 Get Player Names for Watchlist
```bash
# Get name for each player
curl -X GET "http://localhost:8081/api/players/237"
curl -X GET "http://localhost:8081/api/players/155"
```

### ✅ Phase 2 Results

If all these tests pass, your **complete integration is working**:
- ✅ NBA API calls
- ✅ Player search
- ✅ Watchlist management
- ✅ Full end-to-end workflow

---

## Common Issues & Solutions

### Issue 1: Service Won't Start

**Error:** `Fatal error compiling: java.lang.ExceptionInInitializerError`

**Solution:** Configure Java 17 in IntelliJ Maven settings (see Prerequisites #3)

---

### Issue 2: Database Connection Error

**Error:** `Connection refused` or `database does not exist`

**Solution:** 
```bash
# Check if database exists
psql -l | grep bball-stats

# If not, create it
createdb bball-stats
```

---

### Issue 3: 401 Unauthorized from NBA API

**Error:** API returns 401 or "Invalid API key"

**Solutions:**
1. Check that you've updated `application.yml` with your actual key
2. Restart the service after updating configuration
3. Verify your RapidAPI subscription is active
4. Check you haven't exceeded rate limits

---

### Issue 4: Eureka Connection Errors

**Error:** `Connection refused` to `localhost:8761`

**Solution:** This is just a warning if Eureka isn't running. The service will still work fine for direct API testing. If you want to run with Eureka:
```bash
# Start Eureka first
cd eureka-server
mvn spring-boot:run

# Then start stats service
cd ../stats
mvn spring-boot:run
```

---

## Testing Tools

### Using cURL (Command Line)
```bash
curl -X GET "http://localhost:8081/api/players/search?name=LeBron"
```

### Using Postman
1. Create new request
2. Method: GET
3. URL: `http://localhost:8081/api/players/search`
4. Params: `name` = `LeBron`
5. Send

### Using IntelliJ HTTP Client
Create a file `test.http` in your project:
```http
### Search for player
GET http://localhost:8081/api/players/search?name=LeBron

### Add to watchlist
POST http://localhost:8081/api/watchlists?userId=1
Content-Type: application/json

{
  "playerId": 265
}

### Get watchlist
GET http://localhost:8081/api/watchlists?userId=1
```

---

## Quick Test Script

Save this as `test-watchlist.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8081"

echo "🏀 Testing Watchlist Service"
echo "=============================="

echo -e "\n1. Adding player to watchlist..."
curl -X POST "$BASE_URL/api/watchlists?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 265}' \
  -w "\nStatus: %{http_code}\n"

echo -e "\n2. Getting watchlist..."
curl -X GET "$BASE_URL/api/watchlists?userId=1" \
  -w "\nStatus: %{http_code}\n"

echo -e "\n3. Checking if player is in watchlist..."
curl -X GET "$BASE_URL/api/watchlists/check/265?userId=1" \
  -w "\nStatus: %{http_code}\n"

echo -e "\n4. Removing player from watchlist..."
curl -X DELETE "$BASE_URL/api/watchlists/265?userId=1" \
  -w "\nStatus: %{http_code}\n"

echo -e "\n5. Verifying removal..."
curl -X GET "$BASE_URL/api/watchlists?userId=1" \
  -w "\nStatus: %{http_code}\n"

echo -e "\n✅ Testing complete!"
```

Run it:
```bash
chmod +x test-watchlist.sh
./test-watchlist.sh
```

---

## Summary

### What You Can Test RIGHT NOW (No API Key):
✅ Watchlist CRUD operations  
✅ Database persistence  
✅ Error handling (duplicates, not found)  
✅ All service/controller logic  

### What Requires API Key:
⚠️ Player search by name  
⚠️ Get player details by ID  

### Next Steps:
1. Start the service
2. Run Phase 1 tests (watchlist only)
3. Get RapidAPI key
4. Run Phase 2 tests (full integration)

You're basically **ready to test everything except the NBA API integration**! The watchlist service is complete and testable right now. 🚀


