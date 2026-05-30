# 12. Glossary

| Term | Definition |
|---|---|
| Forensics Platform | Central analysis platform for static facts, runtime events, replay and LLM diagnosis |
| Platform Workspace | Top-level organizational and access boundary for workspace-enabled forensic analysis features |
| Repository Checkout Workspace | Repository-source-owned checkout aggregate for one normalized repository identity in FA-MVP-0001; not a membership, authorization, project lifecycle, asset, audit or retention boundary |
| Project | Analysis scope that belongs to exactly one platform workspace |
| Workspace Role | Role assigned to a user inside one platform workspace, used for workspace-level permissions |
| Workspace Membership | Link between a user, platform workspace and workspace role |
| Project Membership | Assignment allowing a workspace member to access one specific project |
| Asset Scope | Domain boundary that distinguishes shared workspace assets from project-scoped assets |
| Shared Asset | Asset owned by a workspace without a project id |
| Project Asset | Asset owned by a project inside a workspace |
| Retention Policy | Workspace-scoped retention duration metadata |
| Workspace Canvas | UI-ready application projection of workspace state and role-derived action flags |
| Audit Event | Append-only record of a sensitive workspace, project, member, asset or retention action |
| Static Fact | Persisted information derived from source code, build context or AST analysis |
| Runtime Event | Event emitted by the instrumented runtime application |
| RuleID | Stable identifier of a server-generated BTM rule |
| MethodKey | Stable identifier for a method across analysis and runtime data |
| CorrelationID | Identifier connecting runtime events belonging to the same business or technical flow |
| TraceID | Identifier for a distributed or local execution trace |
| SpanID | Identifier for a specific execution span |
| Incident | Error-centered analysis unit usually created from an exception event |
| Replay | Reconstructed timeline and call tree for an incident |
| Incident Context Package | Curated evidence package for LLM diagnosis |
| Joern | External semantic code analysis tool for CPG, data-flow and control-flow analysis |
| Graph Projection | Graph representation derived from the canonical model |
| Vector Projection | Semantic vector representation derived from selected canonical facts |
| Redaction | Removal or masking of sensitive runtime values |
| Repair Orchestrator | Future component for gated fix, test and PR preparation |
| Process Strand | One of the mutually exclusive repository governance flows: `skills-agents`, `workflow create` or `workflow execute` |
| skills update | Exact command that activates the `skills-agents` process strand |
| skills-agents | Process strand for skills, agents, roles, prompts, routing rules, organigramm, skill registry and process documentation |
| workflow create | Process strand that clarifies requirements, creates or sharpens workflow.md, checks arc42 and releases workflow execute |
| workflow execute | Process strand that executes checked workflow slices with tests, quality gates and slice checkpoint pushes |
| S3D | Workflow-execute Execution Orchestrator that extracts slice metadata, builds the dependency graph, runs topological sort and checks file, contract, module and architecture-boundary locks |
| S3_CLASSIFY | Workflow-execute slice classification node for backend, frontend, runtime/DevOps/contracts and declared documentation/governance/metadata slices |
| S3_UNCLASSIFIED | STOP node for slices that cannot be classified from the checked workflow |
| D8 | Synchronous blocking quality and release-readiness gate before commit, checkpoint push and release readiness |
| Q11 | Asynchronous execution report path after `CP_FINAL`; non-blocking by default unless explicitly promoted to D8 |
| CP_RECORD | Slice traceability record containing workflow version, slice ID, responsible agent, changed files, quality gates, result, commit hash, rollback reference and documentation update status |
| CP_ROLLBACK | Rollback and revert decision node for failed quality gates, failed checkpoint push or publication failure |
| PUB_PR_RESULT | Publication outcome for a PR that remains open without automatic merge |
| PUB_DONE | Publication completion terminal |
| PUB_PUSH_FAILED | Publication failure terminal that routes to rollback or escalation |
| PUB_REJECTED | Publication rejection terminal for scope, branch, governance or guard failures |
| Requirement Clarification Loop | Workflow-create loop for intent, requirements, assumptions, risks, questions, confidence and readiness decision |
| maxRetries | Maximum number of automatic governance clarification, feedback or correction attempts before STOP and Root Architect escalation; currently `maxRetries = 3` |
| Typed Error Router | Workflow-execute quality and validation failure router that classifies failures as `ARCH_VIOLATION`, `BUILD_FAILURE`, `TEST_FAILURE`, `DOC_GOVERNANCE_FAILURE`, `LOCK_CONFLICT` or `UNKNOWN_FAILURE` before retry or escalation |
| Blocking Questions | Questions that affect architecture boundaries, testability, data ownership, service boundaries, APIs, contracts, runtime behavior or scope |
| Slice checkpoint push | Workflow-execute publication step that commits only the completed slice and pushes the current workflow branch to origin |
| push auto | skills-agents-only guarded PR lifecycle that may merge and clean up after guard checks pass |

## 12.1 Agent Governance Terms

### skills update

Explicit command that activates the `skills-agents` strand.

### skills-agents

Process strand for maintaining skills, agents, roles, prompts, registries, organigramm and process documentation.

### workflow create

Process strand for requirement clarification, workflow authoring and arc42 synchronization.

### workflow execute

Process strand for executing checked workflow slices.

### Slice checkpoint push

Commit and push of a successfully completed slice to the current workflow branch.

### push auto

Guarded publication mode restricted to the `skills-agents` strand.

### Documentation Governance

Mandatory documentation synchronization inside every active process strand.

### DOCROOT

Global documentation-governance check for process documentation, role model,
organigramm, arc42 structure, governance rules, workflow conventions and hard
boundaries.

### S1_DOC

Local documentation node for `skills-agents`.

### S2_DOC

Local documentation node for `workflow create`.

### S3_DOC

Local documentation node for `workflow execute`.

### Governance Flowchart V2

Repository governance model accepted by ADR-0021. It adds S3 safety preflight,
Typed Error Router ownership, S3D orchestration, `CP_ROLLBACK`, explicit
publication terminals, one-slice-one-commit traceability and two-level
flowcharts.
