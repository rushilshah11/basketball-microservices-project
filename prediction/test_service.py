"""
Simple test script for the prediction service
"""
import requests
import json

BASE_URL = "http://localhost:5002"

def test_health():
    """Test health endpoint"""
    print("Testing /health...")
    response = requests.get(f"{BASE_URL}/health")
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    print()

def test_model_info():
    """Test model info endpoint"""
    print("Testing /model/info...")
    response = requests.get(f"{BASE_URL}/model/info")
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    print()

def test_single_prediction():
    """Test single player prediction"""
    print("Testing /predict...")
    
    payload = {
        "playerName": "LeBron James",
        "currentStats": {
            "ppg": 25.2,
            "apg": 7.8,
            "rpg": 8.1,
            "fgPct": 0.52,
            "ftPct": 0.75,
            "gamesPlayed": 45,
            "minutesPerGame": 35.5,
            "stealsPerGame": 1.3,
            "blocksPerGame": 0.7,
            "turnoversPerGame": 2.8
        },
        "opponent": "Lakers",
        "homeGame": True
    }
    
    response = requests.post(f"{BASE_URL}/predict", json=payload)
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    print()

def test_batch_prediction():
    """Test batch prediction"""
    print("Testing /predict/batch...")
    
    payload = {
        "predictions": [
            {
                "playerName": "LeBron James",
                "currentStats": {
                    "ppg": 25.2,
                    "apg": 7.8,
                    "rpg": 8.1,
                    "fgPct": 0.52,
                    "ftPct": 0.75,
                    "gamesPlayed": 45
                },
                "homeGame": True
            },
            {
                "playerName": "Stephen Curry",
                "currentStats": {
                    "ppg": 28.5,
                    "apg": 5.2,
                    "rpg": 4.8,
                    "fgPct": 0.47,
                    "ftPct": 0.91,
                    "gamesPlayed": 48
                },
                "homeGame": False
            }
        ]
    }
    
    response = requests.post(f"{BASE_URL}/predict/batch", json=payload)
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    print()

if __name__ == "__main__":
    print("=" * 60)
    print("Prediction Service Test Suite")
    print("=" * 60)
    print()
    
    try:
        test_health()
        test_model_info()
        test_single_prediction()
        test_batch_prediction()
        
        print("=" * 60)
        print("✅ All tests completed!")
        print("=" * 60)
        
    except requests.exceptions.ConnectionError:
        print("❌ Could not connect to service. Is it running on port 5002?")
    except Exception as e:
        print(f"❌ Test failed: {e}")

