@'
# FarmConnect Backend

Spring Boot REST API for **FarmConnect**, a farmer-to-customer direct selling platform.

FarmConnect connects customers with farmers and provides authentication, product management, shopping cart, orders, reviews, notifications, farmer verification, password reset, and image storage.

---

## Tech Stack

- Java 17
- Spring Boot 3.5.16
- Spring Security
- JWT Authentication
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- Cloudinary
- Docker
- JUnit 5 / Mockito

The project is compiled with Java 17 compatibility and can be developed using a newer JDK that supports the configured Java release.

---

## Project Structure

```text
farmconnect-backend/
├── src/
│   ├── main/
│   │   ├── java/com/farmconnect/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-mailpit.properties
│   │       ├── application-gmail.properties
│   │       └── reset-password.html
│   └── test/
├── scripts/
│   ├── setup-local-secret.ps1
│   └── run-local.ps1
├── Dockerfile
├── .dockerignore
├── .env.example
├── pom.xml
└── README.md
