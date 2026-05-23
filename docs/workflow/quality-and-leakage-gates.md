# Quality And Leakage Gates

## Authoritative Quality Source

`QUALITY.md` is authoritative for all workflow execution quality decisions.

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Workflow Creation Gate

Workflow creation is documentation-only. Required verification:

```bash
python3 -m json.tool docs/workflow/context-pack.json
git diff --check
```

## Project Model Gate

The service-only project model must remain true:

```bash
./gradlew projects --dependency-verification strict --console=plain --stacktrace
```

The project listing must not include any `forensic-analytics-*` Gradle project.

## Active Build Leakage Gate

No active build file outside a deleted legacy source tree may depend on a
legacy Gradle project:

```bash
git ls-files "*build.gradle.kts" | grep -v "^forensic-analytics-" | xargs -r rg -n 'project\(\":forensic-analytics-'
```

This command is expected to produce no matches. A match is a blocker.

## Active Service Source Leakage Gate

No active Java source outside deleted legacy source trees may import legacy
monolith packages:

```bash
git ls-files "*.java" \
  | grep -v "^forensic-analytics-" \
  | xargs -r rg -n -P '^import\s+de\.burger\.forensics\.analytics\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\.request|ingestion\.grpc)\b'
```

This command is expected to produce no matches. A match is a blocker unless a
role review proves the match is generated historical text outside active source.

## Legacy Source Tree Removal Gate

Before S04 deletion:

```bash
git ls-files "forensic-analytics-*"
```

The command records deletion candidates.

After S04 deletion:

```bash
git ls-files "forensic-analytics-*"
```

The command must return no tracked files unless an ADR-backed retained purpose
is documented before closure.

## Targeted Service Test Matrix

| Legacy area removed | Targeted tests |
|---|---|
| Repository source | `./gradlew :services:repository-source-service:test :services:repository-analysis-service:test --dependency-verification strict --console=plain --stacktrace` |
| Ingestion and engine request | `./gradlew :services:ingestion-service:test :services:forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace` |
| JavaParser adapter | `./gradlew :services:java-parser-analysis-service:test :services:java-ast-analysis-service:test --dependency-verification strict --console=plain --stacktrace` |
| Joern adapter | `./gradlew :services:joern-analysis-service:test :services:joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace` |
| Application, domain, engine and persistence | `./gradlew :services:analysis-orchestrator-service:test :services:analysis-store-service:test :services:btm-generation-service:test --dependency-verification strict --console=plain --stacktrace` |
| REST, boot, bootstrap and CLI | `./gradlew :services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test --dependency-verification strict --console=plain --stacktrace` |
| Logging, observability and testbed | `./gradlew :services:observability-stack:test :services:testbed:test --dependency-verification strict --console=plain --stacktrace` |

## Frontend Impact Gate

Only run when a slice changes public API fields, endpoints, response status
shapes or `forensic-ui` API mappers:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

## Evidence Integrity Gates

Execution must stop when a change would:

- treat static source facts as runtime execution evidence;
- hide unresolved symbols or missing trace fields;
- replace observed trace values with inferred values;
- store LLM output as verified evidence;
- collapse confirmed evidence, derived analysis, unresolved gaps and
  hypotheses into one ambiguous field;
- remove the only regression coverage for behavior still claimed as supported;
- claim service runtime, Docker, healthcheck, Swarm or Kubernetes readiness
  without verified repository commands and artifacts.

## Failure Routing

| Failure | Route |
|---|---|
| Build failure | Senior DevOps and responsible implementation owner |
| Test failure | Senior Tester and responsible implementation owner |
| Architecture violation | Senior System Architect and Microservice Senior Expert |
| Contract mismatch | Contract-First API Steward and relevant contract specialist |
| Persistence ownership conflict | Senior Analysis Storage Architect and Data Ownership Steward |
| Unknown or repeated failure | Root Architect escalation after maxRetries = 3 |
