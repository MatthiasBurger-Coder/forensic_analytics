# Three Amigos Decision Record

## Requirement Summary

Create a workflow for local Docker Compose deployment coverage across the
requested service roots. Every named root receives at least one implementation
slice. The deployment must use a Docker network named `forensic_analytics` and
must include a deployment description that lets an operator start the stack,
open the GUI, and collect possible runtime or integration errors.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The goal, named roots, required network, slice requirement, and GUI outcome are explicit. The only accepted assumption is that `forensic_analytics` is the repository stack/network name, not a Gradle service module. |
| Senior System Architect | FULL_PATH is required because deployment, service boundaries, target-vs-transitional roots, and runtime readiness are affected. Planned roots must not be represented as runnable services without implementation evidence. |
| Senior Java Backend Developer | Most Java application roots already have Dockerfiles and service-local configuration. `cli-client` is an application without Dockerfile. `graph-replay-service` and `report-generation-service` are Gradle `base` roots with no runtime implementation. |
| Senior React Frontend Developer | The GUI goal requires `forensic-ui` integration even though it was not in the user's service list. The UI must call `query-report-api-service` public REST routes only, preferably through a verified same-origin nginx `/api` proxy. |
| Senior Tester | Slice checks must combine module tests, `bootJar` or `build`, `.dockerignore` build-context verification, Compose config validation, `git diff --check`, the minimum repository quality gate, and final full local quality gate. Runtime smoke checks must be reported as executed, skipped, or blocked. |

## Requirement Classification

- Functional requirement: per-service Docker Compose descriptors and local
  deployment runbook.
- Non-functional requirement: deterministic local deployment, explicit
  network, reproducible validation commands, clear startup/cleanup behavior.
- Architecture constraint: preserve service ownership and target-vs-
  transitional distinctions.
- UX requirement: local GUI must be available for manual interaction.
- Observability requirement: collect logs and health status without treating
  logs as forensic evidence.
- Quality-gate requirement: use `QUALITY.md` commands and Compose validation.

## Accepted Assumptions

- `forensic_analytics` is the root deployment/network boundary, because no
  Gradle subproject named `forensic_analytics` exists and the repository path
  itself is `/mnt/d/Projects/forensic_analytics`.
- `forensic-ui` is included because the user goal depends on GUI interaction.
- Compose descriptors are local Docker evidence only, not production, Docker
  Swarm, or Kubernetes readiness.
- Planned roots may receive a readiness slice and documented non-runnable
  status rather than fabricated runtime.
- Browser GUI deployment should use same-origin `/api` proxying unless a later
  slice verifies a different browser-safe path.

## Non-Goals

- No service extraction.
- No missing graph, replay, report, or observability runtime implementation.
- No shared Java implementation modules.
- No cross-service private database or filesystem access.
- No Kubernetes, Docker Swarm, brokers, external databases, Graph DB, Vector
  DB, or live LLM providers.

## Risks

- Several internal ports overlap and need unique host mappings.
- Joern image builds may require external image pulls.
- The UI bakes `VITE_API_BASE_URL` during build.
- Current `forensic-ui/nginx.conf` returns 502 for `/api`.
- Root `.dockerignore` may block target-service Dockerfiles from copying boot
  jars unless it is updated.
- Some service roots are transitional or planned, so full-stack deployment may
  expose existing gaps rather than complete behavior.

## Open Questions

- Operator-preferred host ports were not specified. The workflow requires
  deterministic defaults and documentation of any change.

## Blocking Questions

None for workflow creation. Later implementation must stop when a Compose
descriptor would require guessed runtime behavior.

## Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 86 percent.
