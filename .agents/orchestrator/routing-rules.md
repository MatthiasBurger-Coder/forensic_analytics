# Routing Rules

Use these rules to select roles for a slice. Prefer the smallest set that covers the actual files and risks.

## Role Routing

- Backend domain, application, persistence, static analysis, runtime ingestion, gRPC or Protobuf work routes to `roles/senior-java-backend.md`.
- React, frontend state, API client integration or UI component work routes to `roles/senior-react-frontend.md`.
- Information architecture, accessibility, visualization UX or user-flow work routes to `roles/senior-ux-designer.md`.
- Cross-module design, package boundaries, architecture tests or module restructuring routes to `roles/senior-system-architect.md`.
- Test strategy, regression coverage, ArchUnit, coverage gates or quality failures route to `roles/senior-tester.md`.
- Gradle, Docker, Kubernetes, CI, observability or deployment work routes to `roles/senior-devops.md`.
- Multi-role coordination, conflict resolution or slice planning routes to `roles/senior-swarm-orchestrator.md`.

## Escalation

Stop and report when a required file, task, symbol, schema, command or contract cannot be verified exactly.

Use `QUALITY.md` for verification commands. Optional external checks such as Sonar require documented credentials and must be reported when skipped.
