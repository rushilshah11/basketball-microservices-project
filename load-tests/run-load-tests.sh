#!/bin/bash

# Test Authentication and K6 Load Testing Script
# This script registers a test user, obtains a JWT token, and runs k6 tests

echo "🏀 Basketball Microservices - Load Testing Script"
echo "=================================================="

# Step 0: Clean up previous test data
echo -e "\n🧹 Step 0: Cleaning up database volumes from previous runs..."
docker compose -f docker-compose.dev.yml down -v
echo "✅ Volumes cleared"

# Step 0.5: Bring services back up
echo -e "\n🚀 Starting services..."
docker compose -f docker-compose.dev.yml up -d
echo "⏳ Waiting for services to be healthy..."
sleep 60  # Give services time to start and initialize databases

# Step 1: Register a test user and get JWT token
echo -e "\n📝 Step 1: Registering test user..."

TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Test",
    "lastname": "User",
    "email": "test-'"$(date +%s)"'@example.com",
    "password": "TestPassword123!"
  }')

TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to obtain token. Response:"
  echo $TOKEN_RESPONSE
  exit 1
fi

echo "✅ Token obtained: ${TOKEN:0:20}..."

# Step 2: Run ASYNC test
echo -e "\n🚀 Step 2: Running ASYNC test (50 VUs, 30s)..."
echo "Testing: POST /api/watchlists (async - returns immediately)"
echo "Expected: p95 < 200ms"

k6 run k6-async-test.js --vus 50 --duration 30s -e TOKEN="$TOKEN" \
  --summary-export=async-results.json 2>&1 | tee async-test.log

# Wait a bit between tests
sleep 5

# Step 3: Run SYNC test
echo -e "\n🔄 Step 3: Running SYNC test (50 VUs, 30s)..."
echo "Testing: POST /api/watchlists (sync - waits for prediction)"
echo "Expected: p95 < 700ms"

k6 run k6-sync-test.js --vus 50 --duration 30s -e TOKEN="$TOKEN" \
  --summary-export=sync-results.json 2>&1 | tee sync-test.log

# Step 4: Compare results
echo -e "\n📊 Step 4: Comparing results..."
echo "=================================================="
echo "ASYNC TEST RESULTS:"
echo "=================================================="
tail -30 async-test.log | grep -E "p\(95\)|p\(50\)|avg|failed|http_reqs"

echo -e "\n=================================================="
echo "SYNC TEST RESULTS:"
echo "=================================================="
tail -30 sync-test.log | grep -E "p\(95\)|p\(50\)|avg|failed|http_reqs"

echo -e "\n✅ Tests completed!"
echo "Full logs saved to: async-test.log and sync-test.log"
echo "JSON results saved to: async-results.json and sync-results.json"
