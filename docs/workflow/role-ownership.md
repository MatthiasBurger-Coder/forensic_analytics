# Role Ownership: FA-MVP-0001

| Slice | Primary Owner | Required Reviewers |
|---|---|---|
| S00 Workflow Execution Preflight And Context Freeze | Senior Workflow Architect | Requirement Engineer, System Architect, Tester |
| S01 Requirement Terminology And Data Ownership Gate | Senior Requirement Engineer | System Architect, Data Ownership, Storage, Microservice Expert, Tester |
| S02 Contract-First Workspace API And Owner API | Contract Governance Expert | gRPC/Proto, Java Backend, React Frontend, Tester, Security |
| S03 Repository Source Workspace Domain And In-Memory Use Cases | Senior Java Backend | System Architect, Git Workspace, Tester, Security |
| S04 Repository Metadata Resolution And Branch Checkout Refresh | Senior Git Workspace Specialist | Java Backend, Security, Resilience, Tester |
| S05 H2 Dependency, Schema And Persistence Adapters | Senior Analysis Storage Architect | Data Ownership, Java Backend, DevOps, Security, Tester |
| S06 Repository Source gRPC Endpoint And Error Mapping | Senior gRPC/Proto Specialist | Java Backend, Contract Governance, Tester, Observability |
| S07 Query Report Public Workspace REST Facade | Senior Java Backend | Contract Governance, gRPC/Proto, Security, Frontend, Tester |
| S08 Forensic UI Create Workspace Flow | Senior React Frontend | UX Designer, Contract Governance, Security, Tester |
| S09 Docker Local Volumes And Runtime Configuration | Senior DevOps | Storage, Git Workspace, Security, Runtime Readiness, Tester |
| S10 Security, Leakage, Idempotency And Restart Integration Gate | Senior Tester | Security, Resilience, System Architect, Backend, Frontend, DevOps |
| S11 Documentation, arc42 And ADR Closure | Senior Documentation Engineer | System Architect, ADR Steward, Documentation Sync, DevOps, Tester |
| S12 Final Quality Gate And Workflow Handoff | Quality Gate Orchestrator | DevOps, Tester, System Architect, Git Commit Reviewer |

## Subagent Use

The user explicitly requested subagents. During `workflow execute`, callable
subagents may be used for role reviews and bounded implementation slices when
the write scope is disjoint and the slice metadata authorizes the owner.

Subagents must:

- verify the active branch before edits;
- stay inside the slice file locks;
- avoid branch switching;
- avoid reverting unrelated user or agent changes;
- return changed paths, verification commands and blockers.
