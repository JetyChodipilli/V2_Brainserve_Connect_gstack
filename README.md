# BrainServe Connect

BrainServe Connect is a production-oriented appointment, employee, visitor, and physical-access
management application for Brain Serve Pvt. Ltd. It is implemented as a Java 21 Spring Boot
modular monolith with a React/TypeScript frontend.

The first release supports the complete operational path:

1. A visitor requests an appointment and receives an unguessable tracking reference.
2. The selected host reviews and approves or rejects the request.
3. Reception verifies the visitor, assigns a badge, and checks the visitor in.
4. Security can see the live emergency list.
5. Reception checks the visitor out and the badge is released.
6. Every privileged transition is written to the audit log and an outbox event.

It also includes organization and employee onboarding, permission-scoped compensation records,
availability management, role-specific dashboards, refresh-token rotation, standardized problem
responses, and a red-and-white glassmorphism design system.

## Technology

- Backend: Java 21, Spring Boot 4.1, Spring Security, OAuth2 Resource Server, Spring Data JPA,
  Spring Modulith, Flyway, PostgreSQL, Redis, Actuator, Micrometer, OpenAPI
- Frontend: React 19, TypeScript strict mode, Vite, TanStack Query, React Hook Form, Zod
- Required local services: PostgreSQL and Redis
- Reserved integration services in Compose: MinIO and Mailpit (not connected to the current release)

## Start with Docker

Copy `.env.example` to `.env`, replace every development credential, then run:

```bash
docker compose up --build
```

- Web application: `http://localhost:5173`
- API: `http://localhost:8080/api/v1`
- OpenAPI UI: `http://localhost:8080/scalar`
- Health: `http://localhost:8080/actuator/health`
- Mailpit: `http://localhost:8025`
- MinIO console: `http://localhost:9001`

## Run without Docker

Requirements: Java 21, Maven 3.9+, Node 24+, PostgreSQL 16+, and Redis 7+.

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In another terminal:

```bash
cd frontend
npm ci
npm run dev
```

If Maven is already bundled with IntelliJ, `mvnw.cmd` automatically checks the common IntelliJ
Maven location after checking `MAVEN_HOME` and `PATH`.

## Verification

```bash
cd backend
./mvnw verify

cd ../frontend
npm ci
npm run typecheck
npm test -- --run
npm run build
```

PostgreSQL and Redis integration tests use Testcontainers and skip only when Docker is unavailable.

## Development sign-in

Bootstrap accounts are created only when the corresponding environment variables are present:

- `BS_BOOTSTRAP_CEO_EMAIL` / `BS_BOOTSTRAP_CEO_PASSWORD`
- `BS_BOOTSTRAP_ADMIN_EMAIL` / `BS_BOOTSTRAP_ADMIN_PASSWORD`

There are no password defaults in the application. The Docker Compose development profile passes
values from `.env`; never reuse those values in production. First sign-in requires a password
change.

The frontend also has an explicit **Preview workspace** action. Preview mode uses in-memory sample
records only and is visually labelled; it never sends or persists production data.

## Documentation

- [Product and implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Requirements traceability](docs/REQUIREMENTS_TRACEABILITY.md)
- [Architecture and domain boundaries](docs/ARCHITECTURE.md)
- [Security model](docs/SECURITY.md)
- [API examples](docs/API_EXAMPLES.md)
- [Operations runbook](docs/OPERATIONS.md)
- [Verification report](docs/VERIFICATION.md)
- [gstack example prompt](docs/GSTACK_EXAMPLE_PROMPT.md)

## Current release boundary

The repository implements the core domains and the complete appointment-to-checkout journey.
Transactional outbox rows are persisted, but the provider delivery worker, document upload,
malware scanning, and object-storage adapters are explicit follow-up scope. Development can reveal
the one-time code only when `BS_REVEAL_OTP=true`; production must connect the outbox to an approved
email/SMS provider before public launch. Production deployment, DNS, secret-manager configuration,
and external provider credentials are intentionally not performed by this repository.
