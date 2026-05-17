# Producer Boundary Comparison

## Slice

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| sliceId | `04` |
| sliceTitle | Contract And Producer Boundary Comparison |
| producerProto | `/mnt/d/Projects/forensics_tracing/src/main/proto/forensic_ingestion.proto` |
| analyticsContract | `contracts/grpc/forensic-ingestion.proto` |

## Review Result

Slice 04 read-only comparison passed. No contract change is authorized by this
workflow.

## Comparison Findings

| Topic | Finding | EPIC v0.2 Treatment |
|---|---|---|
| Port defaults | The producer quickstart and configuration default to port `6565`, while Analytics documentation and runtime configuration use port `9090`. | Treat as deployment and configuration difference, not an EPIC requirement. |
| RPC surface | The producer proto defines session upload RPCs: `StartAnalysisSession`, streaming `UploadAnalysisData`, `CompleteAnalysisSession` and `AbortAnalysisSession`. The Analytics contract retains `AnalyzeRepository` plus the same session RPC set. | Describe both input paths at requirement level: server-side repository analysis and producer-supplied artifact/session ingestion. Do not encode exact RPC defaults. |
| `AnalyzeRepository` responsibility | `AnalyzeRepository` is a compatibility/current-monolith surface. The independent ingestion service treats repository checkout as owned by repository-analysis responsibilities. | Keep this as a contract-governance topic outside EPIC v0.2. |
| Proto namespace and field numbers | Both producer and Analytics use `de.burger.forensics.analytics.ingestion.v1`, and the shared session-upload message field numbers align. Analytics adds `AnalyzeRepository*` messages and documents `payload_type = 6` as deprecated and not reusable. | Do not change fields or compatibility policy in this workflow. |
| Producer and Analytics boundary | Producer owns build-tool configuration, channel/session handling and build-context submission. Analytics owns repository/source analysis, domain decisions, artifacts, semantic enrichment, storage, runtime/replay, reporting and downstream analytics. | Accept as EPIC boundary language. |
| Legacy producer packages | The producer checkout still contains legacy adapter, application, domain, infrastructure and rule-generation packages. | Treat as migration-audit inventory only. |
| Plugin quickstart identifiers | Plugin IDs, extension names, task names, Maven goals, quickstart project IDs, sample hosts and snapshot versions are examples or producer implementation details. | Exclude from Analytics core wording. |

## Must Not Enter EPIC v0.2

- Port `6565` or `9090` as domain or platform requirement.
- Exact RPC defaults, producer deadlines, plaintext mode, retry behavior or
  idempotency behavior.
- Plugin task names, Maven goals, producer Java package names, helper class
  names or quickstart identifiers.
- Producer-local H2, cache, store paths, local output paths or Java 17 producer
  baseline.
- Compatibility claims about `AnalyzeRepository` beyond the verified
  contract-governance note.

## Deferred To Contract Workflow Or ADR

- Port convergence and client/server default compatibility.
- Whether `AnalyzeRepository` remains in v1, moves fully to repository-analysis
  contracts or is removed in a future major contract.
- Abort semantics, retry, deadline, cancellation, idempotency and duplicate
  session handling.
- Message sizing, compression and chunked upload policy.
- Formal reserved-field policy for deprecated `payload_type = 6`.
- Manifest and checksum payload semantics and contract tests.
- Producer migration from producer-local proto source to contract-owned proto
  source.

## Verification

Commands used by the read-only review included:

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git show-ref --verify --quiet refs/heads/docs/workflow-forensics-tracing-analytics-epic-alignment-20260516
rg -n "6565|9090|AnalyzeRepository|StartAnalysisSession|UploadAnalysisData|CompleteAnalysisSession|AbortAnalysisSession" /mnt/d/Projects/forensics_tracing/README.md /mnt/d/Projects/forensics_tracing/src/main/proto/forensic_ingestion.proto contracts/grpc/forensic-ingestion.proto docs/README.md
```

## Decision

Slice 04 is accepted. EPIC v0.2 may describe producer-neutral input paths and
Analytics-owned normalization, provenance, correlation, replay, graph
projection, reporting context and LLM evidence packaging. It must not change or
imply concrete contract behavior.
