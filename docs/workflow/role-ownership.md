# Role Ownership

| Slice | Primary owner | Required reviewers | N/A impact checks |
|---|---|---|---|
| S00 | Senior Workflow Architect | Senior Requirement Engineer, Senior System Architect, Senior Tester | Senior Java Backend, Senior React Frontend |
| S01 | Senior System Architect | Senior Requirement Engineer, Senior Documentation Engineer, Senior Tester, Skill Registry Conflict Auditor | Senior Java Backend, Senior React Frontend |
| S02 | Senior Tester | Senior System Architect, Senior Documentation Engineer | Senior Java Backend, Senior React Frontend |
| S03 | Senior Workflow Architect | Senior Documentation Engineer, Senior System Architect, Senior Tester | Senior Java Backend, Senior React Frontend |
| S04 | Senior Workflow Architect | Senior Swarm Orchestrator, Senior Documentation Engineer, Senior Tester | Senior Java Backend, Senior React Frontend |
| S05 | Senior System Architect | Senior Swarm Orchestrator, Senior Documentation Engineer, Senior Tester | Senior Java Backend, Senior React Frontend |
| S06 | Senior System Architect | Senior Documentation Engineer, Senior Tester, Skill Registry Conflict Auditor | Senior Java Backend, Senior React Frontend |
| S07 | Senior System Architect | Senior Documentation Engineer, Senior Tester | Senior Java Backend, Senior React Frontend |
| S08 | Senior Documentation Engineer | Senior System Architect, Senior Tester | Senior Java Backend, Senior React Frontend |
| S09 | Senior Workflow Architect | Senior System Architect, Skill Registry Conflict Auditor, Senior Documentation Engineer | Senior Java Backend, Senior React Frontend |
| S10 | Senior Performance Engineer | Senior Workflow Architect, Senior Documentation Engineer, Senior Tester | Senior Java Backend, Senior React Frontend |
| S11 | Senior Documentation Engineer | Senior System Architect, Senior Requirement Engineer, Senior Tester | Senior Java Backend, Senior React Frontend |

## Five-Role Workflow-Create Coverage

The workflow-create gate used all five mandatory perspectives:

- Senior Requirement Engineer: full review.
- Senior System Architect: full review.
- Senior Tester: full review.
- Senior Java Backend Developer: N/A impact check because product backend is out of scope.
- Senior React Frontend Developer: N/A impact check because frontend product code is out of scope.

During workflow execution, Java Backend and React Frontend roles remain N/A
impact checks unless a slice unexpectedly touches product files. If that
happens, execution stops as a scope conflict.
