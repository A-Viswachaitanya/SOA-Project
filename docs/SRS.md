# Software Requirements Specification (SRS)
# RecurringFlex - Subscription Lifecycle Management Engine (PS009)

## 1. Introduction

### 1.1 Purpose
This document defines the functional and non-functional requirements for RecurringFlex, a microservices-based subscription management platform. It serves as a reference for design, development, testing, and evaluation of the system.

### 1.2 Scope
RecurringFlex is a platform that enables businesses to manage recurring billing plans, user registrations, subscription activations, automated renewals, lifecycle status tracking, and mock payment processing. The system follows a Service-Oriented Architecture (SOA) using independently deployable microservices.

### 1.3 Definitions and Acronyms

| Term   | Definition                                              |
|--------|---------------------------------------------------------|
| SOA    | Service-Oriented Architecture                           |
| JWT    | JSON Web Token                                          |
| API    | Application Programming Interface                      |
| CRUD   | Create, Read, Update, Delete                            |
| REST   | Representational State Transfer                         |
| DTI    | Digital Transformation and Innovation                   |
| SRS    | Software Requirements Specification                     |
| RBAC   | Role-Based Access Control                               |

### 1.4 References
- Spring Boot 3.2.5 Documentation
- Spring Cloud Netflix Eureka Documentation
- Spring Cloud Gateway Documentation
- Spring Security and JWT (jjwt 0.12.5) Documentation
- PostgreSQL 15 Documentation

---

## 2. Overall Description

### 2.1 Product Perspective
RecurringFlex operates as a distributed system composed of five microservices communicating over HTTP/REST. An API Gateway serves as the single entry point, routing requests to downstream services discovered via a Eureka service registry. Authentication is stateless, using JWT tokens issued by the User Service and validated at the Gateway level.

### 2.2 Product Functions
- User registration and authentication with JWT
- Subscription plan management (CRUD operations)
- Subscription lifecycle management (activate, renew, cancel, expire)
- Mock payment processing and transaction history
- Service discovery and dynamic routing
- Role-based access control (ADMIN, USER)
- Cross-origin resource sharing (CORS) for frontend integration

### 2.3 User Classes and Characteristics

| User Class    | Description                                                  |
|---------------|--------------------------------------------------------------|
| End User      | Registers, logs in, subscribes to plans, views history       |
| Administrator | Manages plans, views all subscriptions, accesses all data    |
| System        | Automated processes (scheduled expiry checks, auto-renewal)  |

### 2.4 Operating Environment
- Java Runtime Environment: JDK 17
- Database: PostgreSQL 15+
- Operating System: Windows / Linux / macOS
- Build Tool: Apache Maven 3.8+

### 2.5 Design and Implementation Constraints
- Each microservice must own its database (database-per-service pattern)
- Inter-service communication must use declarative REST clients (OpenFeign)
- Authentication must be stateless (no server-side session storage)
- Services must register with Eureka for dynamic discovery

---

## 3. Functional Requirements

### 3.1 User Service (FR-US)

| ID       | Requirement                                                       | Priority |
|----------|-------------------------------------------------------------------|----------|
| FR-US-01 | System shall allow users to register with username, email, password, and role | High |
| FR-US-02 | System shall prevent duplicate usernames and emails                | High     |
| FR-US-03 | System shall hash passwords using BCrypt before storage            | High     |
| FR-US-04 | System shall authenticate users and return a signed JWT token      | High     |
| FR-US-05 | System shall support retrieval of user details by ID or username   | Medium   |
| FR-US-06 | JWT tokens shall contain username, role, and expiration claims     | High     |
| FR-US-07 | JWT tokens shall expire after 24 hours (configurable)             | Medium   |

### 3.2 Subscription Service (FR-SS)

| ID       | Requirement                                                       | Priority |
|----------|-------------------------------------------------------------------|----------|
| FR-SS-01 | System shall support creating subscription plans with name, description, price, and duration | High |
| FR-SS-02 | System shall support listing, updating, and deactivating plans    | High     |
| FR-SS-03 | System shall allow users to subscribe to an active plan           | High     |
| FR-SS-04 | System shall calculate subscription end date based on plan duration| High     |
| FR-SS-05 | System shall track subscription status as ACTIVE, EXPIRED, or CANCELLED | High |
| FR-SS-06 | System shall support subscription cancellation                    | High     |
| FR-SS-07 | System shall support subscription renewal (extending end date)    | High     |
| FR-SS-08 | System shall trigger payment processing upon subscription creation| High     |
| FR-SS-09 | System shall validate user existence via User Service before subscribing | Medium |
| FR-SS-10 | System shall support auto-renewal flag per subscription           | Medium   |

### 3.3 Payment Service (FR-PS)

| ID       | Requirement                                                       | Priority |
|----------|-------------------------------------------------------------------|----------|
| FR-PS-01 | System shall process mock payments with subscription and user context | High |
| FR-PS-02 | System shall generate unique transaction reference per payment    | High     |
| FR-PS-03 | System shall record payment status as SUCCESS or FAILED           | High     |
| FR-PS-04 | System shall support retrieval of payments by user or subscription | Medium  |
| FR-PS-05 | System shall persist all payment records for audit trail          | Medium   |

### 3.4 API Gateway (FR-GW)

| ID       | Requirement                                                       | Priority |
|----------|-------------------------------------------------------------------|----------|
| FR-GW-01 | Gateway shall route requests to downstream services based on path predicates | High |
| FR-GW-02 | Gateway shall validate JWT tokens on secured endpoints            | High     |
| FR-GW-03 | Gateway shall reject requests with invalid or expired tokens      | High     |
| FR-GW-04 | Gateway shall allow unauthenticated access to registration and login endpoints | High |
| FR-GW-05 | Gateway shall implement circuit breaker for fault tolerance       | Medium   |
| FR-GW-06 | Gateway shall propagate user identity (role, username) to downstream services | Medium |

