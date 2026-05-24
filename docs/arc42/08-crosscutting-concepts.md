# 8. Crosscutting Concepts

## 8.1 Canonical IDs

The platform uses stable IDs to correlate static facts, semantic facts, Byteman rules and runtime events.

Important IDs:

- `workspaceId`
- `projectId`
- `moduleId`
- `sourceFileId`
- `classKey`
- `methodKey`
- `callsiteKey`
- `branchKey`
- `ruleId`
- `analysisRunId`
- `runtimeSessionId`
- `correlationId`
- `traceId`
- `spanId`
- `parentSpanId`
- `incidentId`

## 8.2 Workspace and Project Boundary

Workspace-enabled features use `workspaceId` as the top-level organizational and access boundary. A project belongs to exactly one workspace, and project-scoped domain objects carry workspace context explicitly so later application, adapter and storage slices can reject cross-workspace access before reading or writing evidence.

Workspace lifecycle operations are modeled through application use cases. Creating a workspace also creates the owner membership, workspace reads require membership, owner/admin roles may update or archive an active workspace, and archived workspaces are read-only. Workspace lifecycle changes publish audit events.

Workspace member management is modeled separately from workspace lifecycle. Owner/admin roles may add members, change member roles and remove members from active workspaces. Member listing requires workspace membership. Member mutations publish audit events that include the affected user and role metadata.

Project lifecycle operations require an explicit workspace context. Owner/admin workspace roles may create, update or archive projects in active workspaces. Project mutations publish audit events and reject project IDs that do not belong to the requested workspace.

Project access is explicit in addition to workspace membership. Owner/admin workspace roles can read all projects and manage project members. Other workspace members can read only projects where a project membership exists. Project member changes require the affected user to already be a member of the workspace and create audit events for add, role change and removal.

Project storage paths are resolved server-side from workspace and project identifiers. Concrete storage adapters reject path traversal, nested client-supplied paths and unsafe identifier segments before resolving files under `workspaces/{workspaceId}/projects/{projectId}/{area}`. Project storage areas are explicit: original evidence, processed evidence, analysis results, reports and logs.

Audit events are append-only application facts for sensitive workspace and project actions. Workspace audit logs are readable by owner, admin and auditor roles. Audit queries return deterministic ordering by event time, action, target type and target id.

Assets preserve explicit ownership scope, stored file name, checksum and byte size. Shared assets belong to a workspace without a project id. Project assets belong to a project inside the same workspace and are listed separately from shared workspace assets.

Retention policy is workspace-scoped metadata. Owner/admin roles can configure retention while a workspace is active. Archived workspaces remain read-only; retention changes create audit events rather than deleting stored evidence.

The workspace canvas is an application-layer projection for UI adapters. It returns the actor role, visible projects, shared assets and action flags derived from the same server-side permission rules used by write use cases. It does not grant access by itself and does not replace backend authorization checks.

Security policy checks are available as application services for inbound adapters. They verify workspace existence, membership, role permissions, project assignment and project-to-workspace ownership before adapters delegate into use cases. Project object-scope failures are reported as not found in the requested workspace to avoid cross-workspace leakage.

## 8.3 Runtime Data Sensitivity

Runtime data must be treated as sensitive by default.

Supported mechanisms:

- Allowlisting
- Redaction
- Hashing
- Masking
- Length limits
- Sampling
- Retention
- Encryption
- Auditing

## 8.4 Evidence-Based LLM Usage

LLM analysis must be based on curated evidence packages. The LLM must not invent missing facts. If evidence is insufficient, the diagnosis must state the limitation.

## 8.5 Graph and Vector Projections

Graph DB and Vector DB are projections from the canonical analysis model. They are optimized views, not the source of truth.

## 8.6 Ambiguity Handling

Ambiguous mappings between JavaParser, Joern, Byteman rules and runtime events must be marked with confidence levels. Unclear mappings must not be silently accepted.

