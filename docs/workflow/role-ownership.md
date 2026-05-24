# Role Ownership: FA-MVP-0001

| Slice | Primary Owner | Required Reviewers |
|---|---|---|
| S00 Workflow Execution Preflight And Context Freeze | Senior Workflow Architect | Requirement Engineer, System Architect, Tester |
| S01 Requirement Terminology And Data Ownership Gate | Senior Requirement Engineer | System Architect, Data Ownership, Storage, Microservice Expert, Tester |
| S02 Contract-First Workspace API And Owner API | Contract Governance Expert | gRPC/Proto, Java Backend, React Frontend, Tester, Senior Security/Sandbox Engineer |
| S03 Repository Source Workspace Domain And In-Memory Use Cases | Senior Java Backend | System Architect, Git Workspace, Tester, Senior Security/Sandbox Engineer |
| S04 Repository Metadata Resolution And Branch Checkout Refresh | Senior Git Workspace Specialist | Java Backend, Senior Security/Sandbox Engineer, Resilience, Tester |
| S05 H2 Dependency, Schema And Persistence Adapters | Senior Analysis Storage Architect | Data Ownership, Java Backend, DevOps, Senior Security/Sandbox Engineer, Tester |
| S06 Repository Source gRPC Endpoint And Error Mapping | Senior gRPC/Proto Specialist | Java Backend, Contract Governance, Tester, Observability |
| S07 Query Report Public Workspace REST Facade | Senior Java Backend | Contract Governance, gRPC/Proto, Senior Security/Sandbox Engineer, Frontend, Tester |
| S08 Forensic UI Create Workspace Flow | Senior React Frontend | UX Designer, Contract Governance, Senior Security/Sandbox Engineer, Tester |
| S09 Docker Local Volumes And Runtime Configuration | Senior DevOps | Storage, Git Workspace, Senior Security/Sandbox Engineer, Runtime Readiness, Tester |
| S10 Security, Leakage, Idempotency And Restart Integration Gate | Senior Tester | Senior Security/Sandbox Engineer, Resilience, System Architect, Backend, Frontend, DevOps |
| S11 Documentation, arc42 And ADR Closure | Senior Documentation Engineer | System Architect, ADR Steward, Documentation Sync, DevOps, Tester |
| S12 Final Quality Gate And Workflow Handoff | Quality Gate Orchestrator | DevOps, Tester, System Architect, `git_commit_reviewer` |

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

Reviewer identifiers in workflow metadata are resolved against `.agents/roles`,
`.agents/skills` and callable Codex agent definitions under `.codex/agents`.
`git_commit_reviewer` is verified by `.codex/agents/git_commit_reviewer.toml`.
