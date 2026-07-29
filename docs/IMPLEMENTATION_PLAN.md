# Product and implementation plan

## Outcome

Replace spreadsheet and message-based reception coordination with one traceable workflow for
appointment request, host decision, arrival, check-in, emergency visibility, and check-out.

## Office-hours framing

- **Demand reality:** reception, security, hosts, and HR use this workflow daily. The supplied
  specifications define repeated operational and compliance needs.
- **Status quo:** disconnected calls, spreadsheets, and informal messages make current state,
  ownership, and sensitive-data access hard to prove.
- **Desperate specificity:** a receptionist handling a waiting visitor needs to know, within
  seconds, whether the host approved, whether the visitor is restricted, and which badge is free.
- **Narrowest complete wedge:** one visitor journey with authenticated approval and immutable
  transition history.
- **Observation plan:** measure approval time, repeated check-in rejection, current-inside
  accuracy, and whether an emergency list can be produced immediately.
- **Future fit:** module boundaries allow notifications, document scanning, analytics, and
  external access-control integrations to be extracted later.

## Facts, assumptions, and bets

| Type | Statement |
| --- | --- |
| Fact | Java 21, Spring Boot, PostgreSQL, Redis, React/TypeScript, and seven portals are required. |
| Fact | Backend permission and ownership checks are the security boundary. |
| Fact | Salary data must be isolated from generic employee and reception APIs. |
| Decision | The latest user instruction overrides the PDF's orange palette with BrainServe red and white. |
| Assumption | Version one serves one company with branch and department scope, not multiple tenants. |
| Bet | A modular monolith reduces delivery and operational risk while retaining extraction paths. |

## Reconciled scope

### Included

- JWT access tokens, rotating refresh sessions, account lockout, and forced password change
- roles plus independent permissions
- branches, departments, designations, employees, status transitions, and manager-cycle checks
- effective-dated compensation with maker-checker authorization
- weekly availability and UTC appointment slots
- public booking, approval/rejection, tracking reference, and lifecycle validation
- visitor consent, restriction status, QR-token reference, badge, check-in, check-out
- audit and transactional outbox records
- role-specific dashboards and accessible responsive frontend
- Flyway schema, Docker Compose, CI, tests, OpenAPI, health checks, and runbooks

### Non-goals for this release

- full payroll processing
- biometric or facial recognition
- Kafka
- physical gate-controller hardware
- production DNS, secret manager, email/SMS/WhatsApp credentials, or cloud deployment
- a full enterprise content-management UI

## Delivery phases

| Phase | Deliverable | Verification |
| --- | --- | --- |
| Foundation | project, migrations, error contract, correlation IDs, health | clean schema and context test |
| Identity | bootstrap, login, refresh rotation, permission authorities | auth and escalation tests |
| People | organization, employee ID, status and ownership | domain and controller tests |
| Booking | availability, public request, host decision | state and double-booking tests |
| Access | visitor verification, badge, check-in/out, emergency list | lifecycle tests |
| Sensitive domains | compensation, audit, outbox | permission and DTO-leak tests |
| Experience | public and role portals, responsive design | typecheck, component tests, build, browser QA |
| Handoff | Compose, CI, docs, packaged source | clean-workspace verification |

## Acceptance criteria

1. A public visitor can submit a validated request and receive a non-sequential reference.
2. Only the host or a user with `APPOINTMENT_APPROVE` can approve the request.
3. A unique database constraint prevents two active appointments for the same host and start time.
4. Check-in requires approval and cannot be repeated.
5. The emergency list contains only visitors whose access record is currently checked in.
6. Reception and generic employee endpoints never serialize compensation fields.
7. Compensation reads require `SALARY_READ` and create an audit event.
8. Refresh tokens are random, stored as hashes, rotated on use, and revocable by family.
9. API failures use RFC 7807-style problem details with a correlation ID.
10. The frontend builds under TypeScript strict mode and supports mobile, tablet, and desktop.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Bootstrap credential leakage | no code defaults; environment-only seed with forced change |
| IDOR | permission plus ownership checks in application services |
| Concurrent booking | transaction recheck plus partial unique index |
| Salary leakage | separate module, DTOs, endpoints, permission, and audit |
| Notification outage | outbox commits with the business transaction |
| File malware | quarantine state and scanner port before download |
| Redis outage | fail closed for OTP/rate-limit operations; normal authenticated reads remain available |
| Provider lock-in | transactional outbox and provider-neutral event payloads |

## Approval state

Implementation is authorized by the build request. External deployment, publishing, credential
creation, and production-system changes are not authorized and remain outside this delivery.
