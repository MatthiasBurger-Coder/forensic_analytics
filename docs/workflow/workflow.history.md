# Workflow History

## Active Version

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| workflowTitle | Align Forensics Tracing Description With The Analytics EPIC |
| sourceWorkflow | `docs/workflow/workflow.md` |
| workflowCreateBranch | `docs/workflow-forensics-tracing-analytics-epic-alignment-20260516` |
| workflowCreateStatus | created |

## Previous Active Workflow

The previous `docs/workflow/**` package described Governance Flowchart V2. It
was replaced by this workflow during `workflow create` on the dedicated branch.
Historical content remains available through Git history.

## Checkpoint Record Template

When `workflow execute` records slice checkpoints, each record must include:

```text
workflowVersion
sliceId
sliceTitle
responsibleAgent
changedFiles
qualityGateCommands
qualityGateResult
commitHash
rollbackReference
arc42Updated
adrUpdated
```

Do not add a slice checkpoint record until the slice has actually executed.
