# Verification report

## Result

The source is complete with one environment caveat: this workspace has Java 17 and no Docker
daemon. The Maven project remains configured for Java 21; compilation and packaging were verified
by overriding the release level to 17, which is supported by Spring Boot 4.1. The real PostgreSQL
Flyway test is included and automatically skipped only when Docker is unavailable.

## Evidence

| Check | Result |
| --- | --- |
| Maven clean package | pass; executable Spring Boot JAR produced |
| Spring Modulith boundaries | pass |
| Appointment state tests | 3 pass |
| Employee state tests | 2 pass |
| Compensation maker-checker tests | 2 pass |
| PostgreSQL/Flyway Testcontainers test | skipped: no Docker daemon |
| TypeScript strict check | pass |
| Frontend component tests | 3 pass |
| Vite production build | pass |
| Production-preview `/` and `/book` smoke check | HTTP 200; hashed JS/CSS served |
| Chromium visual QA | pass; desktop landing and mobile booking renders inspected |
| Production dependency audit | 0 vulnerabilities |
| Hard-coded credential scan | no findings |

## Required release-gate rerun

On a Java 21 host with Docker:

```bash
cd backend
./mvnw clean verify

cd ../frontend
npm ci
npm run typecheck
npm test -- --run
npm run build
npm audit --omit=dev
```

Then start the Compose stack, complete one request → OTP verification → approval → check-in →
emergency-list → check-out smoke test, and connect an approved provider delivery worker before
accepting real public bookings.
