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
git diff --check
```

## Slice Gate Pattern

Each execution slice must run:

1. The targeted service/module tests named in the slice.
2. Caller-free scans for the candidate module or package.
3. `git diff --check`.
4. The repository minimum gate for production Java, tests, Gradle, contracts,
   runtime wiring or deployment changes.
5. The full local gate before S14 and S15 closure.

## Leakage Checks

For service migration slices, run targeted scans proving the service does not
depend on central legacy implementation modules:

```bash
rg -n -P "^import\s+de\.burger\.forensics\.analytics\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\.request|ingestion\.grpc)\b" services -S -g "*.java"
```

For final removal, prove no legacy build references remain:

```bash
git ls-files "*build.gradle.kts" settings.gradle.kts | xargs rg -n "forensic-analytics-(adapter-javaparser|adapter-joern-docker|adapter-repository-source|application|boot-app|bootstrap|cli|domain|engine|ingestion-grpc|ingestion-request|logging|observability|persistence|rest|testbed)"
```

## Evidence Integrity Gates

Execution must stop when a change would:

- treat static analysis as runtime execution evidence;
- hide unresolved symbols or missing trace fields;
- replace observed trace values with inferred values;
- store LLM output as verified evidence;
- collapse confirmed evidence, derived analysis, unresolved gaps and
  hypotheses into one ambiguous field;
- remove the only regression coverage for behavior being retired.

## Failure Routing

| Failure | Route |
|---|---|
| Build failure | Senior DevOps and responsible implementation owner |
| Test failure | Senior Tester and responsible implementation owner |
| Architecture violation | Senior System Architect and Microservice Senior Expert |
| Contract mismatch | Contract-First API Steward and relevant contract specialist |
| Persistence ownership conflict | Senior Analysis Storage Architect and Data Ownership Steward |
| Unknown or repeated failure | Root Architect escalation after maxRetries = 3 |
