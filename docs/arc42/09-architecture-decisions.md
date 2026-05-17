# 9. Architecture Decisions

## 9.1 Accepted Decisions

| ID | Decision | Status | Rationale |
|---|---|---|---|
| AD-001 | Plugins are producers, not the platform | Accepted | Keeps build integration separate from central analysis |
| AD-002 | Use a canonical analysis model | Accepted | Enables correlation across JavaParser, Joern, Byteman and runtime events |
| AD-003 | Graph DB and Vector DB are projections | Accepted | Prevents storage-specific models from becoming the source of truth |
| AD-004 | Runtime data is sensitive by default | Accepted | Protects secrets, personal data and business-critical values |
| AD-005 | LLM diagnosis must be evidence-based | Accepted | Reduces hallucination risk and improves reviewability |
| AD-006 | Automated repair is gated | Accepted | Prevents unsafe autonomous changes |
| ADR-0005 | Adapter logging uses a JDK observability boundary | Accepted | Keeps operational logging framework-neutral and separate from forensic evidence |
| ADR-0006 | Spring Boot owns the outer server boundary | Accepted | Allows Boot only at the server bootstrap boundary while preserving framework-free core modules |
| ADR-0007 | REST API strategy under Spring Boot | Accepted | Keeps the existing JDK REST adapter behind Boot lifecycle wiring and defers Spring MVC/WebFlux |
| ADR-0008 | Cross-cutting logging module with Spring method interception | Accepted | Allows a named logging-module exception for Boot-scoped method logging while keeping observability framework-neutral |
| ADR-0009 | Do not share Java implementation modules between services | Accepted | Preserves service autonomy and prevents shared-code coupling in future service-split work |
| ADR-0010 | Use contract-first REST and gRPC communication | Accepted | Requires explicit REST/gRPC contracts, compatibility review and tests before service communication changes |
| ADR-0011 | Run Three Amigos requirement gate before workflow authoring | Accepted | Keeps new requirements validated before executable workflow generation |
| ADR-0012 | Require quality gates before commit and push | Accepted | Keeps commit and push readiness tied to verified local quality evidence |
| ADR-0013 | Assign data ownership per service | Accepted | Requires one owner and one write path for persistent data and blocks cross-service database coupling |
| ADR-0014 | Use an explicit agent handoff protocol | Accepted | Keeps delegated workflow ownership, blockers and validation evidence explicit |
| ADR-0015 | Use skill registry and conflict auditing | Accepted | Prevents hidden role, skill and governance conflicts during workflow work |
| ADR-0016 | Create workflow branches before workflow artifacts | Accepted | Keeps workflow creation isolated from shared branches before mutating workflow files |
| ADR-0016-A | Three-strand workflow and agent governance extension | Accepted | Extends branch-first workflow governance with `skills update`, `workflow create`, `workflow execute`, slice checkpoint push and `push auto` limited to `skills-agents` |
| ADR-0017 | Use the target microservices service landscape | Accepted | Aligns the active workflow with the target Gateway, ingestion, analysis, storage, graph/replay, report and frontend service boundaries |
| ADR-0018 | Author initial logical service contracts before implementation | Accepted | Allows planned REST, gRPC and event contracts before implementation while keeping planned design distinct from verified runtime evidence |
| ADR-0019 | Allow Spring Boot at service bootstrap boundaries | Accepted | Permits independent service Spring Boot entrypoints while preserving framework-free service domain and application code |

## 9.2 Open Decisions

| ID | Open Decision | Notes |
|---|---|---|
| OD-001 | Initial relational database | Not selected in EPIC v0.1 |
| OD-002 | Initial Graph DB | Not selected in EPIC v0.1 |
| OD-003 | Initial Vector DB | Not selected in EPIC v0.1 |
| OD-004 | Runtime ingestion mode | JSONL likely for MVP, HTTP collector later |
| OD-005 | Runtime value storage policy | Needs redaction rule model |
| OD-006 | Initial LLM provider | Must remain replaceable |
| OD-007 | Source-code loading and versioning in UI | Needs later design |
| OD-008 | Multi-repo and multi-service trace model | Needs later design |
