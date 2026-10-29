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

| Method | Endpoint                    | Description                                | Access         |
|--------|-----------------------------|--------------------------------------------|----------------|
| POST   | /api/users/register         | Register a new user (always `USER`)        | Public         |
| POST   | /api/users/login            | Authenticate and get JWT                   | Public         |
| GET    | /api/users                  | List all users                             | ADMIN          |
| GET    | /api/users/{id}             | Get user by ID                             | Owner or ADMIN |
| GET    | /api/users/username/{name}  | Get user by username                       | Owner or ADMIN |
| PUT    | /api/users/{id}             | Update email / password (role: ADMIN only) | Owner or ADMIN |
| DELETE | /api/users/{id}             | Delete a user                              | Owner or ADMIN |

### 4. Subscription Service

- **Port:** 8081
- **Database:** subscription_db
- **Purpose:** Manages subscription plans and user subscriptions. Communicates with User Service and Payment Service via OpenFeign clients.
- **Swagger UI:** http://localhost:8081/swagger-ui.html

#### Plan Endpoints

| Method | Endpoint            | Description              | Access        |
|--------|---------------------|--------------------------|---------------|
| POST   | /api/plans          | Create a new plan        | ADMIN         |
| GET    | /api/plans          | Get all plans            | Authenticated |
| GET    | /api/plans/active   | Get active plans         | Authenticated |
| GET    | /api/plans/{id}     | Get plan by ID           | Authenticated |
| PUT    | /api/plans/{id}     | Update a plan            | ADMIN         |
| DELETE | /api/plans/{id}     | Deactivate a plan        | ADMIN         |

#### Subscription Endpoints

| Method | Endpoint                                | Description                                       | Access         |
|--------|-----------------------------------------|---------------------------------------------------|----------------|
| POST   | /api/subscriptions/subscribe            | Subscribe to a plan (`userId` defaults to caller) | Owner or ADMIN |
| GET    | /api/subscriptions                      | Get all subscriptions                             | ADMIN          |
| GET    | /api/subscriptions/{id}                 | Get subscription by ID                            | Owner or ADMIN |
| GET    | /api/subscriptions/user/{userId}        | Get subscriptions by user                         | Owner or ADMIN |
| GET    | /api/subscriptions/user/{userId}/active | Get active subscriptions                          | Owner or ADMIN |
| PUT    | /api/subscriptions/{id}/cancel          | Cancel a subscription                             | Owner or ADMIN |
| PUT    | /api/subscriptions/{id}/renew           | Renew a subscription (charges the plan price)     | Owner or ADMIN |
| DELETE | /api/subscriptions/{id}                 | Delete a subscription                             | ADMIN          |

A subscription stays `ACTIVE` only once its payment succeeds. If payment-service declines the charge or is unreachable, the subscription is set to `CANCELLED` and the API returns `HTTP 402`. A failed renewal payment leaves the end date unchanged.

### 5. Payment Service

- **Port:** 8083
- **Database:** payment_db
- **Purpose:** Processes mock payments for subscriptions. Generates unique transaction references and records payment history.
- **Swagger UI:** http://localhost:8083/swagger-ui.html

#### API Endpoints

| Method | Endpoint                           | Description                  | Access                       |
|--------|------------------------------------|------------------------------|------------------------------|
| POST   | /api/payments/process              | Process a payment            | ADMIN / subscription-service |
| GET    | /api/payments                      | Get all payments             | ADMIN                        |
| GET    | /api/payments/{id}                 | Get payment by ID            | Owner or ADMIN               |
| GET    | /api/payments/subscription/{subId} | Get payments by subscription | Own payments only, or ADMIN  |
| GET    | /api/payments/user/{userId}        | Get payments by user         | Owner or ADMIN               |
| PUT    | /api/payments/{id}/refund          | Refund a successful payment  | ADMIN                        |
| DELETE | /api/payments/{id}                 | Delete a payment record      | ADMIN                        |

> Swagger UIs on the service ports are for documentation only. API calls sent directly to a service port are rejected with `HTTP 401`. Send them through the API Gateway on port 8080.

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
for s in eureka-server api-gateway user-service subscription-service payment-service; do
  (cd $s && mvn clean install)
done
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

# Terminal 6 - Start a SECOND User Service instance (for the load-balancing demo)
cd user-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8092
```

### Step 3: Verify all services are registered

Open the Eureka dashboard at http://localhost:8761 and confirm that all four services (API-GATEWAY, USER-SERVICE, SUBSCRIPTION-SERVICE, PAYMENT-SERVICE) are listed with status UP. USER-SERVICE should show two instances (ports 8082 and 8092).

---

## Testing the APIs

Send all requests through the API Gateway (`http://localhost:8080`).

### 1. Register a User

```
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@recurringflex.com",
  "password": "password123"
}
```

Every self-registered account gets the `USER` role. A `role` field in the body is ignored.

### 2. Login

```
POST http://localhost:8080/api/users/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

Copy the `token` from the response. Send it as `Authorization: Bearer <token>` on every other request.

To log in as admin, use the seeded account (`admin` / `Admin@12345` by default; see [Security](#security)).

### 3. Create a Plan (admin token)

```
POST http://localhost:8080/api/plans
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Basic Monthly",
  "description": "Basic plan with standard features",
  "price": 9.99,
  "durationInDays": 30,
  "active": true
}
```

### 4. Subscribe to a Plan (user token)

```
POST http://localhost:8080/api/subscriptions/subscribe
Authorization: Bearer <user-token>
Content-Type: application/json

