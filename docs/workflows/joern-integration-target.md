# Joern Integration Target

## Intent

Joern is an infrastructure adapter for semantic source analysis. It must remain outside domain and application code.

The current Docker workflow only provides local runtime infrastructure:

```text
Repository checkout
-> Joern CPG creation
-> Joern query execution
-> local artifact storage
-> later ingestion into the analysis store
```

It does not define a production analysis service, a gRPC protocol, graph persistence, replay reconstruction or report semantics.

## Future Port Shape

A future application port may look like this:

```java
public interface CodePropertyGraphAnalysisPort {
    CodePropertyGraphAnalysisResult analyze(CodePropertyGraphAnalysisRequest request);
}
```

The exact request and result types must be verified from source before implementation. Do not introduce this interface by name similarity alone.

## Adapter Options

A future adapter can choose one of several verified execution modes:

- start a local Docker container,
- call an already running Joern service,
- enqueue or trigger analysis through gRPC,
- import existing CPG files only.

The decision remains open until the platform integration task defines the required operational contract.

## Evidence Boundaries

Joern output is semantic analysis output, not runtime execution evidence.

Future ingestion must preserve:

- source locations when available,
- unresolved or incomplete analysis states,
- output artifact provenance,
- the pinned Joern image reference or service version,
- query names and query inputs,
- deterministic artifact checksums where artifacts are stored.

Joern findings must not be presented as confirmed runtime behavior. Static semantic relationships and runtime trace facts must remain distinct.

## Non-Goals

- no domain dependency on Joern classes,
- no application dependency on Docker APIs,
- no default quality gate dependency on live Docker or Joern,
- no hidden fallback image,
- no generated CPG, log or query output files committed to Git.
