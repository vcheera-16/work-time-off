## Running with H2 (in-memory) for fast local development

This project supports an H2 in-memory profile for quick local testing without installing PostgreSQL.

How to run with H2:
- From the project root run:
  mvn spring-boot:run -Dspring-boot.run.profiles=h2

- Alternatively:
  export SPRING_PROFILES_ACTIVE=h2
  mvn spring-boot:run

What this does:
- Uses H2 in-memory DB (jdbc:h2:mem:work_time_off).
- Hibernate will auto-create the schema at startup (spring.jpa.hibernate.ddl-auto=create).
- Flyway is disabled for the H2 profile to avoid Postgres-specific SQL issues.
- The DataInitializer will seed default users (admin/manager/employee) when the DB is empty.
- H2 console is enabled at http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:work_time_off, user: sa, password: empty)

Notes:
- Data is ephemeral in this mode and will be lost when the application stops. Use Postgres for persistent data and production.
- To switch back to Postgres, run without the `h2` profile and ensure src/main/resources/application.properties is configured with your Postgres connection.
