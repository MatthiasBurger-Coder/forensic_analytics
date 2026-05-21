# Role Ownership

| Slice | Owner | Required reviewers | N/A impact checks |
|---|---|---|---|
| S00 | Senior Workflow Architect | Senior Requirement Engineer, Senior System Architect, Senior Tester | Senior React Frontend |
| S01 | Senior Tester | Senior Java Backend, Senior System Architect, Security Sandbox Specialist | Senior React Frontend, Senior DevOps |
| S02 | Senior Performance Engineer | Git Large Repository Specialist, Senior DevOps, Senior Tester, Security Sandbox Specialist | Senior React Frontend |
| S03 | Contract Governance Expert | Senior Java Backend, Senior System Architect, Senior Tester, Senior React Frontend | Senior DevOps |
| S04 | Senior DevOps | Microservice Senior Expert, Senior System Architect, Senior Tester | Senior React Frontend |
| S05 | Microservice Senior Expert | Senior System Architect, Senior Java Backend, Senior Tester | Senior React Frontend |
| S06 | Senior Java Backend | Contract Governance Expert, Senior System Architect, Senior Tester, Microservice Senior Expert | Senior React Frontend unless Gateway API fields change |
| S07 | Senior System Architect | Senior Java Backend, Microservice Senior Expert, Senior DevOps, Senior Tester | Senior React Frontend unless Gateway API fields change |
| S08 | Senior Documentation Engineer | Senior Workflow Architect, Senior System Architect, Senior Tester | Senior React Frontend unless docs describe UI behavior |

## Callable Subagent Policy

Use callable subagents during `workflow execute` only when the active user
request explicitly authorizes delegated or parallel agent work. Otherwise apply
the role files as local review checklists and record that no callable subagent
was used.
