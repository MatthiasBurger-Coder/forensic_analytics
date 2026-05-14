# 06 - Graph, Report, and LLM Projections

Status: completed contract-baseline slice.

## Objective

Define graph, report, LLM, and vector projection contracts that derive outputs from canonical evidence without becoming sources of truth.

## Verified Current Baseline

- No graph adapter module or report module was found.
- Joern semantic analysis currently belongs to the Joern Docker adapter path, not a graph database projection module.
- ADRs state that graph and vector databases are projections derived from the canonical model.
- LLM output must be treated as generated analysis or hypotheses, not verified evidence.
- Provider-neutral projection contracts now exist in the domain layer.

## Future Target

- Planned `graph-analysis-worker` builds graph projections from canonical analysis-store records and artifact-store references.
- Planned `report-worker` builds deterministic reports from canonical evidence, diagnostics, projections, and explicit unresolved gaps.
- Planned LLM projection or prompt preparation consumes structured evidence and labels generated output as hypothesis, explanation, recommendation, or generated text.
- No projection worker mutates primary evidence or finalizes facts that were not present in canonical inputs.

## Completed Contract Baseline

- `AnalysisProjectionKind` defines graph, report, LLM, and vector projection kinds.
- `AnalysisProjection` requires canonical input references for every projection record.
- Available projections require a concrete artifact reference.
- Unavailable and failed projections require explicit diagnostics.
- `AnalysisProjectionOutputLabel` prevents LLM output from being labeled as a canonical projection; LLM output must be `GENERATED` or `HYPOTHESIS`.
- No graph database, report renderer, vector database, LLM provider, prompt runtime, or projection worker adapter was selected or introduced.

## Subagent Roles

- Replay/graph/LLM reviewer: verify projection boundaries and evidence labeling.
- Implementation worker: implement one projection type per approved slice.
- Quality reviewer: test deterministic ordering, provenance references, and unknown handling.
- Security reviewer: review runtime data redaction before LLM or vector use.
- Documentation reviewer: update report or projection docs only after behavior exists.

## Implementation Steps

1. Inspect canonical model, semantic facts, current Joern outputs, and ADRs.
2. Define projection input contracts from analysis-store and artifact-store references.
3. Add tests that reject projections from incomplete or unverified inputs unless gaps are explicit.
4. Keep graph/vector/report/LLM provider APIs outside domain and application.
5. Add concrete projection adapters only after technology decisions are documented.

## Affected Files or Modules to Inspect

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-persistence`
- `forensic-analytics-testbed`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`

## Evidence and Provenance Rules

- Projection records must reference canonical evidence and input artifacts.
- Static relationships must not be presented as executed runtime flow.
- LLM responses must not overwrite evidence and must stay separate from verified facts.
- Missing or redacted runtime values must remain visible as missing or redacted.

## Stop Conditions

Stop and report if:

- a graph label, report section, LLM prompt field, or projection schema cannot be verified;
- a projection requires a concrete provider without ADR and dependency review;
- a worker would infer branch execution, parameter values, or causal chains without evidence;
- sensitive runtime data would be sent to an unsafe destination.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run targeted projection, Joern, report, or prompt tests first when available.

## Done Criteria

- Projection contracts are deterministic and evidence-referenced.
- Unavailable and failed projections preserve diagnostics without implying evidence completeness.
- LLM-related tests do not require live provider access.
