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
| ADR-0007 | REST API strategy under Spring Boot | Accepted | Historical predecessor REST/Boot lifecycle decision; current public API ownership is service-local where explicitly verified |
| ADR-0008 | Cross-cutting logging module with Spring method interception | Accepted | Historical predecessor logging-module exception; after ADR-0022/S05 it must not authorize a shared Java logging module for services |
| ADR-0009 | Do not share Java implementation modules between services | Accepted | Preserves service autonomy and prevents shared-code coupling in future service-split work |
| ADR-0010 | Use contract-first REST and gRPC communication | Accepted | Requires explicit REST/gRPC contracts, compatibility review and tests before service communication changes |
| ADR-0011 | Run Three Amigos requirement gate before workflow authoring | Accepted | Keeps new requirements validated before executable workflow generation |
| ADR-0012 | Require quality gates before commit and push | Accepted | Keeps commit and push readiness tied to verified local quality evidence |
| ADR-0013 | Assign data ownership per service | Accepted | Requires one owner and one write path for persistent data and blocks cross-service database coupling |
| ADR-0014 | Use an explicit agent handoff protocol | Accepted | Keeps delegated workflow ownership, blockers and validation evidence explicit |
| ADR-0015 | Use skill registry and conflict auditing | Accepted | Prevents hidden role, skill and governance conflicts during workflow work |
| ADR-0016 | Create workflow branches before workflow artifacts | Accepted | Keeps workflow creation isolated from shared branches before mutating workflow files |
| ADR-0017 | Use the FA-MSA-001 target microservices service landscape | Accepted | Aligns the active workflow with repository-source, ingestion, JavaParser, Joern, orchestration, query/report API, CLI, observability and testbed boundaries |
| ADR-0018 | Author initial logical service contracts before implementation | Accepted | Allows planned REST, gRPC and event contracts before implementation while keeping planned design distinct from verified runtime evidence |
| ADR-0019 | Allow Spring Boot at service bootstrap boundaries | Accepted | Permits independent service Spring Boot entrypoints while preserving framework-free service domain and application code |
| ADR-0020 | Agent Governance Process Strands | Accepted | Defines `skills-agents`, `workflow create`, `workflow execute`, slice checkpoint push, `push` and `push auto` publication separation |
| ADR-0021 | Governance Flowchart V2 | Accepted | Adds S3 STOP paths, Typed Error Router, S3D orchestration, rollback, one-slice-one-commit traceability and two-level flowcharts |
| ADR-0022 | Retire legacy modular-monolith source trees | Accepted | Closes final source-tree retirement after S05 verified service-only Gradle topology and no active legacy build/source dependencies |
| ADR-0023 | Use H2 only for repository-source MVP persistence | Accepted | Records H2 as a repository-source-owned Docker-local MVP adapter only and keeps the production database decision open |
| ADR-0024 | Use PostgreSQL for repository-source workspace metadata | Accepted | Selects PostgreSQL only for repository-source-owned workspace metadata and keeps broader Analytics persistence decisions open |

## 9.2 FA-MVP-0001 ADR Review

S11 reviewed the repository checkout workspace MVP against the active ADR set.
ADR-0023 records the H2 adapter scope because the workflow introduced durable
service-local persistence for repository-source MVP state. ADR-0023 does not
select a production relational database, does not create shared analytics
persistence and does not close OD-001.

The architecture consequence is: `repository-source-service` owns repository
checkout workspace, branch, source snapshot and idempotency state;
`query-report-api-service` exposes sanitized public workspace REST DTOs
through owner APIs; no new `workspace-service` is introduced.

ADR-0024 supersedes the H2 runtime target for repository-source workspace
metadata after the PostgreSQL cutover. The bounded decision covers only
repository checkout workspace, branch, repository preparation and idempotency
records owned by `repository-source-service`. It does not authorize direct
cross-service table access, does not store checkout bytes in PostgreSQL and
does not select a shared canonical Analytics database.

## 9.3 Open Decisions

| ID | Open Decision | Notes |
|---|---|---|
| OD-001 | Initial relational database | Partially decided by ADR-0024 only for repository-source workspace metadata. Broader canonical Analytics persistence remains open. |
| OD-002 | Initial Graph DB | Not selected in EPIC v0.2 |
| OD-003 | Initial Vector DB | Not selected in EPIC v0.2 |
| OD-004 | Runtime ingestion mode | JSONL likely for MVP, HTTP collector later |
| OD-005 | Runtime value storage policy | Needs redaction rule model |
| OD-006 | Initial LLM provider | Must remain replaceable |
| OD-007 | Source-code loading and versioning in UI | Needs later design |
| OD-008 | Multi-repo and multi-service trace model | Needs later design |
