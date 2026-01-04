# Basketball Microservices Project

A full-stack, distributed system for basketball statistics and performance prediction, built with a microservices architecture.

## Architecture Overview

This project consists of several specialized services communicating through a central API Gateway and Service Discovery:

* **API Gateway**: The single entry point for the frontend, handling routing to backend services.
* **Eureka Server**: Provides service discovery, allowing microservices to find and communicate with each other.
* **Security Service (Auth)**: Manages user authentication and JWT-based authorization, backed by its own PostgreSQL database.
* **Stats Service**: Handles core basketball data and user watchlists. It uses Redis for caching and Pub/Sub events.
* **NBA Fetcher**: A Python-based sidecar service that retrieves raw data from external NBA APIs.
* **Prediction Service**: A machine learning service utilizing PyTorch to predict player/team performance.
* **Frontend**: A modern web interface built with Next.js.

## Tech Stack

* **Back-end**: Java (Spring Boot, Spring Cloud Gateway, Netflix Eureka)
* **Machine Learning/Fetcher**: Python (PyTorch, Flask)
* **Front-end**: Next.js (React), Tailwind CSS
* **Databases**: PostgreSQL (3 instances for data isolation), Redis (Caching & Events)
* **DevOps**: Docker & Docker Compose

## Getting Started

### Prerequisites

* Docker and Docker Desktop
* A `.env` file in the root directory (see `.env.example`)

### Quick Start with Docker

The easiest way to run the entire stack is using Docker Compose:

```bash
# Clone the repository
git clone [your-repo-url]
cd basketball-microservices-project

# Spin up all services
docker-compose -f docker-compose.dev.yml up --build
```

The application will be available at:
* Frontend: `http://localhost:3001`
* API Gateway: `http://localhost:8080`
* Eureka Dashboard: `http://localhost:8761`

## Project Structure

```
├── api-gateway/       # Spring Cloud Gateway
├── eureka-server/     # Service Discovery
├── security/          # Auth Service (Java/Spring)
├── stats/             # Stats & Watchlist Service (Java/Spring)
├── nba-fetcher/       # Python sidecar for data retrieval
├── prediction/        # PyTorch Neural Network service
├── frontend/          # Next.js web application
└── docs/              # Detailed API and Architectural documentation
```

## Documentation & Architecture

Detailed technical documentation and architectural decisions are maintained in the repository to provide insight into the system's design.

### Technical Docs

* [**Architectural Decision Records (ADRs)**](docs/ADRs.md): A log of the significant design choices made throughout the project's development.
* [**API Reference**](docs/API-reference.md): Detailed documentation of the available endpoints across the microservices.

### Architectural Diagrams

* **System Overview**:
   * [System Context Diagram](diagrams/system_context_diagram.jpg): High-level view of how users and external systems interact with the project.
   * [Container Diagram](diagrams/container_diagram.jpg): Breakdown of the major high-level technology building blocks.
* **Service Specifics**:
   * [Stats Service Component Diagram](diagrams/component_diagram_stats.jpg): Detailed look at the internal components of the Stats service.
   * [Database ERD](diagrams/Security_Stats_ERD.jpg): Entity Relationship Diagram for the Security and Stats databases.
* **Process Flows (Sequence Diagrams)**:
   * [Watchlist Logic](diagrams/sequence_addPlayerToWatchlist.jpg): Visualizing the process of adding a player to a user's watchlist.
   * [Error Handling & Resilience](diagrams/sequence_nbaFetcherDown.jpg): Sequence showing system behavior when the NBA Fetcher service is unavailable.

---

## Authors
**Rushil Shah**  
[LinkedIn](https://linkedin.com/in/rushilshahh)
[Portfolio](https://rushilshah11.github.io/portfolio/)

**Ashwin Sanjaye**  
[LinkedIn](https://www.linkedin.com/in/ashwin-sanjaye/)
[Portfolio](https://ashwinsanjaye.com/)
