# ADR Inventory - 2026-06-04

## Purpose

This inventory records the verified Architecture Decision Records that existed
under `docs/adr/` during the ADR Baseline Consolidation workflow.

The inventory is a source index. It does not rewrite ADR history, renumber
ADRs, supersede ADRs, or decide conflicts. Conflict interpretation belongs to
the follow-up conflict-analysis slice.

## Verification Scope

Inspected sources:

- `docs/adr/README.md`
- `docs/adr/ADR-0001-plugins-are-producers.md`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`
- `docs/adr/ADR-0005-adapter-logging-observability-boundary.md`
- `docs/adr/ADR-0006-spring-boot-server-boundary.md`
- `docs/adr/ADR-0007-rest-api-spring-strategy.md`
- `docs/adr/ADR-0008-cross-cutting-logging-module.md`
- `docs/adr/ADR-0009-no-shared-common-modules.md`
- `docs/adr/ADR-0010-contract-first-rest-and-grpc.md`
- `docs/adr/ADR-0011-three-amigos-before-workflow.md`
- `docs/adr/ADR-0012-quality-gates-before-commit.md`
- `docs/adr/ADR-0013-data-ownership-per-service.md`
- `docs/adr/ADR-0014-agent-handoff-protocol.md`
- `docs/adr/ADR-0015-skill-registry-conflict-auditing.md`
- `docs/adr/ADR-0016-branch-first-workflow-creation.md`
- `docs/adr/ADR-0017-target-microservices-service-landscape.md`
- `docs/adr/ADR-0018-initial-logical-contracts.md`
- `docs/adr/ADR-0019-spring-boot-service-bootstrap-boundary.md`
- `docs/adr/ADR-0020-agent-governance-process-strands.md`
- `docs/adr/ADR-0021-governance-flowchart-v2.md`
- `docs/adr/ADR-0022-final-modular-monolith-source-tree-retirement.md`
- `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md`
- `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md`

## Inventory Method

- `Status` records the explicit `## Status` text found in the ADR file.
- `Inventory note` records only direct source signals from the ADR text.
- `Conflict action` is limited to routing guidance for later workflow slices.

## Records

| ADR | Title | Status | Inventory note | Conflict action |
|---|---|---|---|---|
| ADR-0001 | Plugins trigger server-side analysis, not the platform | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0002 | Use a canonical analysis model | Accepted | Storage projections remain replaceable. | Carry into S03 conflict analysis. |
| ADR-0003 | Runtime events are sensitive by default | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0004 | Graph DB and Vector DB are projections | Accepted | Storage technology remains replaceable. | Carry into S03 conflict analysis. |
| ADR-0005 | Adapter logging uses a JDK observability boundary | Accepted. Partially extended by ADR-0008 for the separate cross-cutting predecessor logging module; ADR-0022/S05 later retired predecessor logging and observability source trees as active implementation source. | Historical context remains for the logging boundary and diagnostics-as-not-evidence rule. | Carry active consequences and historical source-tree notes into S03 conflict analysis. |
| ADR-0006 | Spring Boot owns the outer server boundary | Accepted | ADR-0022 supersedes retained predecessor source-tree assumptions after S05 deletion. | Carry active Spring boundary and historical predecessor notes into S03 conflict analysis. |
| ADR-0007 | REST API strategy under Spring Boot | Accepted | Current public API ownership is service-local where explicitly verified; predecessor REST source-tree ownership is historical after ADR-0022/S05. | Carry into S03 conflict analysis. |
| ADR-0008 | Cross-cutting logging module with Spring method interception | Accepted | ADR text marks predecessor monolith logging source as historical after source-tree retirement. | Carry into S03 conflict analysis. |
| ADR-0009 | Do not share Java implementation modules between services | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0010 | Use contract-first REST and gRPC communication | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0011 | Run Three Amigos requirement gate before workflow authoring | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0012 | Require quality gates before commit and push | Accepted | Documentation slices may use `git diff --check`, but it does not replace required Gradle gates when those apply. | Carry into S03 conflict analysis. |
| ADR-0013 | Assign data ownership per service | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0014 | Use an explicit agent handoff protocol | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0015 | Use skill registry and conflict auditing | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0016 | Create Workflow Branches Before Workflow Artifacts | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0017 | Use the FA-MSA-001 target microservices service landscape | Accepted | ADR-0022 closes the conditional central source-tree retirement path; target service landscape and service-autonomy rules remain active. | Carry target service landscape and historical predecessor-name table into S03 conflict analysis. |
| ADR-0018 | Author initial logical service contracts before implementation | Accepted | gRPC v1 ingestion field-number preservation is explicitly mentioned. | Carry into S03 conflict analysis. |
| ADR-0019 | Allow Spring Boot at service bootstrap boundaries | Accepted | No supersession signal found. | Carry into S03 conflict analysis. |
| ADR-0020 | Agent Governance Process Strands | Accepted | Defines `skills update`, `workflow create`, and `workflow execute` process strands. | Carry into S03 conflict analysis. |
| ADR-0021 | Governance Flowchart V2 | Accepted | Defines S3 STOP paths, typed error routing, retry limits and publication terminals. | Carry into S03 conflict analysis. |
| ADR-0022 | Retire legacy modular-monolith source trees | Accepted | Retires tracked legacy modular-monolith source trees as implementation source and preserves only historical/provenance usage. | Carry into S03 conflict analysis. |
| ADR-0023 | Use H2 only for repository-source MVP persistence | Accepted for tests only. Superseded for runtime by ADR-0024. | H2 is not runtime or Docker fallback after ADR-0024 and workflow baseline; H2 remains deterministic adapter test and fixture scope. | Carry into S03 conflict analysis with ADR-0024 runtime supersession. |
| ADR-0024 | Use PostgreSQL for repository-source workspace metadata | Accepted | PostgreSQL is service-owned metadata storage for repository-source workspace state; Liquibase owns schema creation and evolution for this repository-source schema. | Carry into S03 conflict analysis with ADR-0023 test-only boundary. |

## Numbering Check

The verified ADR range is:

```text
ADR-0001 through ADR-0024
```

No gap was found in the numeric sequence.

The next candidate ADR number, if a later reviewed architecture decision is
approved, is:

```text
ADR-0025
```

This inventory does not allocate `ADR-0025`.

## Open Follow-Up For S03

S03 must analyze at least these verified source signals:

- predecessor module and source-tree wording retained in older ADRs;
- ADR-0022 retirement of legacy modular-monolith source trees;
- ADR-0017 target-service landscape versus predecessor/current-state names;
- ADR-0023 test-only H2 scope and ADR-0024 PostgreSQL runtime scope;
- flat current arc42 files versus this workflow's target arc42 chapter
  subdirectories.
