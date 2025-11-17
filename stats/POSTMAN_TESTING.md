# Postman Testing Guide - Stats Service

## Step 1: Start the Stats Service

### Option A: Using Maven (Terminal)
```bash
cd /Users/ashwi/Desktop/Personal\ Projects/basketball-microservices-project/stats
mvn spring-boot:run
```

### Option B: Using IntelliJ
1. Open IntelliJ
2. Navigate to `stats/src/main/java/com/bball/stats/StatsApplication.java`
3. Right-click on the file
4. Click **"Run 'StatsApplication'"**

### ✅ Service is Running When You See:
```
Started StatsApplication in X.XXX seconds
Tomcat started on port(s): 8081 (http)
```

---

## Step 2: Set Up Postman Collection

### Create a New Collection
1. Open Postman
2. Click **"New"** → **"Collection"**
3. Name it: **"Basketball Stats Service"**
4. Save

### Set Base URL Variable
1. Click on your collection
2. Go to **"Variables"** tab
3. Add variable:
   - Variable: `base_url`
   - Initial Value: `http://localhost:8081`
   - Current Value: `http://localhost:8081`
4. Save

---

## Step 3: Test Player Search Endpoints

### Test 1: Search for LeBron James

**Request Setup:**
- **Method:** `GET`
- **URL:** `{{base_url}}/api/players/search?name=LeBron`
- **Headers:** None needed
- **Body:** None

**Steps in Postman:**
1. Click **"New"** → **"Request"**
2. Name: **"Search Player by Name"**
3. Save to your collection
4. Set method to **GET**
5. Enter URL: `{{base_url}}/api/players/search?name=LeBron`
6. Click **"Send"**

**Expected Response (200 OK):**
```json
[
  {
    "id": 265,
    "fullName": "LeBron James"
  }
]
```

**Try Other Searches:**
- `?name=Curry` - Returns multiple Curry players
- `?name=Durant` - Returns Kevin Durant
- `?name=Giannis` - Returns Giannis Antetokounmpo

---

### Test 2: Get Player by ID

**Request Setup:**
- **Method:** `GET`
- **URL:** `{{base_url}}/api/players/265`
- **Headers:** None needed
- **Body:** None

**Steps in Postman:**
1. Create new request: **"Get Player by ID"**
2. Method: **GET**
3. URL: `{{base_url}}/api/players/265`
4. Click **"Send"**

**Expected Response (200 OK):**
```json
{
  "id": 265,
  "fullName": "LeBron James"
}
```

**Try Other Player IDs:**
- `124` - Stephen Curry
- `237` - Kevin Durant
- `155` - Giannis Antetokounmpo

---

## Step 4: Test Watchlist Endpoints

### Test 3: Add Player to Watchlist

**Request Setup:**
- **Method:** `POST`
- **URL:** `{{base_url}}/api/watchlists?userId=1`
- **Headers:** 
  - `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "playerId": 265
}
```

**Steps in Postman:**
1. Create new request: **"Add Player to Watchlist"**
2. Method: **POST**
3. URL: `{{base_url}}/api/watchlists?userId=1`
4. Go to **"Headers"** tab
   - Key: `Content-Type`
   - Value: `application/json`
5. Go to **"Body"** tab
   - Select **"raw"**
   - Select **"JSON"** from dropdown
   - Enter:
     ```json
     {
       "playerId": 265
     }
     ```
6. Click **"Send"**

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "playerId": 265,
  "userId": 1,
  "message": "Player added to watchlist successfully"
}
```

---

### Test 4: Get User's Watchlist

**Request Setup:**
- **Method:** `GET`
- **URL:** `{{base_url}}/api/watchlists?userId=1`
- **Headers:** None needed
- **Body:** None

**Steps in Postman:**
1. Create new request: **"Get User Watchlist"**
2. Method: **GET**
3. URL: `{{base_url}}/api/watchlists?userId=1`
4. Click **"Send"**

**Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "playerId": 265,
    "userId": 1
  }
]
```

---

### Test 5: Check if Player is in Watchlist

**Request Setup:**
- **Method:** `GET`
- **URL:** `{{base_url}}/api/watchlists/check/265?userId=1`
- **Headers:** None needed
- **Body:** None

**Steps in Postman:**
1. Create new request: **"Check Player in Watchlist"**
2. Method: **GET**
3. URL: `{{base_url}}/api/watchlists/check/265?userId=1`
4. Click **"Send"**

**Expected Response (200 OK):**
```json
{
  "playerId": 265,
  "userId": 1,
  "message": "Player is in watchlist"
}
```

---

### Test 6: Try Adding Duplicate (Error Case)

**Request Setup:**
- Same as Test 3 (Add Player to Watchlist)
- Try adding the same player again

**Expected Response (400 Bad Request):**
```json
{
  "message": "Player already in watchlist"
}
```

---

### Test 7: Remove Player from Watchlist

