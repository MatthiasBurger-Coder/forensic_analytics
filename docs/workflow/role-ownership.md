# Role Ownership

## Mandatory Roles

| Role | Workflow Responsibility |
|---|---|
| Senior Requirement Engineer | Maintains EPIC alignment, accepted assumptions, non-goals and requirement drift checks. |
| Senior System Architect | Owns service boundaries, data ownership, contract-first ordering and evidence-retention decisions. |
| Senior Java Backend Developer | Implements repository-source and query-report backend slices after contracts are verified. |
| Senior React Frontend Developer | Implements frontend adapter, routing and Workspaces UI after public API contracts are verified. |
| Senior Tester | Owns regression strategy, targeted verification, no-leak checks and full quality gate reporting. |

## Conditional Roles

| Role | Trigger |
|---|---|
| Senior UX Designer | Required for S05 list/action layout and accessibility review. |
| Security reviewer or Senior Security Sandbox Engineer | Required when list/delete/refresh touches Git remote, path, diagnostic or cleanup safety. |
| Senior Documentation Engineer | Required for S06 documentation closure. |
| Senior DevOps Engineer | Required only if implementation changes Docker, CI, deployment or runtime configuration. |

## Callable Subagents Used During Workflow Create

| Subagent Role | Result |
|---|---|
| Senior Requirement Engineer | Read-only requirement review completed. |
| Senior System Architect | Read-only architecture review completed. |
| Senior Java Backend Developer | Read-only backend planning review completed. |
| Senior React Frontend Developer | Read-only frontend planning review completed. |
| Senior Tester | Read-only test/quality review completed. |

No subagent modified files during workflow creation.

## Execution Ownership Rules

- Each slice owner verifies the active branch before edits.
- Subagents must not switch branches.
- Implementation must not occur on `main`, `master`, `develop` or another
  shared branch.
- A slice may request secondary review before implementation when its stop
  conditions touch another role's authority.
- No worker may bypass S01 contract verification.
