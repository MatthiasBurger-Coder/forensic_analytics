# JavaParser Analysis Service

JavaParser Analysis is an independent Spring Boot/gRPC worker for JavaParser-based
static source scanning. It owns provisional Java source-fact extraction,
stable source-fact IDs, AST diagnostics, produced source-fact artifacts and
producer-local artifact metadata. The current predecessor pipeline may still
handoff references to `analysis-store-service`, but FA-MSA-001 target ownership
keeps canonical static Java facts with `java-parser-analysis-service`.

The service does not import the current modular-monolith domain, application or
adapter modules. It generates transport classes from `contracts/grpc` inside
its own Gradle project and maps them into service-owned models.

## Contract

The current FA-MSA-001-LMR S05 inbound API is
`JavaAstAnalysisService.AnalyzeSourceSnapshot` from
`contracts/grpc/java-ast-analysis.proto`.

Inputs are immutable source snapshot identifiers, declared Java source roots,
bounded UTF-8 source files and scan policy limits. Results are delivered as
deterministic source-fact artifact metadata plus counts and diagnostics. The
response does not claim runtime execution, branch execution, reachability or
observed values.

Current JavaParser extraction preserves verified behavior from the legacy
adapter: deterministic method facts, nested type names, signatures, source
locations and explicit parse diagnostics. JavaParser symbol solving is not
configured in this slice, so the service reports
`SYMBOL_RESOLUTION_NOT_CONFIGURED` as a completeness-affecting limitation for
every scan until real symbol solving is implemented. Source-fact artifacts
carry explicit `sourceRoot` context and never represent static reachability as
observed runtime execution.

Source-fact artifact retrieval references are immutable forensic evidence
handles. A repeated write for the same run, job and source snapshot succeeds
only when the produced bytes are identical; different bytes for an existing
reference are rejected instead of replacing previously published evidence.

S16 uses this service as worker-contract evidence only. The JavaParser service
returns source-fact artifact metadata and diagnostics; it does not replace the
legacy in-process repository-analysis summary, BTM rule generation, runtime
execution or semantic graph assertions.

## Local Commands

```bash
./gradlew :java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :java-parser-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f java-parser-analysis-service/Dockerfile --build-arg SERVICE_JAR=java-parser-analysis-service/build/libs/java-parser-analysis-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/java-parser-analysis-service:local .
```

Local operator start command:

```bash
./gradlew :java-parser-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Default local ports are gRPC `9094` and health `8085`. S05 does not add Docker
Compose, Docker Swarm or Kubernetes readiness for the FA-MSA-001 target
landscape.
