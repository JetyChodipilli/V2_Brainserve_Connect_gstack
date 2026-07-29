# gstack Work Mode example prompt

The following realistic prompt was used to exercise the gstack Work Mode process end to end:

> We run Brain Serve Pvt. Ltd. and reception still manages appointments in spreadsheets and
> WhatsApp. Build a secure Spring Boot and React application where visitors can book a host,
> employees approve requests, reception verifies a QR code and checks visitors in and out, and
> security has a live emergency list. HR must be able to onboard employees, but reception and
> system administrators must never see salary data without a separate permission. Use a premium
> red-and-white glassmorphism interface. Include local infrastructure, migrations, tests,
> developer documentation, and evidence that the complete visitor journey works.

The project follows the gstack sequence:

| Stage | Project evidence |
| --- | --- |
| Think | `IMPLEMENTATION_PLAN.md` demand, wedge, assumptions, and non-goals |
| Plan | `ARCHITECTURE.md`, API contracts, state transitions, threat boundaries |
| Build | `backend/`, `frontend/`, migrations, Compose configuration |
| Review | permission model, module boundaries, problem responses, sensitive DTO separation |
| Test | backend unit/integration/security tests and frontend component/build checks |
| Ship | reproducible ZIP/source handoff; no external deployment without explicit approval |
| Verify | build output, test reports, dependency audit, and production-preview smoke checks |
| Learn | known extension points and follow-up risks in the implementation plan |