### 3.5 Service Discovery (FR-SD)

| ID       | Requirement                                                       | Priority |
|----------|-------------------------------------------------------------------|----------|
| FR-SD-01 | All services shall register with Eureka Server on startup         | High     |
| FR-SD-02 | Services shall discover peers through Eureka rather than hardcoded URLs | High |
| FR-SD-03 | Eureka dashboard shall display all registered service instances   | Medium   |

---

## 4. Non-Functional Requirements

| ID       | Category        | Requirement                                                   |
|----------|-----------------|---------------------------------------------------------------|
| NFR-01   | Security        | All passwords must be hashed using BCrypt                     |
| NFR-02   | Security        | JWT secret key must be at least 256 bits                      |
| NFR-03   | Security        | CORS must be configured to allow only specified origins       |
| NFR-04   | Performance     | API response time should be under 500ms for standard operations |
| NFR-05   | Availability    | Services should gracefully handle downstream failures         |
| NFR-06   | Maintainability | Each service must be independently buildable and deployable   |
| NFR-07   | Scalability     | Architecture must support horizontal scaling of any service   |
| NFR-08   | Data Integrity  | Each service must own its database exclusively                |
| NFR-09   | Logging         | All services must log to standard output with timestamps      |
| NFR-10   | Documentation   | APIs must be documented via Swagger/OpenAPI                   |

---

## 5. System Architecture

```
  Client Application
         |
         v
  +-------------+
  | API Gateway  |  (Port 8080) - JWT Filter, Route Definitions
  +------+------+
         |
    +----+----+----+
    |         |    |
    v         v    v
+--------+ +----+ +-------+
| User   | |Sub | |Payment|
|Service | |Svc | |Service|
|(8082)  | |(8081)|(8083) |
+---+----+ +--+-+ +---+---+
    |         |        |
    v         v        v
 user_db  sub_db   payment_db

  All services register with Eureka Server (Port 8761)
```

---

## 6. Data Model

### 6.1 User Entity (user_db)

| Field      | Type         | Constraints                |
|------------|--------------|----------------------------|
| id         | BIGSERIAL    | Primary Key, Auto-generated|
| username   | VARCHAR(255) | NOT NULL, UNIQUE           |
| email      | VARCHAR(255) | NOT NULL, UNIQUE           |
| password   | VARCHAR(255) | NOT NULL (BCrypt hashed)   |
| role       | VARCHAR(255) | NOT NULL, default "USER"   |
| created_at | TIMESTAMP    | Auto-set on creation       |

### 6.2 Plan Entity (subscription_db)

| Field           | Type         | Constraints                |
|-----------------|--------------|----------------------------|
| id              | BIGSERIAL    | Primary Key, Auto-generated|
| name            | VARCHAR(255) | NOT NULL                   |
| description     | VARCHAR(255) | Nullable                   |
| price           | DECIMAL      | NOT NULL                   |
| duration_in_days| INTEGER      | NOT NULL                   |
| active          | BOOLEAN      | NOT NULL, default TRUE     |
| created_at      | TIMESTAMP    | Auto-set on creation       |

### 6.3 Subscription Entity (subscription_db)

| Field      | Type         | Constraints                    |
|------------|--------------|--------------------------------|
| id         | BIGSERIAL    | Primary Key, Auto-generated    |
| user_id    | BIGINT       | NOT NULL                       |
| plan_id    | BIGINT       | NOT NULL, FK to plans          |
| start_date | DATE         | NOT NULL                       |
| end_date   | DATE         | NOT NULL                       |
| status     | VARCHAR      | NOT NULL (ACTIVE/EXPIRED/CANCELLED) |
| auto_renew | BOOLEAN      | default FALSE                  |
| created_at | TIMESTAMP    | Auto-set on creation           |

### 6.4 Payment Entity (payment_db)

| Field           | Type         | Constraints                |
|-----------------|--------------|----------------------------|
| id              | BIGSERIAL    | Primary Key, Auto-generated|
| subscription_id | BIGINT       | NOT NULL                   |
| user_id         | BIGINT       | NOT NULL                   |
| amount          | DECIMAL      | NOT NULL                   |
| status          | VARCHAR      | NOT NULL (SUCCESS/FAILED)  |
| transaction_ref | VARCHAR      | UNIQUE                     |
| payment_date    | TIMESTAMP    | Auto-set on creation       |

---

## 7. Acceptance Criteria

| Test Case | Description                                          | Expected Result                      |
|-----------|------------------------------------------------------|--------------------------------------|
| TC-01     | Register a new user                                  | 201 Created with user details        |
| TC-02     | Register duplicate username                          | 400 Bad Request with error message   |
| TC-03     | Login with valid credentials                         | 200 OK with JWT token                |
| TC-04     | Login with invalid credentials                       | 401 Unauthorized                     |
| TC-05     | Create a subscription plan                           | 201 Created with plan details        |
| TC-06     | Subscribe user to active plan                        | 201 Created with subscription + payment |
| TC-07     | Cancel an active subscription                        | 200 OK, status changed to CANCELLED  |
| TC-08     | Renew a cancelled subscription                       | 200 OK, status changed to ACTIVE     |
| TC-09     | Access secured endpoint without token                | 401 Unauthorized                     |
| TC-10     | Access secured endpoint with valid token             | 200 OK with response data            |
| TC-11     | Verify all services in Eureka dashboard              | All 4 services listed as UP          |
| TC-12     | Verify payment created after subscription            | Payment record exists in payment_db  |
