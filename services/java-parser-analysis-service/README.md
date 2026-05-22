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

The provisional Slice 07 inbound API is `JavaAstAnalysisService.AnalyzeSourceSnapshot`
from `contracts/grpc/java-ast-analysis.proto`.

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

## Local Commands

```bash
./gradlew :services:java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:java-parser-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/java-parser-analysis-service/Dockerfile --build-arg SERVICE_JAR=services/java-parser-analysis-service/build/libs/java-parser-analysis-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/java-parser-analysis-service:local .
```

Local operator start command:

```bash
./gradlew :services:java-parser-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Default local ports are gRPC `9094` and health `8085`. S07 does not add Docker
Compose, Docker Swarm or Kubernetes readiness for the FA-MSA-001 target
landscape.
