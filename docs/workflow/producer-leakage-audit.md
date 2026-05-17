# Producer Leakage And Sensitive-Data Audit

## Slice

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| sliceId | `08` |
| sliceTitle | Producer Leakage And Sensitive-Data Audit |

## Producer-Specific Term Audit

Command:

```bash
rg -n "GenerateBtmTask|BtmGenMojo|btmGen|generateBtmRules|forensics:btmgen|forensics:analyze|RtTraceHelper|RtTrace|MethodLoggingAspect|AspectJ|cleanupPolicy|analysisStoreDirectory|joernExecutable|joernParseExecutable|joernSliceExecutable" docs/epics docs/arc42 docs/adr docs/README.md
```

Result: matches are allowed. They are historical logging or architecture
boundary references, not Analytics core producer behavior.

Allowed classifications:

| Match area | Classification | Reason |
|---|---|---|
| `docs/adr/ADR-0005-adapter-logging-observability-boundary.md` | Historical reference and explicit exclusion | Mentions inspected source material and forbids unsafe logging or AspectJ adoption in that slice. |
| `docs/adr/ADR-0006-spring-boot-server-boundary.md` | Explicit exclusion | States that AspectJ and concrete logging providers are not introduced by the Boot boundary. |
| `docs/adr/ADR-0008-cross-cutting-logging-module.md` | Explicit exclusion | Accepts Spring method interception only in a bounded logging module and forbids AspectJ annotations or weaving. |
| `docs/arc42/05-building-block-view.md` | Architecture boundary | Keeps observability and logging dependencies constrained. |
| `docs/arc42/08-crosscutting-concepts.md` | Explicit exclusion | States operational logs are diagnostics and not verified forensic evidence. |

No producer task, Maven goal, helper class, local path or producer schema was
introduced as Analytics core behavior in EPIC v0.2.

## Product-Scope Audit

Command:

```bash
rg -n "forensic-ui/|frontend/|services/|forensic-analytics-|contracts/|deployment/|examples/|data/" docs/epics docs/README.md docs/arc42 docs/adr docs/architecture
```

Result: matches are allowed. They are documentation references to verified
repository modules, planned service roots, contract documentation, deployment
gaps or architecture boundaries. The workflow did not modify product source,
contracts, frontend source, deployment files, examples or data.

## Sensitive-Data Audit

Command:

```bash
rg -n "secret|credential|token|password|raw runtime|raw trace|stack trace|LLM prompt|source payload" docs/epics docs/README.md docs/arc42 docs/adr docs/architecture
```

Result: matches are allowed. The hits describe protective controls, redaction,
logging exclusions, security stop conditions or sensitive-by-default runtime
data handling.

EPIC v0.2 states that runtime values are sensitive by default and that LLM
output is generated analysis, not evidence.

## Changed-Path Audit

Command:

```bash
git diff --name-only origin/main...HEAD
```

Result: changed paths are documentation and workflow files only. Product source,
contracts, frontend source, deployment files, examples, data and build logic are
outside this workflow's changed scope.

## Decision

Slice 08 passes. No producer implementation detail is described as Analytics
core behavior, and sensitive runtime/source/LLM material is not normalized as
safe by default.
