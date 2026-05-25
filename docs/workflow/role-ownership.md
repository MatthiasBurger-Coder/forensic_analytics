# Role Ownership

## Mandatory Roles

| Role | Workflow Responsibility |
|---|---|
| Senior Requirement Engineer | Keep request, acceptance criteria and non-goals stable. |
| Senior System Architect | Preserve service boundaries, H2 ownership and public facade rules. |
| Senior Java Backend Developer | Implement repository-source and query-report bootstrap fixes. |
| Senior React Frontend Developer | Confirm no frontend production impact unless live startup proves otherwise. |
| Senior Tester | Own regression tests, quality gates and curl proof acceptance. |

## Conditional Roles

| Role | Trigger |
|---|---|
| Senior Git/Workspace Specialist | WildFly checkout, workspace root and cleanup semantics. |
| Senior Security/Sandbox Engineer | Untrusted repository and safe Git command review. |
| Senior Performance Engineer | Large repository timing and API responsiveness checks. |
| Senior DevOps | Local service startup, ports, process logs and UI availability. |
| Senior Documentation Engineer | Execution report and arc42 check closure. |

## Subagent Policy

Callable subagents were not used during workflow creation. During
`workflow execute`, callable subagents may be used only when the executor can
verify the active branch and the runtime exposes the matching agent. If
callable subagents are unavailable, the role files listed above are the review
checklists.
