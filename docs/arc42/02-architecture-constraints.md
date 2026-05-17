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

## 2.5 Agent And Workflow Governance Constraints

Repository automation follows exactly three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

These process strands are repository governance, not Forensics Platform runtime
capabilities. They must not weaken the product constraints that exclude
autonomous code changes, automatic pull request creation and production
deployment automation from the MVP.

`skills-agents` is limited to skill, role, prompt, Codex agent, registry,
organigramm, process and governance documentation changes. `push auto` is
restricted to this strand and must block backend, frontend, Docker/runtime and
analytics implementation changes.

`workflow create` is limited to requirement, architecture, planning and
documentation work. It requires a dedicated branch before mutating workflow
planning artifacts and must end with checked `docs/workflow/workflow.md` plus
checked or updated arc42 documentation.

`workflow execute` may start only from checked `docs/workflow/workflow.md` and
checked or updated arc42 documentation. It must execute slices through the
configured role/subagent workflow and must return to `workflow create` when
scope, architecture boundaries or testability change.
