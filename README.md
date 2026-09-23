# RecurringFlex - Subscription Management Platform

## Project Overview

RecurringFlex is a microservices-based subscription management platform built using Spring Boot and Spring Cloud. The system handles user authentication, subscription plan management, subscription lifecycle operations, and payment processing through a set of independently deployable services communicating via REST APIs and Netflix Eureka service discovery.

This project demonstrates core Service-Oriented Architecture (SOA) principles including service decomposition, API gateway routing, inter-service communication using OpenFeign, database-per-service pattern, and JWT-based stateless authentication.

---

## Architecture

```
                        +------------------+
                        |   API Gateway    |
                        |   (Port 8080)    |
                        +--------+---------+
                                 |
                +----------------+----------------+
                |                |                |
        +-------v------+ +------v-------+ +------v-------+
        | User Service | | Subscription | |   Payment    |
        | (Port 8082)  | |   Service    | |   Service    |
        |              | | (Port 8081)  | | (Port 8083)  |
        +-------+------+ +------+-------+ +------+-------+
                |                |                |
          +-----v----+   +------v------+   +-----v-----+
          | user_db  |   |subscription |   |payment_db |
          |          |   |    _db      |   |           |
          +----------+   +------------+   +-----------+

        All services register with Eureka Server (Port 8761)
```

---

## Technology Stack

| Component             | Technology                          |
|-----------------------|-------------------------------------|
| Language              | Java 17                             |
| Framework             | Spring Boot 3.2.5                   |
| Service Discovery     | Spring Cloud Netflix Eureka         |
| API Gateway           | Spring Cloud Gateway                |
| Security              | Spring Security + JWT (jjwt 0.12.5) |
| Database              | PostgreSQL                          |
| ORM                   | Spring Data JPA / Hibernate         |
| Inter-Service Comm.   | Spring Cloud OpenFeign              |
| API Documentation     | SpringDoc OpenAPI (Swagger UI)      |
| Build Tool            | Apache Maven                        |

---

## Services

### 1. Eureka Server (Discovery Service)

- **Port:** 8761
- **Purpose:** Service registry for all microservices. Each service registers itself on startup and discovers other services through Eureka.
- **Dashboard:** http://localhost:8761

### 2. API Gateway

- **Port:** 8080
- **Purpose:** Single entry point for all client requests. Routes incoming requests to the appropriate downstream service using Spring Cloud Gateway. Implements JWT-based authentication filtering.
- **Key Features:**
  - Route configuration for all services
  - JWT validation filter for secured endpoints
  - CORS configuration

### 3. User Service

- **Port:** 8082
- **Database:** user_db
- **Purpose:** Handles user registration, authentication, and JWT token generation.
- **Swagger UI:** http://localhost:8082/swagger-ui.html

#### API Endpoints

| Method | Endpoint                    | Description               | Auth Required |
|--------|-----------------------------|---------------------------|---------------|
| POST   | /api/users/register         | Register a new user       | No            |
| POST   | /api/users/login            | Authenticate and get JWT  | No            |
| GET    | /api/users/{id}             | Get user by ID            | No            |
| GET    | /api/users/username/{name}  | Get user by username      | No            |

### 4. Subscription Service

- **Port:** 8081
- **Database:** subscription_db
- **Purpose:** Manages subscription plans and user subscriptions. Communicates with User Service and Payment Service via OpenFeign clients.
- **Swagger UI:** http://localhost:8081/swagger-ui.html

#### Plan Endpoints

| Method | Endpoint            | Description              |
|--------|---------------------|--------------------------|
| POST   | /api/plans          | Create a new plan        |
| GET    | /api/plans          | Get all plans            |
| GET    | /api/plans/active   | Get active plans         |
| GET    | /api/plans/{id}     | Get plan by ID           |
| PUT    | /api/plans/{id}     | Update a plan            |
| DELETE | /api/plans/{id}     | Deactivate a plan        |

#### Subscription Endpoints

| Method | Endpoint                           | Description                    |
|--------|------------------------------------|--------------------------------|
| POST   | /api/subscriptions/subscribe       | Subscribe user to a plan       |
| GET    | /api/subscriptions                 | Get all subscriptions          |
| GET    | /api/subscriptions/{id}            | Get subscription by ID         |
| GET    | /api/subscriptions/user/{userId}   | Get subscriptions by user      |
| GET    | /api/subscriptions/user/{userId}/active | Get active subscriptions  |
| PUT    | /api/subscriptions/{id}/cancel     | Cancel a subscription          |
| PUT    | /api/subscriptions/{id}/renew      | Renew a subscription           |

