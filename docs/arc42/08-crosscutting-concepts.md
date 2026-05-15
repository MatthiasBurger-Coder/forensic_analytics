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

## 8.7 Replay Uncertainty

The replay must explicitly show missing, incomplete or uncertain event chains.

## 8.8 Operational Observability

Operational observability is scoped to non-core operational boundaries. REST, gRPC, CLI, bootstrap, source adapters, Joern Docker execution, engine entrypoints, engine-request import and persistence write operations create sanitized operation logs through `forensic-analytics-observability` where useful.

The observability boundary uses JDK logging only. It does not introduce Spring AOP, AspectJ, SLF4J or concrete logging providers. Logs are diagnostics, not verified forensic evidence.

Logging must avoid raw payloads, source content, method arguments, method return values, credentials, local paths, stack frames and LLM prompt content. Failure logs use exception categories instead of raw exception messages.
