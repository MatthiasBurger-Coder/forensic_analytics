# Java AST Analysis Service

Java AST Analysis is an independent Spring Boot/gRPC worker for JavaParser-based
static source scanning. It owns provisional Java source-fact extraction,
stable source-fact IDs and AST diagnostics until accepted by Analysis Store.

The service does not import the current modular-monolith domain, application or
adapter modules. It generates transport classes from `contracts/grpc` inside
its own Gradle project and maps them into service-owned models.

## Contract

The provisional inbound API is `JavaAstAnalysisService.AnalyzeSourceSnapshot`
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
`SYMBOL_RESOLUTION_NOT_CONFIGURED` as a completeness-affecting limitation when
requested.

## Local Commands

```bash
./gradlew :services:java-ast-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:java-ast-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/java-ast-analysis-service/Dockerfile -t java-ast-analysis-service:slice07 .
```