### 5. Payment Service

- **Port:** 8083
- **Database:** payment_db
- **Purpose:** Processes mock payments for subscriptions. Generates unique transaction references and records payment history.
- **Swagger UI:** http://localhost:8083/swagger-ui.html

#### API Endpoints

| Method | Endpoint                                  | Description                       |
|--------|-------------------------------------------|-----------------------------------|
| POST   | /api/payments/process                     | Process a payment                 |
| GET    | /api/payments                             | Get all payments                  |
| GET    | /api/payments/{id}                        | Get payment by ID                 |
| GET    | /api/payments/subscription/{subId}        | Get payments by subscription      |
| GET    | /api/payments/user/{userId}               | Get payments by user              |

---

## Prerequisites

- Java 17 (JDK)
- Apache Maven 3.8+
- PostgreSQL 15+
- pgAdmin (optional, for database management)

---

## Database Setup

Create the following three databases in PostgreSQL:

```sql
CREATE DATABASE user_db;
CREATE DATABASE subscription_db;
CREATE DATABASE payment_db;
```

The application uses the following default credentials (configured in each service's `application.yml`):

| Property | Value    |
|----------|----------|
| Username | postgres |
| Password | root     |

Update the `application.yml` files in each service if your PostgreSQL credentials differ.

Tables are automatically created on application startup via Hibernate's `ddl-auto: update` setting.

---

## Build and Run

### Step 1: Build all services

```bash
cd eureka-server && mvn clean install
cd api-gateway && mvn clean install
cd user-service && mvn clean install
cd subscription-service && mvn clean install
cd payment-service && mvn clean install
```

### Step 2: Start services in order

Start the services in the following sequence to ensure proper service registration:

```bash
# Terminal 1 - Start Eureka Server first
cd eureka-server
mvn spring-boot:run

# Terminal 2 - Start API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 3 - Start User Service
cd user-service
mvn spring-boot:run

# Terminal 4 - Start Subscription Service
cd subscription-service
mvn spring-boot:run

# Terminal 5 - Start Payment Service
cd payment-service
mvn spring-boot:run
```

### Step 3: Verify all services are registered

Open the Eureka dashboard at http://localhost:8761 and confirm that all four services (API-GATEWAY, USER-SERVICE, SUBSCRIPTION-SERVICE, PAYMENT-SERVICE) are listed with status UP.

---

## Testing the APIs

### 1. Register a User

```
POST http://localhost:8082/api/users/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@recurringflex.com",
  "password": "password123",
  "role": "USER"
}
```

### 2. Login

```
POST http://localhost:8082/api/users/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

Copy the `token` from the response for authenticated requests.

### 3. Create a Plan

```
POST http://localhost:8081/api/plans
Content-Type: application/json

{
  "name": "Basic Monthly",
  "description": "Basic plan with standard features",
  "price": 9.99,
  "durationInDays": 30,
  "active": true
}
```

### 4. Subscribe to a Plan

```
POST http://localhost:8081/api/subscriptions/subscribe
Content-Type: application/json

{
  "userId": 1,
  "planId": 1,
  "autoRenew": true
}
```

### 5. Check Payment History

```
GET http://localhost:8083/api/payments/user/1
```

---

## Project Structure

```
RecurringFlex/
|-- eureka-server/              # Service Discovery Server
|   |-- src/main/java/          # Eureka Server Application
|   |-- src/main/resources/     # application.yml
|
|-- api-gateway/                # API Gateway
|   |-- src/main/java/
|   |   |-- config/             # CORS configuration
|   |   |-- filter/             # JWT authentication filter
|   |-- src/main/resources/     # application.yml with route definitions
|
|-- user-service/               # User Management and Authentication
|   |-- src/main/java/
|   |   |-- config/             # Swagger, CORS, Security configuration
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities
|   |   |-- repository/         # Spring Data repositories
|   |   |-- security/           # JWT utility, Security config
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- subscription-service/       # Plans and Subscription Management
|   |-- src/main/java/
|   |   |-- client/             # Feign clients (User, Payment)
|   |   |-- config/             # Swagger, CORS configuration
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities (Plan, Subscription)
|   |   |-- repository/         # Spring Data repositories
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- payment-service/            # Payment Processing
|   |-- src/main/java/
|   |   |-- config/             # Swagger, CORS configuration
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities
|   |   |-- repository/         # Spring Data repositories
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- docs/
|   |-- SRS.md                  # Software Requirements Specification (Rubric 1)
|   |-- linkedin-article.md     # DTI & SOA Architecture Article (Rubric 5)
|
|-- docker-compose.yml          # Containerized deployment specification
|-- validate-system.ps1         # Automated PowerShell E2E validation script
|-- validate-system.sh          # Automated Bash E2E validation script
|-- init-databases.sql          # Database initialization script
|-- generate-ssl.bat            # SSL certificate generation script
```

---

## Role-Based Access Control (RBAC)

The platform enforces role-based access control at the API Gateway layer:
- **`USER`**: Can register, login, view active subscription plans, subscribe to plans, renew subscriptions, cancel subscriptions, and view their own payment receipts.
- **`ADMIN`**: In addition to standard user capabilities, only administrators can perform Plan Management mutations (`POST /api/plans`, `PUT /api/plans/**`, `DELETE /api/plans/**`). Any non-admin attempting plan modifications receives `HTTP 403 Forbidden`.

---

## Circuit Breaker and Resilience

The API Gateway integrates **Spring Cloud Circuit Breaker (Resilience4j)** to prevent cascading failures across microservices:
- Each downstream route (`user-service`, `subscription-service`, `payment-service`) is protected by an independent circuit breaker.
- If a downstream service encounters a 50% failure rate over 10 requests, or if requests exceed the 3-second timeout, the circuit transitions to `OPEN`.
- Downstream failures are intercepted by `FallbackController`, returning a structured `HTTP 503 SERVICE UNAVAILABLE` JSON response without dropping client connections.

---

## Automated Testing & System Validation

### Unit and Integration Tests

Run unit test suites across all services:

```bash
# Run tests for subscription service (lifecycle, plans, scheduler)
cd subscription-service && mvn test

# Run tests for user service (auth, registration, JWT)
cd ../user-service && mvn test

# Run tests for payment service (transactions, history)
cd ../payment-service && mvn test
```

### End-to-End Validation Script

An automated end-to-end test script verifies the entire running system:
1. Eureka Service Registry availability
2. User registration and JWT authentication
3. Admin registration and RBAC enforcement (blocking standard user from plan creation, allowing admin)
4. Subscription activation, renewal, and cancellation lifecycle
5. Payment transaction logging and audit trail
6. Circuit breaker fallback responsiveness

```powershell
# On Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File validate-system.ps1

# On Linux / macOS (Bash)
chmod +x validate-system.sh
./validate-system.sh
```

---

## Docker Deployment

To deploy the entire platform (PostgreSQL, Eureka, Gateway, and all 3 microservices) via Docker:

```bash
# 1. Package all services into JARs
mvn clean package -DskipTests

# 2. Start the cluster
docker-compose up -d

# 3. Check container status
docker-compose ps

# 4. View logs
docker-compose logs -f
```

---

## Project Documentation

Detailed project documentation is available in the `docs/` directory:
- [Software Requirements Specification (SRS)](docs/SRS.md): Comprehensive formal specification including functional requirements, non-functional requirements, use case diagrams, and domain data models.
- [LinkedIn Article on SOA & Digital Transformation](docs/linkedin-article.md): Professional publication covering Digital Transformation & Innovation (DTI) principles, SOA architectural patterns, and practical implementation insights.

---

## Cross-Cutting Concerns

### CORS

All services are configured to accept cross-origin requests from `http://localhost:3000`, `http://localhost:4200`, and `http://localhost:8080` to support frontend frameworks during development.

### JWT Authentication

- Tokens are issued by the User Service upon successful login.
- The API Gateway validates JWT tokens for all secured endpoints via the `JwtAuthenticationFilter`.
- Public endpoints (`/api/users/register`, `/api/users/login`, Swagger UI paths) bypass authentication.
- User identity (`X-User-Id`, `X-User-Name`, `X-User-Role`) is injected into downstream request headers.

### Inter-Service Communication

- The Subscription Service communicates with the User Service and Payment Service using Spring Cloud OpenFeign declarative REST clients.
- Service discovery is handled automatically through Eureka, eliminating hardcoded service URLs.

### Database-per-Service Pattern

Each microservice owns its database, ensuring loose coupling and independent deployability:
- User Service: `user_db`
- Subscription Service: `subscription_db`
- Payment Service: `payment_db`

---

## Configuration

Service ports and database connections are defined in each service's `application.yml`:

| Service              | Port | Database         |
|----------------------|------|------------------|
| Eureka Server        | 8761 | None             |
| API Gateway          | 8080 | None             |
| User Service         | 8082 | user_db          |
| Subscription Service | 8081 | subscription_db  |
| Payment Service      | 8083 | payment_db       |
