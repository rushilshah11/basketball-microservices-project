# Basketball Microservices Project

A distributed microservices application for tracking NBA players, managing watchlists, and predicting player performance using a neural network. The system uses a mix of **Java Spring Boot** and **Python FastAPI** services, orchestrated via **Eureka** and protected by an **API Gateway**.

## 🏗 System Architecture

The project consists of 6 core services and supporting infrastructure:

| Service | Technology | Port | Description |
|---------|------------|------|-------------|
| **API Gateway** | Java / Spring Cloud Gateway | `8080` | Entry point. Handles routing, load balancing, and rate limiting. |
| **Eureka Server** | Java / Spring Cloud Netflix | `8761` | Service discovery registry. |
| **Security Service** | Java / Spring Security | `8082` | Handles JWT authentication, registration, and gRPC token verification. |
| **Stats Service** | Java / Spring Boot | `8081` | Manages player watchlists and orchestrates data fetching via Circuit Breakers. |
| **Prediction Service** | Python / FastAPI / PyTorch | `5002` | Neural network service that predicts player points/assists/rebounds. Listens to Redis events. |
| **NBA Fetcher** | Python / FastAPI | `5001` | Sidecar service that wraps the `nba_api` to fetch real-time stats. |

### Infrastructure
- **PostgreSQL:** Databases for Security, Stats, and Prediction services.
- **Redis:** Caching and Pub/Sub messaging for async events.
- **Zipkin:** Distributed tracing server (Port `9411`).
- **Prometheus:** Metrics scraping (Port `9090`).
- **Grafana:** Metrics dashboard (Port `3000`).

---

## 🔌 API Endpoints

All requests should be sent through the **API Gateway** on port **8080**.

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/register` | Register a new user. Returns JWT. |
| `POST` | `/authenticate` | Login. Returns JWT. |

### Player & Stats (`/api/players`)
*Requires `Authorization: Bearer <token>` header.*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/search?name=LeBron` | Search for a player by name. |
| `GET` | `/stats?name=LeBron...` | Get season averages for a player. |
| `GET` | `/games?name=LeBron...` | Get recent game logs. |

### Watchlist (`/api/watchlists`)
*Requires `Authorization: Bearer <token>` header.*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `?userId=1` | Add player to watchlist (Triggers async prediction). |
| `GET` | `?userId=1` | View your watchlist. |
| `DELETE` | `/{name}?userId=1` | Remove player from watchlist. |

### Predictions (`/predict`)
*Requires `Authorization: Bearer <token>` header.*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Get an on-demand prediction for specific stats. |
| `GET` | `/predictions/{name}` | Get the stored prediction for a player. |
| `POST` | `/training/collect/{name}` | Trigger background data collection for AI training. |

---

## 🧠 Features & Patterns

### 1. Resilience (Circuit Breaker & Rate Limiting)
The **Stats Service** uses **Resilience4j** to robustly handle interactions with the external NBA data source:
* **Circuit Breaker:** If the Python `NBA Fetcher` or the external RapidAPI fails repeatedly, the circuit opens to prevent cascading failures. Fallback methods return empty or cached data instead of crashing the user request.
* **Rate Limiting:** Protects the external API from being overwhelmed. If users or internal batch processes exceed the configured threshold (e.g., 10 requests/second), further requests are rejected or queued to ensure the system stays within API quota limits.

### 2. Event-Driven Architecture
When a user adds a player to their watchlist:
1.  **Stats Service** saves to DB and publishes a `PLAYER_ADDED` event to **Redis**.
2.  **Prediction Service** listens for this event asynchronously.
3.  It fetches fresh stats, runs the PyTorch model, and saves the prediction to its own Postgres DB.

### 3. Observability (Full Stack)
The project implements **Distributed Tracing** across Java and Python services using **Zipkin** and **OpenTelemetry/Micrometer**.
- You can trace a request from `Gateway` → `Stats` → `NBA Fetcher`.
- **Prometheus** scrapes metrics from all services.
- **Grafana** visualizes JVM usage, request latency, and prediction model performance.

### 4. Machine Learning Pipeline
The **Prediction Service** isn't just an API; it's a full MLOps pipeline:
- **Collector:** Fetches historical game data in the background.
- **Trainer:** Retrains the PyTorch neural network via the `/training/train` endpoint.
- **Inference:** Serves predictions with home/away game adjustments.

---

## 🧑‍💻 Authors
**Rushil Shah**  
📫 [LinkedIn](https://linkedin.com/in/rushilshahh)
💼 [Portfolio](https://rushilshah11.github.io/portfolio/)

**Ashwin Sanjaye**  
📫 [LinkedIn](https://www.linkedin.com/in/ashwin-sanjaye/)
💼 [Portfolio](https://ashwinsanjaye.com/)