Source snapshots for cross-service analysis are commit-pinned. A moving branch
name is resolved once by `repository-source-service` and later branch movement
creates a new snapshot instead of mutating existing analysis input.

Complete build-output packages are explicit artifacts, not inferred runtime
facts. Artifact resolution tries a verified Artifact Store/Artifactory
reference first, optional Jenkins second, and a sandboxed build-artifact worker
fallback only when the earlier options are absent. Manifest or checksum
mismatch is an integrity failure and must not trigger fallback.

Joern materialization creates a Joern-owned workspace from validated
source/build packages. It rejects private Repository Source workspace IDs,
absolute paths, `file:` URIs, traversal, symlinks, hardlinks, device files,
duplicate normalized paths and quota overruns before Docker mounting.

FA-MSA-001 requires JavaParser source-fact artifacts to carry valid owner and
retrieval information before another service consumes them for planning. If
`repository-source-service` cannot provide available and complete source/build
package descriptors, Joern must be skipped with explicit incomplete diagnostics
instead of receiving invalid package metadata. Public query/report diagnostics
must be allow-listed or redacted before downstream messages cross the external
API boundary.

Source-fact retrieval must resolve to a verified `java-parser-analysis-service`
owner API or documented artifact contract before bytes are consumed by another
service. Repository Source to JavaParser handoff completion must be represented
through a reviewed service contract, and deterministic local fixtures must
avoid external Git network access, Docker, Jenkins, Artifactory, credentials,
private workspace paths and raw source content by default.

The JavaParser source-fact JSON payload must be an explicit external contract
before another service turns source facts into target-planning input. Parsing
belongs in that service's adapter boundary, not in shared Java DTOs or
JavaParser implementation imports. JavaParser, Joern and optional artifact
filesystem adapters must use no-follow symlink checks for directory segments
and files before reading, writing or accepting existing artifact bytes. Published
artifact byte references are immutable: repeating a write for identical bytes is
idempotent, while different bytes for an existing reference are rejected instead
of replacing prior evidence.

## 8.7 Replay Uncertainty

The replay must explicitly show missing, incomplete or uncertain event chains.

## 8.8 Operational Observability

Operational observability is scoped to non-core operational boundaries. Under
FA-MSA-001, `observability-stack` owns deployment and configuration material
for logs, metrics, tracing and dashboards. Productive services may have
service-local diagnostics, but they must not depend on a shared Java
observability module.

S10 verifies `services/observability-stack` and
`deployment/observability/service-diagnostics-policy.yaml` as target
observability-stack evidence. The stack is deployment-oriented policy material,
not a productive backend service and not a shared Java runtime dependency.
Docker Compose, Swarm and Kubernetes observability readiness remains
unclaimed until concrete runtime descriptors and validation commands exist.

Logs are diagnostics, not verified forensic evidence.

Logging must avoid raw payloads, source content, method arguments, method return values, credentials, local paths, stack frames and LLM prompt content. Failure logs use exception categories instead of raw exception messages.

ADR-0008 describes the historical monolith logging-module exception. ADR-0022
retired that source tree for active implementation. FA-MSA-001 productive
services must use service-local diagnostics or `observability-stack`
deployment material rather than shared Java logging modules.

Prior workflow evidence recorded that this retirement was not executable while
callers and regression coverage depended on
`forensic-analytics-logging` and `forensic-analytics-observability`. The active
final-retirement workflow supersedes that state with service-only project-model
checks, S05 deletion, S06 architecture closure and S07 release readiness.

S10 does not certify end-to-end ingestion upload correlation preservation. The
current ingestion upload contract has no verified correlation carrier, so that
gap remains owned by a contract-authorized ingestion refinement slice.

Automatic method logging records method operation names, phases, durations, correlation IDs and exception categories only. It must not record arguments, return values, raw exception messages, stack frames or evidence payloads.

## 8.9 Spring Boot Boundary

