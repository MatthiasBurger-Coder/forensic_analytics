# Role Ownership

| Area | Primary owner | Supporting roles |
|---|---|---|
| Workflow execution | Workflow Executor | Senior Workflow Architect |
| Requirement integrity | Senior Requirement Engineer | Three Amigos Requirement Gatekeeper |
| Service boundaries | Senior System Architect | Microservice Senior Expert |
| Contract governance | Contract-First API Steward | Senior gRPC Proto Specialist, Contract Governance Expert |
| Repository checkout and workspaces | Senior Git Workspace Specialist | Security Sandbox Engineer |
| Build-output artifact production | Senior Java Backend Developer | Senior DevOps, Security Sandbox Engineer |
| Backend service implementation | Senior Java Backend Developer | Architecture and ArchUnit reviewers |
| Analysis Store and artifact ownership | Senior Analysis Storage Architect | Data Ownership and Persistence Steward |
| Joern CPG analysis | Senior Joern CPG Specialist | Joern Semantic Analysis reviewer |
| BTM generation | Senior Java Backend Developer | gRPC Proto Specialist, Quality reviewer |
| Gateway facade and orchestration owner API | Senior System Architect | Contract-First API Steward, Senior Java Backend Developer |
| Frontend/API integration | Senior React Frontend Developer | Senior UX Designer |
| Docker and deployment | Senior DevOps Engineer | Microservice Runtime Readiness Expert |
| Testing and quality gates | Senior Tester | Quality Gate Orchestrator |
| Documentation synchronization | Senior Documentation Engineer | arc42 Architecture Governance, ADR Steward |
| Slice checkpoint commit and push | git commit preparation | git commit message preparation |

## Role Rules

- Every write-capable role must verify the active branch before editing.
- No role may implement on `main`, `master`, `develop` or another shared
  branch.
- No role may introduce shared Java implementation modules between services.
- Contract-owning roles must complete contract review before implementation.
- Quality-owning roles must report exact commands, failures and residual risk.
- Every successful `workflow execute` slice must stage only current-slice
  files, run `git diff --cached --check`, create a slice-scoped checkpoint
  commit and push the active workflow branch before the next slice starts.

## Required Reviews By Slice

| Slice | Required reviews |
|---|---|
| 00 | Workflow Architect, git branch strategy |
| 01 | Senior System Architect, Microservice Senior Expert, Data Ownership Steward |
| 02 | Contract-First API Steward, Senior gRPC Proto Specialist, Senior Tester |
| 03 | Senior Analysis Storage Architect, Data Ownership Steward, Contract Governance |
| 04 | Senior Java Backend, Senior DevOps, Microservice Runtime Readiness |
| 05 | Senior Git Workspace Specialist, Security Sandbox Engineer, Senior Tester |
| 06 | Contract-First API Steward, Senior gRPC Proto Specialist, Senior Java Backend, Source Analysis reviewer, Quality ArchUnit reviewer |
| 07 | Senior System Architect, Contract-First API Steward, Senior gRPC Proto Specialist, Senior Analysis Storage Architect, Senior Security Sandbox Engineer, Senior DevOps, Senior Tester |
| 08 | Contract-First API Steward, Senior gRPC Proto Specialist, Senior Joern CPG Specialist, Joern Semantics reviewer, Senior Tester |
| 09 | Senior System Architect, Senior Java Backend, Evidence integrity review |
| 10 | Senior gRPC Proto Specialist, Senior Java Backend, BTM determinism tests |
| 11 | Senior System Architect, Contract-First API Steward, Senior gRPC Proto Specialist, Security Sandbox Engineer, Senior Java Backend, Senior Tester |
| 12 | Senior Swarm Orchestrator, Senior Java Backend, Senior Tester |
| 13 | Senior DevOps, Microservice Runtime Readiness, Security review |
| 14 | Senior System Architect, Replay/Graph/LLM reviewer |
| 15 | Senior React Frontend, Senior UX Designer, frontend test owner |
| 16 | Senior System Architect, Senior Java Backend, Release governance |
| 17 | Senior DevOps, Build Gradle, Architecture validation |
| 18 | Senior Tester, Quality Gate Orchestrator, Documentation reviewer |
