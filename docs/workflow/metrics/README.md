# Workflow Metrics

This directory stores process-performance diagnostics for workflow execution.

Metrics are operational notes. They help identify repeated governance reads,
long critical paths, unused parallelization opportunities and expensive quality
paths. They are not correctness evidence and must not replace required role
reviews, S3/S3D checks, D8 quality gates, commit review, checkpoint push rules
or `QUALITY.md` commands.

## Report Naming

Use:

```text
docs/workflow/metrics/<workflow-id>-run.md
```

The workflow id should be stable for the checked workflow version, for example
`governance-performance-20260521-v1-run.md`.

## Required Fields

Every metrics report should contain:

```text
workflowVersion
workflowBranch
runDate
executionProfile
sliceCount
rolesInvoked
filesRead
qualityCommandsRun
repeatedGovernanceReads
typedErrorRetries
blockerCount
longestCriticalPath
parallelizationOpportunitiesNotUsed
diagnosticLimitations
```

## Safety Rules

- Do not record secrets, credentials, tokens, prompt content, personal data,
  raw evidence payloads, source-code excerpts or runtime trace payloads.
- Do not run extra product, parser, Joern, BTM, replay, graph or UI performance
  checks just to populate these files.
- Do not block a required gate while waiting for metrics.
- Do not treat metrics as proof that a workflow, slice, product behavior or
  forensic result is correct.
