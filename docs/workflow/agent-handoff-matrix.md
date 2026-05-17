# Agent Handoff Matrix

Callable subagents may be used only when the active request explicitly
authorizes delegated or parallel agent work. Otherwise, the matching role file
or skill is used as a local review checklist.

Each role or subagent must verify the active branch before modifying files:

```text
architecture/microservices-ecosystem-conversion-20260516
```

| Slice | Owner | Required Reviews |
|---:|---|---|
| 00 | Senior System Architect | Microservice Senior Expert, Senior Java Backend Developer, Senior DevOps Engineer, Senior Tester |
| 01 | Senior System Architect | Three Amigos Requirement Gatekeeper, Microservice Senior Expert, Data Ownership And Persistence Steward, Senior Tester |
| 02 | Senior DevOps Engineer | Senior System Architect, Senior Java Backend Developer, Senior Tester |
| 03 | Senior gRPC/Proto Specialist | Contract Governance Expert, Senior System Architect, Microservice Senior Expert, Senior Tester |
| 04 | Senior Java Backend Developer | Senior gRPC/Proto Specialist, Microservice Senior Expert, Senior DevOps Engineer, Senior Tester |
| 05 | Senior Java Backend Developer | Data Ownership And Persistence Steward, Senior System Architect, Senior DevOps Engineer, Senior Tester |
| 06 | Senior Java Backend Developer | Microservice Senior Expert, Senior DevOps Engineer, Senior Git Workspace Specialist, Security Sandbox Specialist, Senior Tester |
| 07 | Senior Java Backend Developer | Source Analysis Pipeline, Microservice Senior Expert, Senior Tester |
| 08 | Senior Java Backend Developer | Senior Joern CPG Specialist, Senior DevOps Engineer, Microservice Senior Expert, Senior Tester |
| 09 | Senior Java Backend Developer | Microservice Senior Expert, Senior Tester |
| 10 | Senior Java Backend Developer | Senior System Architect, Replay/Graph/LLM Reviewer, Data Ownership And Persistence Steward, Senior Tester |
| 11 | Senior Java Backend Developer | Senior UX Designer, Replay/Graph/LLM Reviewer, Microservice Senior Expert, Senior Tester |
| 12 | Senior Java Backend Developer | Senior System Architect, Senior UX Designer, Contract Governance Expert, Senior Tester |
| 13 | Senior React Frontend Developer | Senior UX Designer, Senior Tester |
| 14 | Senior DevOps Engineer | Senior System Architect, Microservice Runtime Readiness Expert, Senior Tester |
| 15 | Senior DevOps Engineer | Senior System Architect, Microservice Runtime Readiness Expert, Senior Tester |
| 16 | Senior Tester | Senior gRPC/Proto Specialist, Senior Java Backend Developer, Senior DevOps Engineer |
| 17 | Senior System Architect | Senior Java Backend Developer, Senior Tester |
| 18 | Senior System Architect | Microservice Senior Expert, Senior DevOps Engineer, Senior Tester |
| 19 | Senior Documentation Engineer | Senior System Architect, Senior DevOps Engineer, Senior UX Designer, Senior Tester |
| 20 | Senior Swarm Orchestrator | Senior System Architect, Senior Tester, Git Commit Reviewer |

## Handoff Rules

- No role may introduce shared Java implementation modules between services.
- No role may implement service communication before the relevant contract
  exists and has review notes.
- No role may move source packages unless the slice write scope explicitly
  allows it.
- No role may claim runtime independence without build, start, test,
  configuration, healthcheck and container evidence.
- Every slice ends with diff inspection, quality evidence and a result note.
