# Documentation Synchronization

Documentation must follow implemented behavior. Do not describe planned features as active runtime behavior.

## Required Sync Points

| Change | Documentation to inspect and update |
|---|---|
| Spring Boot boundary accepted | `docs/adr/`, `docs/arc42/02-architecture-constraints.md`, `docs/arc42/05-building-block-view.md` |
| Boot app module added | `docs/README.md`, `docs/arc42/05-building-block-view.md`, `docs/arc42/07-deployment-view.md` |
| Configuration properties added | `docs/README.md`, `docs/arc42/07-deployment-view.md`, profile documentation |
| gRPC lifecycle moved to Boot | `docs/README.md`, `docs/arc42/03-system-scope-and-context.md`, `docs/arc42/06-runtime-view.md` |
| REST strategy changes | `docs/README.md`, `forensic-ui/README.md` if UI behavior changes, `docs/arc42/05-building-block-view.md`, new ADR if Spring MVC/WebFlux is selected |
| Observability policy changes | `docs/adr/ADR-0005-adapter-logging-observability-boundary.md`, new ADR if superseded, `docs/arc42/08-crosscutting-concepts.md` |
| Dependency-minimization decision accepted | `AGENTS.md` only if repository-wide agent rules change; otherwise this workplan or a dedicated ADR |
| Docker files added | `docs/README.md`, `docs/arc42/07-deployment-view.md`, `docker/README.md` if present |
| Quality commands change | `QUALITY.md` and CI workflows, only after verifying build tasks |

## ADR Expectations

Add new ADRs instead of rewriting history when a decision changes.

Expected ADR candidates:

- `ADR-0006: Spring Boot owns the outer server boundary`
- `ADR-0007: REST API strategy under Spring Boot`
- optional `ADR-0008: Spring-specific observability bridge`
- optional `ADR-0009: Initial relational analysis store`

ADR-0005 currently says the observability boundary uses JDK logging and does not introduce Spring AOP, AspectJ, SLF4J or concrete logging providers. Spring Boot migration must not silently contradict this.

## README Expectations

The repository currently has no root `README.md`. Do not document it as an affected file unless a later documentation slice explicitly creates it.

After implementation, `docs/README.md` material should include:

- WSL command requirement on Windows
- build and test commands
- Spring Boot start command
- profile names and defaults
- gRPC enabled flag and port
- REST enabled flag, host and port if still available
- workspace base path behavior
- Docker startup if implemented
- explicit note that plugins send data to the server and remain Spring-free

## Workplan Lifecycle

This workplan remains planning material. When implementation is completed, either:

- update this workplan with execution status and quality log, or
- replace `docs/workplan` with the next active workplan according to the repository workflow-authoring rule.

Do not leave stale completed plans mixed with active future plans.
