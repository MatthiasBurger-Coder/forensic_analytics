# Governance Conflict Review

## Review Result

Governance Flowchart V2 is compatible with the accepted three-strand model only when S1, S2 and S3 are labels for existing process strands:

| V2 label | Existing strand |
|---|---|
| S1 | `skills-agents` |
| S2 | `workflow create` |
| S3 | `workflow execute` |

S3D is not a new strand. It is the execution-orchestration node inside `workflow execute`.

## Resolved Conflicts

| Conflict | Resolution |
|---|---|
| V2 labels are not existing repository terms | Treat them as introduced labels and require execution slices to map them explicitly. |
| `workflow execute` could jump back to workflow creation | Add R10: no automatic backward jump from S3 to S2. |
| Slice checkpoint commits could collect multiple slices | Add R11: one slice, one commit. |
| Generic quality failure routing hides ownership | Add Typed Error Router with explicit error categories and target roles. |
| Automatic correction loops could run indefinitely | Add `maxRetries = 3` and Root Architect escalation. |
| Rollback could imply destructive Git reset | Define `CP_ROLLBACK` as a governance decision node, not blind reset. |
| Documentation Governance could become a fourth strand | Keep `DOCROOT` global and local docs nodes strand-scoped. |
| Workflow-create files could be sent through `push auto` | Treat any branch containing `docs/workflow/**` as a `workflow create` or `workflow execute` publication and use normal `push` or slice checkpoint push instead. |

## Remaining Gaps For workflow execute

- Decide whether to add a dedicated Root Architect role file or keep Root Architect escalation mapped to Senior System Architect.
- Decide whether to add a dedicated flowchart-integrity skill or keep the check distributed across documentation, architecture and quality roles.
- Update active process, agent, arc42 and ADR documents in the planned execution slices.

## Stop Conditions

Stop if:

- a V2 label cannot be mapped to an existing strand or approved new node,
- a slice would create a fourth process strand,
- `workflow execute` would rewrite `workflow create` artifacts automatically,
- rollback would require destructive Git operations without explicit approval,
- documentation claims planned governance as already implemented before its slice is complete.
