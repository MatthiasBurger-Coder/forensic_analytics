# 07 - Server API and Distributed Runtime

Status: planned slice.

## Objective

Plan a server-facing distributed runtime that accepts analysis requests, delegates orchestration, and keeps adapters out of core evidence ownership.

## Verified Current Baseline

- No implemented `forensic-analytics-server` module was found.
- `forensic-analytics-bootstrap` currently wires executable bootstrap behavior.
- `forensic-analytics-ingestion-grpc` is an inbound adapter for plugin analysis uploads.
- gRPC ingestion must not own persistence, Joern execution, replay, LLM, or final payload schema.

## Future Target

- Planned `forensic-analytics-server` receives analysis requests and delegates to `analysis-orchestrator`.
- Planned `analysis-orchestrator` creates analysis runs, source snapshots, queue jobs, and store records through application ports.
- Planned distributed runtime dispatches typed worker contracts through a technology-neutral queue/runtime boundary.
- Server APIs expose job state, diagnostics, artifact references, and projection availability without exposing storage internals.

## Subagent Roles

- Architecture reviewer: validate inbound adapter boundaries.
- Implementation worker: add server/runtime wiring only after ports and contracts exist.
- Quality reviewer: test API delegation, authorization placeholders where scoped, and deterministic responses.
- Security reviewer: review sensitive evidence access and runtime data exposure.
- Documentation reviewer: update API docs only after implemented endpoints exist.

## Implementation Steps

1. Inspect bootstrap, gRPC ingestion, application use cases, and current CLI boundaries.
2. Define server request/response contracts in terms of application commands and result views.
3. Ensure server APIs delegate to orchestrator ports instead of directly using persistence, Joern, graph, report, or LLM adapters.
4. Add distributed runtime wiring only after queue and worker contracts are implemented.
5. Keep the final plugin payload schema outside the gRPC ingestion adapter unless a future task explicitly defines it.

## Affected Files or Modules to Inspect

- `forensic-analytics-bootstrap`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-cli`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `settings.gradle.kts`
- `docs/adr/ADR-0001-plugins-are-producers.md`
- `docs/adr/ADR-0002-canonical-analysis-model.md`

## Evidence and Provenance Rules

- Server responses must distinguish accepted work, running work, completed evidence, failed jobs, dead-lettered jobs, and unavailable projections.
- API views must reference evidence and artifacts rather than embedding uncontrolled raw sensitive data.
- Runtime adapters must not infer missing results from queue state alone.

## Stop Conditions

Stop and report if:

- the server module or API scope is not approved for the task;
- a transport framework or queue runtime must be selected without ADR and dependency review;
- gRPC ingestion would need to own persistence, Joern, replay, LLM, report, graph, or final schema behavior;
- source/build files are outside the approved write scope.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run targeted server, bootstrap, or ingestion adapter tests first when available.

## Done Criteria

- Server/runtime behavior delegates through application boundaries.
- Inbound adapters remain thin request translators.
- API state views preserve evidence completeness and failure provenance.
- Concrete runtime technology decisions are documented before dependencies are added.
