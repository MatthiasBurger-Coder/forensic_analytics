# 4. Solution Strategy

## 4.1 Strategy Overview

The Forensics Platform separates producer-side triggers from central
server-side analysis. Build plugins provide repository, branch, commit, build
and runtime-launch context. The central platform checks out repositories,
runs analysis, normalizes evidence, persists facts, correlates runtime data and
produces generated artifacts such as BTM files as the broader target strategy.
FA-MVP-0001 implements only the repository checkout workspace foundation of
that strategy.

## 4.2 Core Strategy

1. Use Gradle and Maven plugins only as request producers and runtime-binding adapters.
2. Normalize all inputs into a canonical analysis model.
3. Use stable IDs for classes, methods, callsites, branches, rules and runtime events.
4. Generate Byteman/BTM files server-side from an explicit instrumentation plan.
5. Let the plugin bind server-generated BTM files through the runtime agent when debugging requires instrumentation.
6. Collect runtime events with stable `ruleId` and `methodKey` references.
7. Build incidents from exception events.
8. Reconstruct replay timelines by correlation ID, trace ID, thread ID and sequence.
9. Build graph and vector projections from the canonical model.
10. Provide curated evidence packages to the LLM.
11. Keep repair automation gated by tests, quality gates and review.

## 4.3 Broader MVP Strategy

The broader product MVP focuses on read-only analysis:

- Static fact import
- Canonical model persistence
- Joern result import or attachment
- Server-side Byteman/BTM generation with stable rule IDs
- JSONL runtime event import
- Exception incident creation
- CorrelationID-based replay
- Simple graph projection
- LLM root-cause explanation without code modification

FA-MVP-0001 is the first foundation slice for that broader strategy. It
implements repository metadata preview, idempotent repository checkout
workspace and branch creation, repository-source PostgreSQL metadata
persistence under ADR-0024, branch refresh, sanitized public REST DTOs and a
Create Workspace UI flow. H2 is retained only for deterministic
repository-source adapter tests and direct fixtures. The MVP does not implement
JavaParser execution, Joern execution, BTM generation, replay, reports, graph
projections, vector storage, LLM context generation or broader analytics
database selection.

## 4.4 Non-MVP Scope

The following items are explicitly postponed:

- Automated patch generation
- Automated pull request creation
- Automated staging or production deployment
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers

## 4.5 Repository Governance Strategy

Repository agent governance follows three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

`skills update` is the explicit entrypoint for the `skills-agents` strand. It updates skills, agents, roles, prompts, Codex agent definitions, routing rules, organigramm, skill registry and process documentation. It does not implement product behavior.

`workflow create` sharpens requirements through the Requirement Clarification Loop and the five-role Three Amigos Requirement Gate. It ends with checked `docs/workflow/workflow.md`, checked or updated arc42 documentation, Documentation Governance and explicit release for `workflow execute`.

Automatic governance feedback, correction and clarification loops are bounded by `maxRetries = 3`. After retry exhaustion, the active strand stops and escalates to the Root Architect.

`workflow execute` executes the checked workflow slice by slice. Each successful slice must run its quality gate, create a slice-scoped checkpoint commit and push the current workflow branch to `origin`.

Publication modes stay separate: slice checkpoint push, `push` and `push auto` are different processes. `push auto` belongs only to `skills-agents`.

## 4.6 Agent-Governed Engineering Strategy

The repository uses agent-governed engineering to prevent uncontrolled changes, mixed responsibilities and architecture drift.

The strategy separates:

- `skills update` for maintaining the virtual development team,
- `workflow create` for requirement clarification and architecture-aware workflow planning,
- `workflow execute` for controlled slice implementation.

This separation ensures that requirements are clarified before implementation, workflow artifacts are checked before execution and every implementation slice is validated, documented, committed and pushed as a recoverable checkpoint.

The Senior System Architect owns the top-level governance boundary. Documentation Governance is mandatory in every strand but does not create a separate fourth strand.

## 4.7 Target Service Placement Strategy

Architecture entries are placed by arc42 responsibility, not by source-document
order. `docs/architecture/**` remains source-map evidence, while
`docs/arc42/**` holds the authoritative arc42 extracts.

The FA-MSA-001 service landscape is the target microservice direction for
repository source, ingestion, JavaParser analysis, Joern analysis,
orchestration, query/report API, CLI, observability and testbed boundaries.
Those target names are migration and architecture evidence, not production
readiness evidence. Production readiness still requires verified independent
build, start, configuration, health, container and deployment evidence per
service.

The migration strategy is strangler-first: preserve predecessor and historical
source-tree evidence as provenance, move behavior into service-owned domain,
application, adapter and bootstrap boundaries, and route integration through
contract-first REST/OpenAPI, gRPC/protobuf, approved messaging or documented
file contracts. Shared Java implementation modules, shared domain modules,
shared DTO modules and direct cross-service persistence coupling remain
forbidden.

The ingestion strategy uses a service-local gRPC/API intake boundary. Worker
and job coordination belong to orchestration-owned lifecycle, lease, attempt,
retry, timeout, failure, dead-letter and status concepts where these concepts
are verified by the service-boundary documentation. Concrete contract fields,
endpoint names and compatibility semantics remain contract-governance topics
and must not be inferred from this strategy section.

`SCA` is not recorded as a strategy here because the term has no verified
repository expansion in the placement assessment. If a verified source later
defines it as a migration concept, place the concept under chapter 8 and add a
glossary definition if the term remains project terminology.
