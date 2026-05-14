# Workplan Index

This directory contains workflows for distributed analysis-orchestrator work. Slices 00 through 08 now describe the completed technology-neutral contract baseline and continue to distinguish that baseline from planned concrete runtime behavior.

## Files

- [workflow.md](workflow.md) - master orchestration workflow and slice order
- [TEMPLATE.md](TEMPLATE.md) - required structure for future workplan slices
- [00-documentation-baseline-alignment.md](00-documentation-baseline-alignment.md) - documentation entry points and workflow ownership
- [01-orchestrator-domain-vocabulary.md](01-orchestrator-domain-vocabulary.md) - completed orchestration vocabulary
- [02-source-snapshots-and-workspaces.md](02-source-snapshots-and-workspaces.md) - completed immutable source snapshots and workspace handling
- [03-analysis-job-queue-and-retry.md](03-analysis-job-queue-and-retry.md) - completed queue-neutral job lifecycle and retry model
- [04-typed-worker-contracts.md](04-typed-worker-contracts.md) - completed provider-neutral worker contracts
- [05-analysis-store-and-artifact-store.md](05-analysis-store-and-artifact-store.md) - completed canonical storage and artifact port baseline
- [06-graph-report-and-llm-projections.md](06-graph-report-and-llm-projections.md) - completed provider-neutral projection contracts
- [07-server-api-and-distributed-runtime.md](07-server-api-and-distributed-runtime.md) - completed server-facing request/status view contracts without runtime wiring
- [08-quality-ci-and-rollout.md](08-quality-ci-and-rollout.md) - completed verification and rollout status documentation

## Execution Rule

Future subagents must treat each slice as a bounded task. They must verify the current source, tests, ADRs, and `QUALITY.md` before editing, and they must stop when a required symbol, module, Gradle task, schema, status, or storage concept cannot be found exactly as expected.
