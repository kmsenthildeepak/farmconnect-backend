# FarmConnect Backend

Spring Boot REST API for **FarmConnect**, a farmer-to-customer direct selling platform.

FarmConnect connects customers with farmers and provides authentication, product management, shopping cart, orders, reviews, notifications, farmer verification, password reset, and image storage.

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

## Main Features

### Authentication & Security
- Customer, Farmer, and Admin authentication
- JWT-based authentication
- Role-based authorization
- Farmer verification
- Account block/unblock controls
- Secure password reset with expiring one-time tokens

### Customer
- Browse products
- Search and filter products
- Shopping cart
- Checkout and orders
- Order tracking
- Reviews for eligible delivered orders
- Notifications

### Farmer
- Farmer registration and verification
- Product management
- Product image upload
- Stock and availability management
- Order management
- Order status updates
- Sales information

### Admin
- Dashboard statistics
- Customer management
- Farmer management
- Farmer verification
- Product and order management
- User block/unblock controls

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
```

## Configuration

Sensitive configuration is supplied through environment variables.

Use `.env.example` as the reference for required variables.

Important configuration includes:

- Database connection
- JWT secret
- Password reset base URL
- Email credentials
- Cloudinary credentials
- Application logging level

**Do not commit `.env` files or real credentials.**

## Local Development

### Requirements

- JDK 17 or newer
- Maven
- MySQL 8

### Clone the repository

```bash
git clone https://github.com/kmsenthideepak/farmconnect-backend.git
cd farmconnect-backend
```
## Local Secret Helper

For Windows development, the repository includes PowerShell scripts that store the local MySQL password using Windows DPAPI.

```powershell
.\scripts\setup-local-secret.ps1
```

Then:

```powershell
.\scripts\run-local.ps1
```

The encrypted password is stored under `.local-secrets/`, which is ignored by Git.

## API Overview

The backend exposes REST APIs under `/api`.

Main API areas include:

```text
/api/auth
/api/products
/api/customer/cart
/api/customer/orders
/api/farmer/orders
/api/admin
/api/notifications
```

Protected endpoints use JWT bearer authentication.

## Password Reset

The password reset flow includes:

- Secure random reset tokens
- SHA-256 token hashing
- Token expiration
- One-time token usage
- BCrypt password hashing
- Configurable reset URL
- Email-based reset instructions

## Image Storage

Product images are stored using **Cloudinary** in production.

Local file storage is also supported for development when Cloudinary is not configured.

Cloudinary credentials must be supplied through environment variables.

## Docker

Build the backend image:

```bash
docker build -t farmconnect-backend .
```

Run it with the required environment variables:

```bash
docker run -p 8080:8080 farmconnect-backend
```

The application uses the `PORT` environment variable when supplied, with `8080` as the default.

## Testing

The backend uses JUnit 5 and Mockito.

Current test suite:

```text
45 tests
0 failures
0 errors
```

Run the tests with:

```bash
mvn clean test
```

## Production Architecture

```text
FarmConnect Android App
          |
          | HTTPS / REST API
          v
Spring Boot Backend
          |
     +----+----+
     |         |
     v         v
   MySQL   Cloudinary
     |
     v
Application Data
```

## Related Project

### FarmConnect Android App

Native Android application built with Java and XML.

**Android Repository:**

https://github.com/kmsenthildeepak/farmconnect-android

The Android application communicates with this Spring Boot backend through REST APIs.

## Security

This repository is public, so production credentials, passwords, private keys, database backups, and local secret files must not be committed.

Sensitive configuration is externalized through environment variables.

GitHub security features such as secret scanning, push protection, and Dependabot should be enabled for this repository.

## Author

**Deepak S**

B.E. Computer Science and Engineering

KPR Institute of Engineering and Technology