**Request Setup:**
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/watchlists/265?userId=1`
- **Headers:** None needed
- **Body:** None

**Steps in Postman:**
1. Create new request: **"Remove Player from Watchlist"**
2. Method: **DELETE**
3. URL: `{{base_url}}/api/watchlists/265?userId=1`
4. Click **"Send"**

**Expected Response (200 OK):**
```json
{
  "playerId": 265,
  "userId": 1,
  "message": "Player removed from watchlist successfully"
}
```

---

### Test 8: Verify Removal

**Request Setup:**
- Same as Test 4 (Get User's Watchlist)

**Expected Response (200 OK):**
```json
[]
```

---

## Step 5: Complete End-to-End Test Flow

### Scenario: User searches and adds multiple players to watchlist

**1. Search for Stephen Curry**
```
GET {{base_url}}/api/players/search?name=Curry
```
Result: Note ID `124` for Stephen Curry

**2. Search for Kevin Durant**
```
GET {{base_url}}/api/players/search?name=Durant
```
Result: Note ID `237` for Kevin Durant

**3. Add Stephen Curry to watchlist**
```
POST {{base_url}}/api/watchlists?userId=1
Body: { "playerId": 124 }
```

**4. Add Kevin Durant to watchlist**
```
POST {{base_url}}/api/watchlists?userId=1
Body: { "playerId": 237 }
```

**5. View complete watchlist**
```
GET {{base_url}}/api/watchlists?userId=1
```
Expected: Array with both players

**6. Get player details for display**
```
GET {{base_url}}/api/players/124  (Stephen Curry)
GET {{base_url}}/api/players/237  (Kevin Durant)
```

**7. Remove one player**
```
DELETE {{base_url}}/api/watchlists/124?userId=1
```

**8. Verify updated watchlist**
```
GET {{base_url}}/api/watchlists?userId=1
```
Expected: Only Durant remains

---

## Step 6: Import Ready-to-Use Collection

### Option 1: Manual Import
Save this as `basketball-stats.postman_collection.json`:

```json
{
  "info": {
    "name": "Basketball Stats Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Player Search",
      "item": [
        {
          "name": "Search Player by Name",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/players/search?name=LeBron",
              "host": ["{{base_url}}"],
              "path": ["api", "players", "search"],
              "query": [{"key": "name", "value": "LeBron"}]
            }
          }
        },
        {
          "name": "Get Player by ID",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/players/265",
              "host": ["{{base_url}}"],
              "path": ["api", "players", "265"]
            }
          }
        }
      ]
    },
    {
      "name": "Watchlist",
      "item": [
        {
          "name": "Add Player to Watchlist",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"playerId\": 265\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/watchlists?userId=1",
              "host": ["{{base_url}}"],
              "path": ["api", "watchlists"],
              "query": [{"key": "userId", "value": "1"}]
            }
          }
        },
        {
          "name": "Get User Watchlist",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/watchlists?userId=1",
              "host": ["{{base_url}}"],
              "path": ["api", "watchlists"],
              "query": [{"key": "userId", "value": "1"}]
            }
          }
        },
        {
          "name": "Check Player in Watchlist",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/watchlists/check/265?userId=1",
              "host": ["{{base_url}}"],
              "path": ["api", "watchlists", "check", "265"],
              "query": [{"key": "userId", "value": "1"}]
            }
          }
        },
        {
          "name": "Remove Player from Watchlist",
          "request": {
            "method": "DELETE",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/watchlists/265?userId=1",
              "host": ["{{base_url}}"],
              "path": ["api", "watchlists", "265"],
              "query": [{"key": "userId", "value": "1"}]
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8081"
    }
  ]
}
```

**Import in Postman:**
1. Click **"Import"** button
2. Select the JSON file
3. Collection will be added with all requests ready to use

---

## Troubleshooting

### Issue: Connection Refused
**Error:** `Could not get any response`

**Solution:**
- Check if service is running: Look for "Started StatsApplication" in logs
- Verify port: Service should be on `8081`
- Check firewall settings

---

### Issue: 404 Not Found
**Error:** `404 Not Found`

**Solution:**
- Double-check URL path: `/api/players/search` or `/api/watchlists`
- Ensure service started successfully without errors

---

### Issue: 400 Bad Request on POST
**Error:** `400 Bad Request` when adding to watchlist

**Solutions:**
- Check `Content-Type` header is set to `application/json`
- Verify JSON body is valid: `{"playerId": 265}`
- Check playerId is a number, not a string

---

### Issue: Empty Response from Player Search
**Error:** Returns `[]` for valid player names

**Solutions:**
- Check API key is configured correctly in `application.yml`
- Verify you have API quota remaining (check RapidAPI dashboard)
- Check service logs for error messages

---

## Quick Reference

### Base URL
```
http://localhost:8081
```

### All Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/players/search?name={name}` | Search players by name |
| GET | `/api/players/{id}` | Get player by ID |
| GET | `/api/watchlists?userId={id}` | Get user's watchlist |
| POST | `/api/watchlists?userId={id}` | Add player to watchlist |
| DELETE | `/api/watchlists/{playerId}?userId={id}` | Remove player from watchlist |
| GET | `/api/watchlists/check/{playerId}?userId={id}` | Check if player in watchlist |

### Sample Player IDs
- `265` - LeBron James
- `124` - Stephen Curry
- `237` - Kevin Durant
- `155` - Giannis Antetokounmpo

---

## Testing Checklist

- [ ] Service starts successfully
- [ ] Search for player by name works
- [ ] Get player by ID works
- [ ] Add player to watchlist works
- [ ] Get watchlist works
- [ ] Check player in watchlist works
- [ ] Duplicate prevention works (400 error)
- [ ] Remove player from watchlist works
- [ ] Verify removal works (empty list)
- [ ] Complete end-to-end flow works

---

**Ready to test!** Start with Test 1 (Search for LeBron) and work your way through. 🏀


