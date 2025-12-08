# Prediction Service

PyTorch-based neural network service for predicting NBA player performance.

## Overview

This microservice uses a feedforward neural network to predict player performance (points, assists, rebounds) for upcoming games. Built with PyTorch for easy future expansion to Graph Neural Networks (GNN).

## Architecture

```
┌─────────────────────────────────────────────┐
│         Prediction Service (PyTorch)         │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │  PlayerPerformanceNet                  │ │
│  │  - Input: 10 features                  │ │
│  │  - Hidden: 64 → 32 neurons             │ │
│  │  - Output: 3 predictions (pts/ast/reb) │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  FastAPI REST Endpoints:                    │
│  - POST /predict                            │
│  - POST /predict/batch                      │
│  - GET /model/info                          │
└─────────────────────────────────────────────┘
```

## Features

✅ Simple feedforward neural network  
✅ PyTorch-based (ready for GNN expansion)  
✅ Batch prediction support  
✅ Home/away game adjustment  
✅ Confidence scoring  
✅ Easy to extend and retrain  

## API Endpoints

### 1. Single Player Prediction

```bash
POST http://localhost:5002/predict
Content-Type: application/json

{
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
  "homeGame": true
}
```

**Response:**
```json
{
  "playerName": "LeBron James",
  "predictedPoints": 26.5,
  "predictedAssists": 8.2,
  "predictedRebounds": 8.7,
  "confidence": 0.87,
  "model": "basic_nn"
}
```

### 2. Batch Prediction

```bash
POST http://localhost:5002/predict/batch
Content-Type: application/json

{
  "predictions": [
    {
      "playerName": "LeBron James",
      "currentStats": { ... },
      "homeGame": true
    },
    {
      "playerName": "Stephen Curry",
      "currentStats": { ... },
      "homeGame": false
    }
  ]
}
```

### 3. Model Information

```bash
GET http://localhost:5002/model/info
```

**Response:**
```json
{
  "modelType": "FeedForward Neural Network",
  "framework": "PyTorch",
  "inputFeatures": 10,
  "outputFeatures": 3,
  "hiddenLayers": 2,
  "parameters": 2883,
  "futureEnhancements": [
    "GNN",
    "Attention Mechanism",
    "Player Embeddings"
  ]
}
```

### 4. Health Check

```bash
GET http://localhost:5002/health
```

## Input Features

The model uses 10 normalized features:

| Feature | Description | Normalization |
|---------|-------------|---------------|
| `ppg` | Points per game | / 30.0 |
| `apg` | Assists per game | / 10.0 |
| `rpg` | Rebounds per game | / 12.0 |
| `fgPct` | Field goal % | As-is (0-1) |
| `ftPct` | Free throw % | As-is (0-1) |
| `gamesPlayed` | Games played | / 82.0 |
| `minutesPerGame` | Minutes per game | / 48.0 |
| `stealsPerGame` | Steals per game | / 3.0 |
| `blocksPerGame` | Blocks per game | / 3.0 |
| `turnoversPerGame` | Turnovers per game | / 5.0 |

## Output

Predictions for next game:
- **Predicted Points** (denormalized)
- **Predicted Assists** (denormalized)
- **Predicted Rebounds** (denormalized)
- **Confidence Score** (0-1)

## Running Locally

```bash
cd prediction

# Install dependencies
pip install -r requirements.txt

# Run service
python app.py

# Or with uvicorn
uvicorn app:app --reload --port 5002
```

## Running with Docker

```bash
# Build image
docker build -t prediction .

# Run container
docker run -p 5002:5002 prediction
```

## Integration with Other Services

### From Stats Service

```java
// Call prediction service from Java
RestTemplate restTemplate = new RestTemplate();
String url = "http://prediction:5002/predict";

PredictionRequest request = new PredictionRequest();
request.setPlayerName("LeBron James");
// ... set stats ...

PredictionResponse prediction = restTemplate.postForObject(
    url, 
    request, 
    PredictionResponse.class
);
```

### From Frontend

```javascript
const response = await fetch('http://localhost:5002/predict', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    playerName: 'LeBron James',
    currentStats: { ppg: 25.2, apg: 7.8, rpg: 8.1, ... },
    homeGame: true
  })
});

const prediction = await response.json();
console.log(`Predicted: ${prediction.predictedPoints} pts`);
```

## Future Enhancements

### Short-term
- [ ] Train model on real historical data
- [ ] Add opponent strength factor
- [ ] Injury status integration
- [ ] Recent form weighting

### Long-term (GNN)
- [ ] Graph Neural Network architecture
- [ ] Player relationship modeling
- [ ] Team dynamics representation
- [ ] Multi-player interaction effects
- [ ] Transfer learning from other sports

## Model Architecture

```python
PlayerPerformanceNet(
  (fc1): Linear(10 → 64)
  (relu1): ReLU()
  (dropout1): Dropout(p=0.3)
  (fc2): Linear(64 → 32)
  (relu2): ReLU()
  (dropout2): Dropout(p=0.3)
  (fc3): Linear(32 → 3)
)
```

**Total Parameters:** ~2,883

## GNN Future Architecture (Planned)

```
Player Nodes ─┐
              ├─→ Graph Convolution ─→ Attention ─→ Prediction
Team Nodes ───┘

Features:
- Player embeddings
- Team chemistry graph
- Opponent matchup edges
- Historical performance links
```

## Dependencies

- **PyTorch** - Neural network framework
- **FastAPI** - REST API framework
- **NumPy** - Numerical computations
- **scikit-learn** - Utilities
- **Pandas** - Data handling (future use)

## Testing

```bash
# Test health endpoint
curl http://localhost:5002/health

# Test prediction
curl -X POST http://localhost:5002/predict \
  -H "Content-Type: application/json" \
  -d '{
    "playerName": "Test Player",
    "currentStats": {
      "ppg": 20.0,
      "apg": 5.0,
      "rpg": 6.0,
      "fgPct": 0.45,
      "ftPct": 0.80,
      "gamesPlayed": 30
    },
    "homeGame": true
  }'
```

## Notes

- **Current Model:** Random initialization (placeholder)
- **Production:** Train on historical NBA data
- **Confidence:** Based on games played (more games = higher confidence)
- **Home Advantage:** 5% boost for home games
- **Extensible:** Easy to add more features or change architecture

