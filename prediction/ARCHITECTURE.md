# Prediction Service Architecture

## Overview

The Prediction Service is a standalone Python microservice using PyTorch for neural network-based player performance predictions. It's designed to be easily extensible to Graph Neural Networks (GNN) in the future.

## Service Position in Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Microservices Ecosystem                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Security Service│  │   Stats Service  │  │  NBA Fetcher     │
│  (Auth/Users)    │  │  (Watchlist)     │  │  (Data Source)   │
│  Port: 8082      │  │  Port: 8081      │  │  Port: 5001      │
│  Java + gRPC     │  │  Java + REST     │  │  Python/FastAPI  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
         │                     │                      │
         └─────────────────────┼──────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │  Prediction Service │ ← NEW!
                    │  (ML Predictions)   │
                    │  Port: 5002         │
                    │  Python + PyTorch   │
                    └─────────────────────┘
```

## Technology Stack

| Component | Technology | Reason |
|-----------|-----------|---------|
| **Framework** | FastAPI | Fast, async, auto-docs |
| **ML Library** | PyTorch | Flexible, GNN-ready |
| **API Style** | REST/JSON | Easy integration |
| **Containerization** | Docker | Consistent deployment |
| **Language** | Python 3.11 | ML ecosystem |

## Neural Network Architecture

### Current: Feedforward Network

```
Input Layer (10 features)
    ↓
Dense (64 neurons) + ReLU + Dropout(0.3)
    ↓
Dense (32 neurons) + ReLU + Dropout(0.3)
    ↓
Output Layer (3 predictions)
```

**Features:**
- Simple and fast
- Easy to understand and debug
- Good baseline for future improvements

### Future: Graph Neural Network (GNN)

```
Player Graph:
  Nodes: Players, Teams, Opponents
  Edges: Teammates, Opponents, Matchup History

Architecture:
  Graph Convolution Layers
    ↓
  Attention Mechanism
    ↓
  Aggregation
    ↓
  Prediction Layer
```

**Benefits:**
- Model player interactions
- Team chemistry effects
- Opponent matchups
- Historical patterns

## Data Flow

### Prediction Request Flow

```
1. Client Request
   ↓
2. FastAPI Endpoint (/predict)
   ↓
3. Normalize Input Features
   ↓
4. PyTorch Model Forward Pass
   ↓
5. Denormalize Predictions
   ↓
6. Add Confidence Score
   ↓
7. Return JSON Response
```

### Integration with Stats Service

```
Stats Service (Java)
    ↓
    │ GET player current stats
    ↓
NBA Fetcher (Python)
    ↓
    │ Returns stats JSON
    ↓
Stats Service
    ↓
    │ POST to /predict
    ↓
Prediction Service (PyTorch)
    ↓
    │ Returns predictions
    ↓
Stats Service
    ↓
    │ Store/Display to user
```

## API Design

### RESTful Endpoints

```
GET  /health           - Health check
GET  /model/info       - Model metadata
GET  /                 - Service info
POST /predict          - Single prediction
POST /predict/batch    - Multiple predictions
```

### Request/Response Format

**Request:**
```json
{
  "playerName": "LeBron James",
  "currentStats": {
    "ppg": 25.2,
    "apg": 7.8,
    "rpg": 8.1,
    ...
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

## Model Features

### Input Features (10)

1. **Points per game** (ppg)
2. **Assists per game** (apg)
3. **Rebounds per game** (rpg)
4. **Field goal percentage** (fgPct)
5. **Free throw percentage** (ftPct)
6. **Games played** (gamesPlayed)
7. **Minutes per game** (minutesPerGame)
8. **Steals per game** (stealsPerGame)
9. **Blocks per game** (blocksPerGame)
10. **Turnovers per game** (turnoversPerGame)

### Output Predictions (3)

1. **Predicted Points** - Next game points
2. **Predicted Assists** - Next game assists
3. **Predicted Rebounds** - Next game rebounds

### Additional Factors

- **Home/Away Adjustment** - 5% boost for home games
- **Confidence Score** - Based on sample size (games played)

## Deployment

### Docker Configuration

```yaml
prediction:
  build: ./prediction
  container_name: bball_prediction
  ports:
    - "5002:5002"
  environment:
    PYTHONUNBUFFERED: 1
```

### Port Allocation

| Service | Port |
|---------|------|
| Eureka | 8761 |
| Security | 8082 |
| Stats | 8081 |
| NBA Fetcher | 5001 |
| **Prediction** | **5002** ← New! |

## Scalability Considerations

### Current Implementation
- **Stateless** - No session storage
- **Containerized** - Easy horizontal scaling
- **Lightweight** - Small model, fast inference

### Future Scaling
- Load balancer for multiple instances
- Model serving with TorchServe
- GPU acceleration for complex models
- Batch processing for efficiency

## Security

### Current
- No authentication (internal service)
- Trusts calling services

### Future
- JWT validation
- Rate limiting
- Input validation (already implemented via Pydantic)
- API key authentication

## Monitoring & Observability

### Metrics to Track
- Request count
- Response time
- Prediction accuracy (with ground truth)
- Model confidence distribution
- Error rate

### Logging
- Request/response logging
- Model version tracking
- Performance metrics

## Testing Strategy

### Unit Tests
- Model forward pass
- Feature normalization
- Prediction logic

### Integration Tests
- API endpoint responses
- Docker container health
- Service-to-service communication

### Model Tests
- Prediction ranges (sanity checks)
- Confidence scoring
- Edge cases

## Future Enhancements

### Phase 1: Improved Model
- [ ] Train on historical data
- [ ] Feature engineering
- [ ] Hyperparameter tuning
- [ ] Cross-validation

### Phase 2: Advanced Features
- [ ] Opponent strength modeling
- [ ] Injury status integration
- [ ] Rest days factor
- [ ] Recent form weighting

### Phase 3: GNN Implementation
- [ ] Player relationship graph
- [ ] Graph convolution layers
- [ ] Attention mechanisms
- [ ] Multi-task learning

### Phase 4: Production Ready
- [ ] Model versioning
- [ ] A/B testing
- [ ] Automated retraining
- [ ] Performance monitoring

## Integration Examples

### From Stats Service (Java)

```java
@Service
public class PredictionClient {
    private final RestTemplate restTemplate;
    
    public PredictionResponse getPrediction(PlayerStats stats) {
        String url = "http://prediction:5002/predict";
        
        PredictionRequest request = new PredictionRequest();
        request.setPlayerName(stats.getPlayerName());
        request.setCurrentStats(stats);
        
        return restTemplate.postForObject(url, request, PredictionResponse.class);
    }
}
```

### From Frontend (JavaScript)

```javascript
async function getPrediction(playerStats) {
  const response = await fetch('http://localhost:5002/predict', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      playerName: playerStats.name,
      currentStats: playerStats,
      homeGame: true
    })
  });
  
  return await response.json();
}
```

## Development Workflow

1. **Local Development**
   ```bash
   cd prediction
   pip install -r requirements.txt
   python app.py
   ```

2. **Test Locally**
   ```bash
   python test_service.py
   ```

3. **Build Docker Image**
   ```bash
   docker build -t prediction .
   ```

4. **Run in Docker Compose**
   ```bash
   docker-compose up -d prediction
   ```

## Summary

The Prediction Service is a **standalone, scalable microservice** that:

✅ Uses PyTorch for flexibility and future GNN support  
✅ Provides REST API for easy integration  
✅ Runs independently in Docker  
✅ Has clear upgrade path to advanced ML  
✅ Fits seamlessly into existing architecture  

**Next Steps:** Train on real data, integrate with Stats service, add more features!

