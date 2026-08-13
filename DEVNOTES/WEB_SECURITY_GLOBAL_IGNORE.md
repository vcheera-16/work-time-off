# DEVNOTES: Global security ignore

This temporary change configures Spring Security to ignore all requests (web.ignoring()/"/**").
Use only for local development and debugging. Do NOT merge or enable in production.

To revert:
- Restore WebSecurityIgnoreConfig to ignore only static/H2 paths, or remove the file.

To test:
1) mvn clean package
2) mvn spring-boot:run -Dspring-boot.run.profiles=h2
3) Open http://localhost:8080/ and verify main.js loads and H2 console renders.
