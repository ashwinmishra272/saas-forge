# 🚀 SaaSForge Platform

A **multi-tenant SaaS backend platform** built using **Spring Boot**, designed to simulate real-world production systems with authentication, tenant isolation, and scalable architecture.

---

## 📌 Vision

SaaSForge is being built to:

- Learn **production-grade backend engineering**
- Demonstrate **real-world system design**
- Showcase skills required for **top backend roles**
- Implement scalable architecture like real SaaS companies

---

## 🧠 What This Project Demonstrates

- Multi-tenant architecture
- Authentication & Authorization (JWT)
- Role-based access control (RBAC)
- Clean layered architecture (Controller → Service → Repository)
- Secure password handling (BCrypt)
- Database design with relationships
- Production mindset (scalability, modularity)

---

## 🏗️ Tech Stack

- **Backend:** Spring Boot (Java)
- **Database:** PostgreSQL
- **ORM:** Hibernate / JPA
- **Security:** Spring Security + JWT
- **Build Tool:** Gradle
- **Future Additions:** Redis, Kafka, Docker, AWS

---

## 🧩 Current Features

### ✅ Tenant Management
- Register new tenant
- Unique tenant key generation

### ✅ User Management
- Create admin user during tenant registration
- Associate users with tenant and role

### ✅ Role Management
- System roles (ADMIN, USER)

### ✅ Authentication
- Login API
- JWT token generation

### ✅ Security
- Password hashing using BCrypt
- Spring Security basic config

---

## 🔐 Authentication Flow

1. User registers → Tenant + Admin created
2. User logs in → receives JWT token
3. Token will be used to access protected APIs

Upcoming:
- JWT validation filter
- Security context handling
- Role-based authorization

---

## 🧱 Architecture

```
Controller → Service → Repository → Database
```

- Controller: Handles API requests
- Service: Business logic
- Repository: DB interaction
- Entity: Database models

---

## 📂 Project Structure

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

## ⚙️ Setup Instructions

### 1. Clone Repository
```
git clone <your-repo-url>
```

### 2. Configure Database
```
spring.datasource.url=jdbc:postgresql://localhost:5432/saasforge
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Run Application
```
./gradlew bootRun
```

---

## 📡 API Endpoints

### Register Tenant
```
POST /api/tenants/register
```

### Login
```
POST /api/auth/login
```

---

## 🚧 Upcoming Features (Roadmap)

### 🔥 Phase 1 (Current Focus)
- JWT Authentication Filter
- Secure protected APIs
- Exception handling
- Input validation

### 🔥 Phase 2
- Redis caching
- API rate limiting
- Pagination & filtering
- Audit logging

### 🔥 Phase 3
- Kafka event system
- Email notifications
- Async processing

### 🔥 Phase 4
- Docker containerization
- AWS deployment (EC2 + RDS)
- CI/CD pipeline

---

## 🎯 Long-Term Goals

- Build a **production-ready SaaS backend**
- Handle **multi-tenant scaling**
- Add **real-world complexity**

---


## 👨‍💻 Author

Ashwin Mishra

---

## ⚡ Status

🚧 Actively under development  
🔥 Built with a focus on learning and mastery