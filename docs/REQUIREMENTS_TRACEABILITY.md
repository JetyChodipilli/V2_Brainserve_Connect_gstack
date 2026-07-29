# Requirements traceability

The implementation reconciles the three supplied BrainServe specifications with the user's latest
visual direction. The user's red-and-white monochrome request takes precedence over the earlier
orange-and-white palette.

| Source | Requirement theme | Implementation evidence |
| --- | --- | --- |
| BrainServe Appointment System | seven role experiences and visitor lifecycle | permission-derived workspace sections, public booking/tracking, host decisions, reception arrivals, badge check-in/out, security emergency list |
| BrainServe Appointment System | HR and compensation separation | organization/employee modules, maker-checker compensation module, salary-free reception DTOs, System Admin salary exclusion |
| BrainServe Implementation Plan | Java 21 modular monolith | Spring Boot, Spring Modulith boundary test, package-by-domain application interfaces |
| BrainServe Implementation Plan | PostgreSQL, Redis, Flyway, observability | versioned migration, Redis rate limits, Actuator, Prometheus, correlation IDs |
| BrainServe Implementation Plan | React/TypeScript portal | React 19, strict TypeScript, TanStack Query, React Hook Form, Zod, responsive glassmorphism design |
| BrainServe Service Implementation Prompts | real implementation and validation | executable Docker topology, backend state tests, Testcontainers migration test, component tests, dependency audit, browser rendering |
| Latest user instruction | BrainServe red/white brand expression | monochrome ruby palette, translucent glass panels, responsive public and internal surfaces |

## Deliberate release boundary

The provider delivery worker, real email/SMS delivery, document upload, malware scanning, and
managed object-storage adapters are not represented as complete. The current release persists
provider-neutral outbox events and can reveal OTPs only in an explicitly enabled development mode.
Those integrations are required before a real public launch.
