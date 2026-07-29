# Security model

## Trust boundaries

- The browser is untrusted. Frontend route guards improve navigation only.
- Every protected use case checks permission, ownership, or organizational scope in the backend.
- Public appointment input, uploaded metadata, QR values, and provider responses are untrusted.
- Salary, government identifiers, tokens, passwords, and object keys are sensitive.

## Controls

- BCrypt password hashing
- short-lived signed JWT access tokens
- random rotating refresh tokens stored only as SHA-256 hashes
- refresh-family revocation on replay
- account lockout after configurable failures
- first-login password change
- CORS allowlist and secure response headers
- request validation and typed DTOs
- JPA parameter binding and no arbitrary SQL
- permission-based `@PreAuthorize` checks
- ownership checks for own profile and assigned appointments
- append-only audit records for privileged and sensitive access
- correlation IDs without token or decrypted PII logging
- database uniqueness plus transactional recheck for booking concurrency

## Role rules

Permissions are authorities independent of roles. `ROLE_SYSTEM_ADMIN` intentionally excludes salary
permissions. A grant operation must be authorized for each permission and cannot alter the actor's
own privileged role.

## Production checklist

1. Replace all `.env` values and move secrets to a secret manager.
2. Use asymmetric JWT signing or a managed identity provider.
3. Configure HTTPS/HSTS at ingress and set secure cookie attributes.
4. Restrict database, Redis, MinIO, Mailpit, and Actuator to private networks.
5. Keep document upload disabled until private object storage, signature validation, quarantine,
   authorized download, and malware scanning are implemented.
6. Configure backups and prove restoration on a schedule.
7. Run dependency/container scanning and DAST against the deployed environment.
8. Send structured audit logs to protected centralized storage.
9. Configure rate limits at both ingress and application levels.
10. Review retention, privacy notice, and consent language with counsel.
