# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🏗️ High-Level Architecture

This project implements a Spring Boot REST API for ticket management and user authentication. The architecture is structured into layers:

1.  **Controller Layer**: Handles incoming HTTP requests and calls the Service Layer.
2.  **Service Layer**: Contains the core business logic (e.g., user creation, ticket state changes).
3.  **Security Layer**: Manages authentication, authorization, and JWT token handling.
4.  **Repository Layer**: Handles data persistence (JPA/Spring Data).

**Key Design Patterns Used:**
*   **JWT Authentication:** Stateless authentication using Bearer tokens.
*   **Separation of Concerns:** Critical modules like `TokenService` are isolated to handle specific responsibilities (e.g., token blacklisting) to maintain high cohesion.

## 🔒 Security Enhancements (Critical Focus)

The primary security focus is on robust token validation and revocation.

**1. JWT Access:**
All protected endpoints require a valid JWT token in the `Authorization` header (`Bearer <token>`).

**2. Token Revocation (Blacklisting):**
To mitigate the risks of compromised tokens, a central `TokenService` is used to blacklist access tokens upon explicit logout or session termination.

*   **File:** `src/main/java/com/example/ticketing/global/security/TokenService.java`
*   **Functionality:** Utilizes Redis to store the token (`key: blacklist:token`) along with an expiration time (TTL).
*   **Usage:** Any request handler must first call `TokenService.isAccessTokenBlacklisted(token)` to verify the token's validity before processing the request.

## 📚 Component Deep Dive

### 1. Authentication Flow (`JwtAuthenticationFilter` / `JwtTokenProvider`)
The primary mechanism for token validation. The `JwtAuthenticationFilter` intercepts every request, extracts the token, validates it using `JwtTokenProvider`, and checks its status against the `TokenService`'s blacklist.

### 2. User Management (`UserController`)
Handles user profile retrieval and creation, operating under the protection of the implemented security filters.

## 🛠️ Development Commands

### Build & Run
*   **Dependencies:** Java JDK 17+
*   **Build:** `mvn clean install`
*   **Run:** `mvn spring-boot:run`
*   **Testing:** Use `mvn test` to run unit and integration tests.

### Development Tips
*   **Security Check:** When modifying authentication or authorization logic, always validate the token against the `TokenService` blacklist check.
*   **API Endpoints:** The base URL is typically `http://localhost:8080/api/v1`.
*   **Best Practice:** Use the `TokenService` when implementing 'logout' or 'session ended' logic to ensure the token is immediately unusable.