Spring Boot is a server bootstrap and adapter wiring concern. It may configure outer modules and lifecycle adapters, but it must not become a domain or application dependency.

The accepted Boot boundary preserves ADR-0005. Spring-specific method logging, MDC propagation, SLF4J bindings, AspectJ weaving and concrete logging providers require a separate architecture decision before they can be introduced. Boot-scoped REST behavior in ADR-0007 was historical predecessor behavior: it wrapped the former JDK REST adapter instead of adding Spring MVC or WebFlux. Current public API behavior is service-local where explicitly verified.

Earlier service-local architecture tests verify this boundary. Productive
services may depend on Spring only from their `bootstrap..` packages, while
`cli-client` must not depend on Spring at all. The former broad
`services:testbed` Spring and logging checks are retired from the
legacy-dependent testbed classpath after those service-local replacements are
present and passing.

## 8.10 Engineering Governance and Documentation Synchronization

Repository governance uses three process strands:

- `skills-agents`
- `workflow create`
- `workflow execute`

The strands must not be mixed. Shared roles such as Senior System Architect, Documentation Governance, Skill Registry Maintainer, Organigramm Maintainer, Process Governance Maintainer and `S1_PUSH_ELIGIBILITY_GUARD` execute inside the active strand and apply that strand's file scope, quality gate and documentation duty.

Documentation Governance is split into global and local nodes. `S1_DOC`,
`S2_DOC` and `S3_DOC` update concrete artifacts inside `skills-agents`,
`workflow create` and `workflow execute`. `DOCROOT` checks global consistency
for process documentation, role model, organigramm, arc42 structure, governance
rules, workflow conventions and hard boundaries.

`skills update` is the explicit entrypoint for `skills-agents`.

`workflow create` requires the Requirement Clarification Loop, the five-role Three Amigos Requirement Gate, checked `docs/workflow/workflow.md`, checked or updated arc42 documentation and explicit release for `workflow execute`.

Automatic governance feedback, correction and clarification loops are capped at `maxRetries = 3`; retry exhaustion stops the active strand and escalates to the Root Architect.

`workflow execute` requires checked workflow and arc42 artifacts before implementation and performs slice checkpoint commits and pushes after successful slice quality gates.

`workflow execute` quality-gate and validation failures are classified by the
Typed Error Router before retry or escalation. The router categories are
`ARCH_VIOLATION`, `BUILD_FAILURE`, `TEST_FAILURE`,
`DOC_GOVERNANCE_FAILURE`, `LOCK_CONFLICT` and `UNKNOWN_FAILURE`; unknown or
unowned failures escalate to the Root Architect instead of starting a generic
retry loop.

Governance Flowchart V2 extends ADR-0020 with S3 safety preflight, S3D
execution orchestration, one-slice-one-commit traceability, `CP_ROLLBACK`
rollback decisions, explicit publication terminals and two-level diagram
governance. The canonical diagrams live in `docs/governance/workflow/`.

`workflow execute` does not automatically jump backward to `workflow create`.
When workflow scope, dependencies or governance assumptions are wrong, the
allowed outcomes are STOP, report, Root Architect escalation and a manual
recommendation to refine the workflow.

Documentation synchronization must keep `AGENTS.md`, `QUALITY.md`, process docs, workflow docs, skill-audit docs, arc42 and ADR references consistent. Planned behavior is not implemented behavior.

## 8.11 Agent Governance

Agent Governance is a crosscutting engineering concept.

It applies to every non-trivial repository change and controls:

- command routing
- process strand selection
- requirement clarification
- architecture validation
- role and subagent assignment
- documentation synchronization
- quality gates
- commit and push behavior

The model prevents:

- mixed workflow responsibilities
- uncontrolled implementation by agents
- missing arc42 updates
- unverified requirements
- broad speculative changes
- accidental `push auto` on product implementation
- loss of work after local machine failures
