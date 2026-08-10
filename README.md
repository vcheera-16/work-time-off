# Work Time Off

Simple scaffold for Work Time Off application.

Requirements
- Java 17
- Maven
- PostgreSQL (for production; app will run without DB for static pages)

Run locally
1. Build: mvn package
2. Run: mvn spring-boot:run
3. Open: http://localhost:8080

What's included
- Spring Boot backend scaffold
- Static frontend served from src/main/resources/static (index.html, main.js, main.css)
- Flyway migrations folder with initial schema

Next steps
- Implement authentication endpoints and secure the API
- Implement TimeOffRequest entity and CRUD endpoints
- Add tests and CI workflow
