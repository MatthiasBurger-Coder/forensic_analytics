# Workflow History

This file is the authoritative workflow-version record for `workflow execute`.
It preserves version identity and slice handoff state; it does not replace
`AGENTS.md`, `QUALITY.md`, ADRs, arc42 documentation or the active workflow.

## Active Workflow

- workflowId: `gov-02-04-role-inventory-skill-schema`
- workflowVersion: `gov-02-04-role-inventory-skill-schema-v1`
- title: GOV-02 and GOV-04 Role Inventory Validation and Skill Schema Standardization
- branch: `feature/workflow-gov-02-04-20260814`
- processStrand: `workflow execute`
- executionProfile: `FULL_PATH`
- status: `READY_FOR_EXECUTION`
- completedSlices: `[]`
- source: [`workflow.md`](workflow.md)
- contextPack: [`context-pack.json`](context-pack.json)

## Slice Checkpoint Record Contract

Each successful slice checkpoint records exactly one slice with these fields:

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

Before the slice commit, `commitHash` is `pending`. After the commit and branch
push, the actual commit hash and push result are recorded in the execution
report or in a new checkpoint entry in this file.

## Governance Cache Status

The skill registry and audit files remain derived caches. Slice S04 owns their
refresh after S01–S03 produce verified validator and schema evidence. The
registry must not be used as the source of truth for this workflow.
