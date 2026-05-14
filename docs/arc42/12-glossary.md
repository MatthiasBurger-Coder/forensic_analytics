# 12. Glossary

| Term | Definition |
|---|---|
| Forensics Platform | Central analysis platform for static facts, runtime events, replay and LLM diagnosis |
| Workspace | Top-level organizational and access boundary for workspace-enabled forensic analysis features |
| Project | Analysis scope that belongs to exactly one workspace |
| Workspace Role | Role assigned to a user inside one workspace, used for workspace-level permissions |
| Workspace Membership | Link between a user, workspace and workspace role |
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
