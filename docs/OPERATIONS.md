# Operations runbook

## Health and diagnostics

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- Prometheus metrics: `/actuator/prometheus` on the management network
- Every API response contains `X-Correlation-ID`; search structured logs using that value.

## PostgreSQL failure

1. Stop write traffic or mark the instance unready.
2. Confirm database reachability and pool saturation without printing credentials.
3. Restore service, then verify Flyway status and the outbox backlog.
4. If data is damaged, restore the latest backup into an isolated database and validate before cutover.

## Redis failure

OTP and rate-limit operations fail closed. Existing access tokens and database-backed refresh
revocation continue to work. Restore Redis, verify health, and ask users with expired OTPs to
request new ones.

## Reserved object-storage integration

Document upload is not enabled in this release. Before enabling it, add private object storage,
malware quarantine, authorized downloads, health checks, and a tested failure runbook.

## Outbox growth

The current release persists provider-neutral outbox events but does not dispatch them. Before a
public launch, add an idempotent delivery worker and alerts for pending age, retry count, and dead
events. Until that adapter exists, use `BS_REVEAL_OTP=true` only in isolated development.

## Compromised refresh token

Use logout-all for the account, disable it when compromise is active, rotate JWT signing material
when broader exposure is suspected, inspect authentication audit events, and require a password
reset.

## Emergency evacuation

Open the Security portal emergency list, export or print if the network is unstable, reconcile
each active access record at the assembly point, and record manual corrections after the event.

## Backup/restore proof

Run a scheduled encrypted PostgreSQL backup, restore it to an isolated environment, run Flyway
validation and a visitor lifecycle smoke test, record duration and row-count reconciliation, then
destroy the isolated copy according to retention policy.
