# 2. Architecture Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java 25 | The platform baseline is Java 25. |
| Gradle 9.4.0 | Gradle integration must be compatible with Gradle 9.4.0. |
| JUnit 6 | Automated tests use the JUnit 6 baseline. |
| Maven support | Maven must be supported as a separate plugin adapter. |
| Hexagonal Architecture | Core domain logic must be independent from frameworks and external tools. |
| Plugins as adapters | Gradle and Maven plugins are producers of facts, not the central platform. |
| Spring Boot server boundary | Spring Boot is approved only as an outer server/bootstrap technology. Domain and application modules remain Spring-free. |
| Microservice autonomy | Future service-split work must keep services independently buildable, runnable, testable, containerized and deployable without shared Java code modules. Service integration is limited to REST/OpenAPI, gRPC/protobuf and RabbitMQ/message contracts. |
| Joern as adapter | Joern integration must be encapsulated behind a port. |
| Byteman integration | BTM files are generated server-side from the analysis model and runtime planning, then bound by the plugin through the runtime agent when debugging requires instrumentation. |
| Three process strands | Repository agent work is constrained to `skills-agents`, `workflow create` and `workflow execute`. The strands must not be mixed. |
| skills update command | Exact `skills update` activates only the `skills-agents` strand for skills, agents, roles, prompts, routing rules, organigramm, skill registry and process documentation. |
| workflow create end state | `workflow create` ends only after no blocking questions remain, `docs/workflow/workflow.md` is checked, arc42 is checked or updated, Documentation Governance passes and workflow execute is explicitly released. |
| bounded governance loops | Automatic governance feedback, correction and clarification loops are capped at `maxRetries = 3`; retry exhaustion stops and escalates to the Root Architect. |
| workflow execute checkpoints | `workflow execute` commits and pushes a slice-scoped checkpoint after each successful slice quality gate. |
| push auto restriction | `push auto` is restricted to `skills-agents` and must not publish product implementation, services, contracts, Docker/runtime, build logic, frontend or analytics implementation files. |

## 2.2 Product Constraints

- The MVP does not include autonomous code changes.
- The MVP does not include automatic pull request creation.
- The MVP does not include production deployment automation.
- The first platform version focuses on JSONL runtime event import.
- Graph DB and Vector DB products are not finally selected.

## 2.3 Security Constraints

- Runtime values must be treated as sensitive by default.
- Redaction must happen before persistence into graph or vector projections.
- Secrets must not be indexed in a Vector DB.
- Runtime data access must be auditable.

## 2.4 Architectural Guardrails

- Canonical model first.
- Graph DB and Vector DB are projections, not the source of truth.
- Ambiguous mappings must be reported, not silently accepted.
- LLM output must be evidence-based.
- Automated repair must be gated by tests, quality gates and human review.
- Spring Boot wiring must stay outside the forensic core and must not weaken the observability boundary from ADR-0005.
- Future microservices must not share Java code modules, domain models, event classes or test fixtures.
- Planned governance behavior must not be described as product runtime behavior.
- Slice checkpoint push must not be confused with `push auto`.

## 2.5 Agent And Workflow Governance Constraints

Forensic Analytics uses a governed agent workflow model with exactly three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

The strands must not be mixed.

`skills update` activates the `skills-agents` strand.

`workflow create` is responsible for requirement clarification, Three Amigos validation, checked workflow authoring and arc42 synchronization.

Automatic governance feedback, correction and clarification loops are capped at `maxRetries = 3`. Retry exhaustion stops the active strand and escalates to the Root Architect.

`workflow execute` is responsible for executing checked workflow slices through role or subagent routing, quality gates, documentation synchronization and slice checkpoint commits.

`push auto` is restricted to the `skills-agents` strand and must not publish backend, frontend, Docker/runtime, contract or analytics implementation changes.

Slice checkpoint push is part of `workflow execute` and is not `push auto`.
