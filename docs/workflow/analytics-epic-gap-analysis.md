# Analytics EPIC Gap Analysis

## Slice

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| sliceId | `02` |
| sliceTitle | Analyze Current Analytics EPIC And Related Docs |
| epicBaseline | `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md` |

## Review Result

Slice 02 read-only review passed. EPIC v0.1, documentation, ADRs,
architecture notes, module settings, services documentation and the gRPC
ingestion contract were inspected.

## Existing Coverage

EPIC v0.1 already covers these topics at a concept level:

- static fact import,
- canonical model persistence,
- Joern attachment,
- rule planning and generation,
- runtime event import,
- exception incident creation,
- replay,
- graph projection,
- LLM root-cause explanation,
- runtime data sensitivity,
- canonical identifiers.

ADR and arc42 coverage is strong for:

- plugins as producers,
- Analytics-owned canonical model,
- runtime sensitivity,
- graph and vector stores as projections,
- contract-first communication,
- one-owner data ownership.

The gRPC ingestion contract supports repository analysis, session upload,
payload descriptors, source facts, semantic artifacts, rule artifacts, runtime
traces, diagnostic reports, schema versions, build identity, plugin identity
and explicit ingestion statuses.

## Gap Decisions

| Topic | Decision | Rationale |
|---|---|---|
| Artifact package ingestion as an Analytics input path | Add | Producer-supplied packages must be equivalent to server-side repository analysis as an input source after normalization. |
| Manifest, checksum, artifact integrity, provenance, completeness and rejection semantics | Add | EPIC v0.2 needs requirement-level integrity language without inventing concrete proto fields. |
| Producer-neutral analysis input contract | Add | Static facts, semantic facts, runtime events, rule artifacts, diagnostic reports and artifact metadata need neutral requirement wording. |
| Producer-neutral static, semantic and runtime fact models | Add | Missing, partial and invalid package handling must be explicit. |
| Producer metadata as provenance only | Add | Plugin identifiers and local producer behavior must not become Analytics core semantics. |
| Correlation across static facts, semantic artifacts, rule artifacts, runtime traces, incidents, replay, graph, reports and LLM packages | Add | This is central to the analysis platform boundary. |
| Planned-vs-implemented wording discipline | Add | Graph, replay, report and LLM service behavior must stay planned unless verified from source, tests and runtime evidence. |
| Gradle/Maven task names, Mojo or task classes, local output paths, producer H2 schema, default ports and plugin cache behavior | Reject | These are producer implementation details. |
| Exact storage schema, graph labels, vector DB, relational DB, LLM provider, runtime collector mode, retry/deadline policy and contract field changes | Defer | These require dedicated contract, ADR or implementation workflows. |

## Documentation Synchronization Candidates

- `docs/README.md` should reference EPIC v0.2 after it exists and should state
  both input paths: server-side repository analysis and producer-supplied
  artifact package ingestion.
- `docs/arc42/README.md` still references the previous governance-flowchart
  workflow branch as current and should be updated or marked historical.
- `docs/architecture/current-state.md`,
  `docs/architecture/current-build-and-test-map.md`,
  `docs/architecture/current-coupling-map.md`,
  `docs/architecture/target-microservices-architecture.md` and
  `docs/architecture/monorepo-service-build-strategy.md` contain current-state
  claims that may be stale against `settings.gradle.kts`, `contracts/**` and
  `services/README.md`.
- `docs/architecture/service-boundaries.md` may need a status refresh for
  implemented initial services. Gateway, graph replay and report generation
  remain planned unless implementation evidence is verified.

## Verification

Commands used by the read-only review included:

```bash
git rev-parse --show-toplevel
git branch --show-current
git show-ref --verify --quiet refs/heads/docs/workflow-forensics-tracing-analytics-epic-alignment-20260516
git status --short --branch
rg -n "Version: 0.1|Source Note|Plugins|canonical|Runtime data|Joern|Byteman|BTM|LLM|Graph|artifact|manifest|checksum" docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md docs/README.md docs/arc42 docs/adr docs/architecture
rg -n "include\\(|services:" settings.gradle.kts services/README.md docs/architecture/current-build-and-test-map.md
```

## Decision

Slice 02 is accepted. Slice 03 may evaluate candidate EPIC changes using this
gap analysis and the Slice 01 fact matrix.
