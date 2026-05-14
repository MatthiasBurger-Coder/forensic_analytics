# Workplan Index

This directory contains planned workflows for future distributed analysis-orchestrator work. These documents are execution plans for subagents and do not describe implemented behavior unless a section explicitly says it is part of the verified current baseline.

## Files

- [workflow.md](workflow.md) - master orchestration workflow and slice order
- [TEMPLATE.md](TEMPLATE.md) - required structure for future workplan slices
- [00-documentation-baseline-alignment.md](00-documentation-baseline-alignment.md) - documentation entry points and workflow ownership
- [01-orchestrator-domain-vocabulary.md](01-orchestrator-domain-vocabulary.md) - planned orchestration vocabulary
- [02-source-snapshots-and-workspaces.md](02-source-snapshots-and-workspaces.md) - planned immutable source snapshots and workspace handling
- [03-analysis-job-queue-and-retry.md](03-analysis-job-queue-and-retry.md) - planned queue-neutral job lifecycle and retry model
- [04-typed-worker-contracts.md](04-typed-worker-contracts.md) - planned worker contracts
- [05-analysis-store-and-artifact-store.md](05-analysis-store-and-artifact-store.md) - planned canonical storage and artifact boundaries
- [06-graph-report-and-llm-projections.md](06-graph-report-and-llm-projections.md) - planned projection workers
- [07-server-api-and-distributed-runtime.md](07-server-api-and-distributed-runtime.md) - planned server API and runtime wiring
- [08-quality-ci-and-rollout.md](08-quality-ci-and-rollout.md) - planned verification and rollout controls

## Execution Rule

Future subagents must treat each slice as a bounded task. They must verify the current source, tests, ADRs, and `QUALITY.md` before editing, and they must stop when a required symbol, module, Gradle task, schema, status, or storage concept cannot be found exactly as expected.
