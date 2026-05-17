# 10. Quality Requirements

## 10.1 Quality Tree

```text
Quality
├── Performance
│   ├── Incremental static imports
│   ├── Fast incident graph queries
│   └── Low runtime overhead
├── Scalability
│   ├── Multi-module support
│   ├── Large legacy codebase support
│   └── Runtime event partitioning
├── Security
│   ├── Runtime redaction
│   ├── Secret protection
│   ├── Auditability
│   └── Retention control
├── Observability
│   ├── Adapter correlation
│   ├── Sanitized operation logs
│   └── Logging framework isolation
├── Traceability
│   ├── Rule to source mapping
│   ├── Event to rule mapping
│   ├── Patch to incident mapping
│   └── Diagnosis to evidence mapping
└── Extensibility
    ├── Replaceable storage adapters
    ├── Replaceable LLM provider
    ├── Replaceable graph projection
    └── Replaceable vector projection
```

## 10.2 Quality Scenarios

| Scenario | Expected Result |
|---|---|
| Import changed Java files only | The platform detects changed files by hash and avoids full reprocessing |
| Load incident by CorrelationID | Replay timeline is generated in interactive time |
| Runtime event contains secret-like value | Value is redacted before persistence |
| Adapter operation fails | Logs contain operation category, correlation ID and exception category without raw payloads or secrets |
| Joern mapping is ambiguous | Mapping is marked as ambiguous and not silently linked |
| LLM lacks evidence | Diagnosis reports insufficient evidence |
| Tests fail after generated fix | No PR is created |
| `workflow create` has blocking questions | No final `docs/workflow/workflow.md` is created and `workflow execute` is not released |
| `workflow create` clarification retries are exhausted | The loop stops after `maxRetries = 3` and escalates to the Root Architect |
| `workflow create` completes | `docs/workflow/workflow.md` and arc42 are checked, Documentation Governance passes and release for `workflow execute` is explicit |
| `workflow execute` completes a slice | The slice quality gate passes before a slice checkpoint commit and push |
| `workflow execute` preflight fails | The process stops and reports at `S3_STATUS`, `S3_BRANCH` or `S3_SCOPE` without mutating files |
| Slice cannot be classified | `S3_CLASSIFY` routes to `S3_UNCLASSIFIED` and Root Architect escalation |
| S3D detects overlapping locks | The failure routes as `LOCK_CONFLICT` through the Typed Error Router |
| Slice checkpoint commit is created | The commit contains exactly one slice and records workflow version, changed files, quality result, commit hash and rollback reference |
| Checkpoint push fails | The failure routes to `CP_ROLLBACK` or Root Architect escalation instead of force-push or `push auto` |
| Slice checkpoint push is requested | The push targets only `origin/<workflow-branch>` and does not create or merge a PR |
| `push auto` is requested | `S1_PUSH_ELIGIBILITY_GUARD` proves the change belongs to `skills-agents` and no product implementation files changed |
| Governance diagram is changed | Level 1 and Level 2 diagrams are checked for dead nodes, missing paths, unbounded loops, missing STOP paths, circular references, missing terminals, wrong backward jumps and missing escalation paths |

## 10.3 Agent Governance Quality Scenarios

| Scenario | Quality Goal | Expected Response |
|---|---|---|
| User requests `skills update` | Process correctness | Codex routes to `skills-agents` and does not change product code |
| User requests `workflow create` | Requirement quality | Codex clarifies requirements before workflow authoring |
| Blocking questions exist | Safety | Codex returns `REQUIRES_REFINEMENT` and does not create final workflow.md |
| Clarification attempts exceed `maxRetries = 3` | Safety | Codex stops the loop and escalates to the Root Architect |
| User requests `workflow execute` | Execution reliability | Codex executes checked slices only |
| Slice completes | Recoverability | Codex commits and pushes the workflow branch as a checkpoint |
| Slice quality gate fails | Ownership clarity | Codex classifies the failure with the Typed Error Router before retry or escalation |
| Quality failure cannot be classified | Safety | Codex routes `UNKNOWN_FAILURE` to Root Architect escalation |
| Retry attempts are exhausted | Determinism | Codex stops after `maxRetries = 3` and escalates instead of looping |
| Workflow scope is wrong during execution | Process boundary safety | Codex reports the conflict and does not regenerate `workflow create` output automatically |
| User requests `push auto` | Publication safety | Codex allows it only for `skills-agents` |
