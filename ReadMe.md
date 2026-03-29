# SaaSForge Platform

A **multi-tenant SaaS backend platform** built using **Spring Boot**, designed to simulate real-world production systems with authentication, tenant isolation, and scalable architecture.

---

## Vision

SaaSForge is being built to:

- Learn **production-grade backend engineering**
- Demonstrate **real-world system design**
- Showcase skills required for **top backend roles**
- Implement scalable architecture like real SaaS companies

---

## Tech Stack

- **Backend:** Spring Boot (Java)
- **Database:** PostgreSQL
- **ORM:** Hibernate / JPA
- **Security:** Spring Security + JWT
- **Build Tool:** Gradle
- **Future Additions:** Redis, Kafka, Docker, AWS

---

## Architecture

```
Controller → Service → Repository → Database
```

| Layer | Responsibility |
|-------|---------------|
| Controller | REST API endpoints |
| Service | Business logic |
| Repository | Database interaction |
| Entity | JPA models |
| Security | JWT + Filters |
| DTO | Request/Response models |

---

## Project Structure

```
com.saasforge
│
├── config          # Security configuration
├── controller      # REST APIs
├── dto             # Request/Response models
├── entity          # JPA entities
├── repository      # Database layer
├── security        # JWT + Filters
├── service         # Business logic
```

---

## Implemented Features

### Tenant Management
- Register new tenant with unique key generation
- Full CRUD operations (create, read, update, delete)
- Tenant-specific data isolation via `TenantContext`

### User Management
- Create admin user during tenant registration
- Associate users with tenant and role
- Tenant-filtered user queries

### Role Management
- System roles: `ADMIN`, `USER`
- Role-based access control with `@PreAuthorize`

### Authentication & Security
- Login API with JWT token generation
- JWT Authentication Filter (`JwtAuthenticationFilter`)
- JWT carries tenant context
- Password hashing with BCrypt
- Spring Security configuration
- Method-level security enforcement

### Exception Handling & Validation
- Global Exception Handler (`GlobalExceptionHandler`)
- Input validation on all endpoints
- Proper HTTP status codes and error responses

### Pagination & Filtering
- Paginated responses via `PageResponse` DTO
- `Pageable` support in `UserService` and `TenantService`
- Paginated repository queries with tenant filtering

### Testing
- Unit tests for `RoleService`, `RoleController`
- Unit tests for `JwtAuthenticationFilter`
- Unit tests for `UserService` and `TenantService` with pagination and tenant filtering

---

## API Endpoints

### Tenant
```
POST   /api/tenants/register
GET    /api/tenants/{id}
PUT    /api/tenants/{id}
DELETE /api/tenants/{id}
```

### Auth
```
POST /api/auth/login
```

---

## Setup Instructions

### 1. Clone Repository
```bash
git clone <your-repo-url>
```

### 2. Configure Database
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/saasforge
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Run Application
```bash
./gradlew bootRun
```

---

## Roadmap

See [futurePlan.md](futurePlan.md) for the full roadmap including upcoming phases:

- **Phase 2:** Redis caching, rate limiting, audit logging
- **Phase 3:** Kafka event system, email notifications, async processing
- **Phase 4:** Docker, AWS deployment (EC2 + RDS), CI/CD pipeline

---

## Status

Actively under development — Phase 1 complete.

---

## Author

Ashwin Mishra
