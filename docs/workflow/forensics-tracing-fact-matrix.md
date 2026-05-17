# Forensics Tracing Fact Matrix

## Slice

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| sliceId | `01` |
| sliceTitle | Extract Analysis-Relevant Facts From forensics_tracing |
| sourceProducerDescription | `/mnt/d/Projects/forensics_tracing/README.md` |
| targetEpicBaseline | `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md` |

## Review Result

Slice 01 read-only review passed. The producer checkout was inspected as the
current local working tree. The producer checkout is not treated as a clean
release baseline, and producer implementation details are not accepted as
Analytics core behavior.

Question: Does the implementation still match the EPIC?

Answer: partially. The current Analytics EPIC v0.1 and ADRs already say that
plugins are producers and Analytics owns normalization, persistence, replay,
graph projection and LLM evidence packaging. The producer description is more
explicit about artifact-package submission and confirms that legacy producer
internals must not leak into Analytics core.

## Classified Facts

| Fact | Classification | Analytics Core Treatment |
|---|---|---|
| Plugins submit build context over gRPC; the server owns repository and source analysis, artifacts, semantic enrichment, storage, runtime/replay, reporting and downstream analytics. | Platform requirement | Preserve as a non-negotiable Analytics boundary. |
| Session ingestion flow includes `StartAnalysisSession`, streaming `UploadAnalysisData`, `CompleteAnalysisSession` and `AbortAnalysisSession` in the producer contract shape. | Platform requirement | Accept the producer-neutral ingestion lifecycle concept; exact RPC semantics remain contract-governed. |
| Payload envelope includes build, module, plugin identity, schema version, payload descriptor, content type, attributes and bytes. | Platform requirement | Accept as artifact-package ingestion shape with producer metadata as provenance. |
| Payload categories include source facts, semantic artifacts, rule artifacts, runtime trace and diagnostic report. | Platform requirement | Keep as producer-neutral payload categories. |
| Source files, classes, methods, source locations, signatures, return types and static scan events are relevant input facts. | Platform requirement | Analytics owns normalized canonical facts; producer parser classes are not adopted. |
| Branches, switches, returns, throws and call or dependency candidates from static scanning are available as static candidates. | Platform requirement | Accept as candidate static facts only; never present them as proof of runtime execution. |
| Rule artifacts, rule identifiers, manifests and checksums are analysis-relevant. | Platform requirement | Accept integrity and provenance requirements; do not adopt producer file names or renderer classes. |
| Semantic nodes, edges, methods, call relations, control-flow relations, data-flow paths and semantic anchors are analysis-relevant. | Platform requirement | Accept semantic fact requirements behind Analytics-owned adapters. |
| Runtime event concepts include timestamp, event type, thread identity, correlation ID, span ID, runtime details, exception metadata and error metadata. | Platform requirement | Accept as runtime observation requirements; runtime values remain sensitive by default. |
| `traceId` and `parentSpanId` are EPIC canonical IDs, but the Slice 01 review did not verify them in the producer runtime helper. | Open decision | Keep in EPIC and contract review; do not infer them from producer span data. |
| Producer gRPC defaults such as host, port `6565`, plaintext mode, deadline seconds and schema-version defaults. | Producer implementation | Explicitly exclude from Analytics domain requirements. |
| Gradle plugin ID, extension name, task names, Maven goal prefix and Maven goal names. | Producer implementation | Explicitly exclude from Analytics core wording. |
| Legacy producer JavaParser, rule renderer, H2 store, runtime helper, aspect, domain model and older use cases. | Producer implementation | Treat as migration-audit inventory only. |
| Producer local paths such as `forensics/`, `manifest.json`, `checksums.sha256`, `engine-request.json` and local cache or store names. | Producer implementation | Explicitly exclude as canonical Analytics paths or schemas. |
| Quickstart project identifiers, repository URLs, branch or commit examples, Java 17 producer baseline and localhost setup. | Example only | Do not convert into Analytics requirements. |
| Analytics database, graph database, vector database, runtime ingestion mode, runtime value policy and LLM provider choices remain unresolved in EPIC v0.1. | Open decision | Preserve as open decisions until dedicated architecture or ADR slices decide them. |

## Explicit Exclusions From Analytics Core

- Producer task names, Maven goals, plugin extension names and helper class names.
- Producer default host, port, deadline, plaintext and local setup defaults.
- Producer-local H2 schemas, cache behavior, cleanup policy and output paths.
- Producer JavaParser, runtime helper, aspect, renderer and writer class names.
- Quickstart examples, sample repository coordinates and local machine paths.

## Verification

Commands used by the read-only review included:

```bash
git branch --show-current
git rev-parse --show-toplevel
git show-ref --verify --quiet refs/heads/docs/workflow-forensics-tracing-analytics-epic-alignment-20260516
git status --short --branch
test -f /mnt/d/Projects/forensics_tracing/README.md
rg -n "Current Boundary|server owns|StartAnalysisSession|UploadAnalysisData|CompleteAnalysisSession|AbortAnalysisSession|6565|forensics:btmgen|RtTraceHelper|MethodLoggingAspect|analysisStoreDirectory" /mnt/d/Projects/forensics_tracing/README.md /mnt/d/Projects/forensics_tracing/src/main /mnt/d/Projects/forensics_tracing/src/test
```

## Decision

Slice 01 is accepted. Slice 03 may use these facts only through their recorded
classification.