{
  "planId": 1,
  "autoRenew": true
}
```

### 5. Check Payment History

```
GET http://localhost:8080/api/payments/user/1
Authorization: Bearer <user-token>
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
|   |   |-- config/             # Swagger, CORS, admin seeding, instance-id header
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities
|   |   |-- exception/          # Custom exceptions + global exception handler
|   |   |-- repository/         # Spring Data repositories
|   |   |-- security/           # JWT utility, Security config, gateway auth filter
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- subscription-service/       # Plans and Subscription Management
|   |-- src/main/java/
|   |   |-- client/             # Feign clients (User, Payment)
|   |   |-- config/             # Swagger, CORS, Feign auth, instance-id header
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities (Plan, Subscription)
|   |   |-- exception/          # Custom exceptions + global exception handler
|   |   |-- repository/         # Spring Data repositories
|   |   |-- security/           # Gateway auth filter, caller identity
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- payment-service/            # Payment Processing
|   |-- src/main/java/
|   |   |-- config/             # Swagger, CORS, instance-id header
|   |   |-- controller/         # REST endpoints
|   |   |-- dto/                # Request/Response DTOs
|   |   |-- entity/             # JPA entities
|   |   |-- exception/          # Custom exceptions + global exception handler
|   |   |-- repository/         # Spring Data repositories
|   |   |-- security/           # Gateway auth filter, caller identity
|   |   |-- service/            # Business logic
|   |-- src/main/resources/     # application.yml
|
|-- docs/
|   |-- SRS.md                  # Software Requirements Specification (Rubric 1)
|   |-- linkedin-article.md     # DTI & SOA Architecture Article (Rubric 5)
|
|-- docker-compose.yml          # Containerized deployment specification
|-- init-databases.sql          # Database initialization script
```

---

## Security

### JWT authentication

- User Service issues HS256-signed tokens on login. Each token carries `sub` (user id), `username`, `role`, `iss` and a 1-hour expiry. Passwords are hashed with BCrypt.
- The API Gateway's `JwtAuthenticationFilter` checks the signature, expiry and issuer on every non-public request. Missing, tampered or expired tokens get `HTTP 401`.
- Login returns the same `Invalid username or password` error for an unknown user and a wrong password, so usernames cannot be enumerated.

### Role-based access control and ownership

- **`USER`**: can view plans and manage only their **own** profile, subscriptions and payments. Requests for another user's data get `HTTP 403`.
- **`ADMIN`**: can also manage plans, list all users, subscriptions and payments, change roles, refund and delete payments, and delete subscriptions.
- Self-registration always creates a `USER`. The first `ADMIN` is seeded at startup from `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` (defaults `admin` / `admin@recurringflex.com` / `Admin@12345`). Admins can promote other users with `PUT /api/users/{id}` and `{"role":"ADMIN"}`.
- Checks run in two places: coarse path rules at the gateway, and per-resource ownership checks in each service.

### Gateway-only access

- The gateway removes any client-supplied `X-User-*` headers. It then sets `X-User-Id` / `X-User-Role` from the verified token and adds a shared `X-Internal-Secret`.
- Every service rejects `/api/**` requests that lack that secret. Calling a service port directly with forged headers therefore fails. In Docker the service ports are not published at all.
- Feign calls between services add the same secret and the `SYSTEM` role.
- Only the explicit gateway routes are exposed. The discovery locator is disabled, so auto-generated `/<service-id>/**` routes cannot dodge the path rules.

### Secrets

`JWT_SECRET`, `INTERNAL_SECRET` and `ADMIN_PASSWORD` are read from environment variables. The values in `application.yml` are development defaults only. Set real values outside local development.

---

## Load Balancing

- Every gateway route uses `lb://<service-id>`. Spring Cloud LoadBalancer resolves instances from Eureka and distributes requests round-robin.
- Feign clients in subscription-service (`UserClient`, `PaymentClient`) balance across instances the same way.
- Each service adds an `X-Instance-Id` response header (`<service>:<host>:<port>`), which shows the instance that handled each request:

```bash
for i in 1 2 3 4; do
  curl -s -o /dev/null -D - -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/1 | grep -i x-instance-id
done
# X-Instance-Id: user-service:host:8092
# X-Instance-Id: user-service:host:8082
# ...
```

- Locally: start a second instance with `--server.port=8092` (see Build and Run).
- Docker: `user-service` and `payment-service` run 2 replicas each (`deploy.replicas`). Scale further with `docker compose up -d --scale user-service=3`. `subscription-service` stays at 1 replica because its scheduler must not run twice.

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

---

## Docker Deployment

To deploy the entire platform (PostgreSQL, Eureka, Gateway, and all 3 microservices) via Docker:

```bash
# 1. Package all services into JARs
for s in eureka-server api-gateway user-service subscription-service payment-service; do
  (cd $s && mvn clean package -DskipTests)
done

# 2. Start the cluster (optionally export JWT_SECRET, INTERNAL_SECRET, ADMIN_PASSWORD first)
docker-compose up -d --build

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
- Public endpoints (`/api/users/register`, `/api/users/login`) bypass authentication.
- User identity (`X-User-Id`, `X-User-Name`, `X-User-Role`) is injected into downstream request headers. Services enforce ownership with it; see [Security](#security).

### Error Handling

Each service has a `GlobalExceptionHandler` that maps errors to consistent JSON (`{"error": "..."}`) and status codes:

| Status | Cause                            |
|--------|----------------------------------|
| 400    | Validation or business rule      |
| 401    | Bad credentials                  |
| 402    | Payment failed                   |
| 403    | Access denied                    |
| 404    | Resource not found               |
| 503    | A dependent service is down      |

Request bodies are validated with Bean Validation (`@Valid`).

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
| User Service         | 8082 (+8092 second instance) | user_db |
| Subscription Service | 8081 | subscription_db  |
| Payment Service      | 8083 | payment_db       |
