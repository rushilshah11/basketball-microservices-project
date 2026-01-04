# Architecture Decision Records (ADRs)

## ADR 001: Use Redis for Event Bus

### Context
When a user adds a player to their watchlist, we want to trigger a prediction. We realized that if we did this all at once, the user would have to wait for the prediction to finish before seeing a **"Success"** message.

### Decision
We decided to use **Redis Pub/Sub** as a lightweight event bus—a “fire and forget” system.  
The Stats service simply announces that a player was added and immediately moves on.

### Consequences
- The app feels much faster to the user.
- If the prediction service is busy, it does not slow down the rest of the app.
- Services remain **decoupled**, meaning they don’t have to wait on each other.

### Tradeoffs
- No message persistence (events are lost if a consumer is down)
- No replay or guaranteed delivery
- Simpler than Kafka but less reliable

---

## ADR 002: Python Sidecar for NBA Data

### Context
We found that Python has significantly better tools and libraries for handling NBA data and AI models than Java.

### Decision
We built a small Python **sidecar service** dedicated to fetching NBA data and running data-heavy logic.

### Consequences
- Our main Java codebase stays clean and focused.
- External API logic does not pollute core services.
- We use the **best language for the job**:
  - Java for security and infrastructure
  - Python for data and AI workflows

---

## ADR 003: Database-Per-Service Pattern

### Context
We wanted to follow professional microservice best practices by avoiding a shared, monolithic database.

### Decision
Each service (e.g., Security and Stats) was given its own **private PostgreSQL database**.

### Consequences
- If the Stats database fails, users can still log in via the Security service.
- Prevents **tight coupling** and “spaghetti data.”
- Each service owns and evolves its schema independently.

### Tradeoffs
- No cross-service joins
- More complex data consistency
  
---

## ADR 004: Microservices vs. Monolith

### Context
We could have built a single monolithic application, but we wanted hands-on experience with how modern systems (e.g., Netflix, Uber) are designed.

### Decision
We chose a **microservices architecture**, splitting the system into multiple focused services.

### Consequences
- Strong learning experience in:
  - Networking
  - Service discovery
  - Inter-service communication
- Allows independent scaling (e.g., scaling Login separately from Prediction).

### Tradeoffs
- Higher operational complexity
- Harder local development
- Requires service discovery, networking, and observability
  
---

## ADR 005: gRPC for Internal Communication

### Context
Our services communicate frequently (e.g., validating user tokens). REST/JSON introduces unnecessary overhead for internal traffic.

### Decision
We used **gRPC** for internal service-to-service communication.

### Consequences
- Faster communication using a **binary protocol**.
- Strongly-typed contracts via `.proto` files.
- Compile-time guarantees that services agree on message structure.

### Tradeoffs
- Harder to debug than REST
- Requires code generation
- Not browser-friendly
  
---

## ADR 006: REST for Client-Facing API

### Context
We needed a simple and widely supported way for the frontend (Next.js) to interact with backend services.

### Decision
We exposed **REST APIs** for all client-facing communication.

### Consequences
- Industry standard and easy to debug.
- Simple integration with tools like Postman.
- Compatible with web, mobile, and third-party clients.

---

## ADR 007: API Gateway Pattern

### Context
With multiple services running on different ports, we didn’t want the frontend to manage multiple backend addresses.

### Decision
We introduced an **API Gateway** to act as the system’s front door.

### Consequences
- The frontend communicates with a **single endpoint**.
- Centralized routing and security.
- Simplifies frontend configuration and deployment.

### Tradeoffs
- Extra latency hop
