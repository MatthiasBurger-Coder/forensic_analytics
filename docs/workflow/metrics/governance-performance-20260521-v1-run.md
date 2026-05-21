# Governance Performance Workflow Run Metrics

```text
workflowVersion=governance-performance-20260521-v1
workflowBranch=architecture/workflow-governance-performance-20260521
runDate=2026-05-21
executionProfile=FULL_PATH
sliceCount=12
rolesInvoked=Senior Workflow Architect; Senior Requirement Engineer; Senior System Architect; Senior Documentation Engineer; Senior Tester; Senior Execution Orchestrator; Senior Swarm Orchestrator; Senior Performance Engineer; Skill Registry Conflict Auditor; Release Branch Governance; Flowchart Integrity Auditor
filesRead=not captured before process-performance-profiler activation
qualityCommandsRun=git diff --check; git diff --cached --check; python3 -m json.tool docs/workflow/context-pack.json; python3 -m json.tool docs/skill-audit/skill-registry.json; targeted rg governance checks
repeatedGovernanceReads=AGENTS.md; QUALITY.md; docs/workflow/workflow.md; docs/workflow/context-pack.json; .agents/orchestrator/routing-rules.md; .agents/skills/workflow-executor/SKILL.md; docs/agents/skill-registry.md
typedErrorRetries=0
blockerCount=0
longestCriticalPath=S00 -> S01 -> S03 -> S04 -> S05 -> S11
parallelizationOpportunitiesNotUsed=S06/S08 and S07/S09 were executed serially in this local run to keep checkpoint commits and documentation updates one-slice-at-a-time
diagnosticLimitations=Timing and file-read counts were not captured for early slices because the profiler was introduced by S10. Metrics are diagnostics only and are not correctness evidence.
```

## Slice Checkpoint Summary

| Slice | Checkpoint |
|---|---|
| S00 | `f5fc0be` |
| S01 | `d1398fe` |
| S02 | `2d0c444` |
| S03 | `954bdf7` |
| S04 | `80d4d21` |
| S05 | `c2859a3` |
| S06 | `3b77583` |
| S07 | `b948465` |
| S08 | `c04b54b` |
| S09 | `264bd48` |
| S10 | `ca64df1` |

## Notes

The metrics report intentionally omits raw command logs, prompt content,
source-code excerpts and forensic evidence payloads. The final S11 checkpoint
will be recorded in the execution report after commit and push.
