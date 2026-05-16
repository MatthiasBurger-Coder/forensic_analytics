# Deadlock Prevention Rules

## Core Rules

- Every slice has exactly one owner at a time.
- Every reviewer may block, but must provide concrete resolution steps.
- No agent may wait for an artifact without knowing its owner.
- No cyclic handoff chain may continue without Senior Swarm Orchestrator or Agent Workflow Orchestrator decision.
- No parallel work may edit the same files without ownership and merge order.
- Generated artifacts must not become inputs until their generation contract is stable.

## Blocker Classes

```text
REQUIRES_INPUT
REQUIRES_DECISION
REQUIRES_FIX
REQUIRES_ARCHITECTURE_DECISION
```

## Deadlock Patterns

| Pattern | Risk | Resolution |
| --- | --- | --- |
| Mutual artifact wait | Two agents wait on each other's output. | Orchestrator assigns source of truth and sequence. |
| Shared file ownership | Parallel workers edit the same file. | Split ownership or serialize work. |
| Missing reviewer | Slice requires a target skill that does not exist. | Use documented bootstrap role or stop. |
| Unstable contract | Implementation depends on unapproved API or data contract. | Run contract or data steward first. |
| Quality gate ambiguity | Commit readiness depends on unknown command. | Verify `QUALITY.md` and build files or stop. |
| ADR uncertainty | Implementation conflicts with existing decision. | Route to ADR Steward and Senior System Architect. |

## Stop Conditions

Stop when:

- owner cannot be named;
- dependency cycle cannot be broken;
- handoff target is missing;
- blocker class is unknown;
- conflict resolution would require guessing repository rules or architecture authority.
