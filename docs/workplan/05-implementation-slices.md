# Implementation Slices

Each slice begins with read-only verification of the files it will touch. Stop instead of guessing if any expected file, task, dependency, or contract cannot be verified.

## Slice 01 - Source And Contract Confirmation

Purpose:

- Confirm whether the missing `logging.zip` can be replaced by the inspected unzipped source package.
- Record any user/project decision about source authority.

Prerequisites:

- User request and this workplan.

Affected files:

- `docs/workplan/00-verified-baseline.md`
- no production code

Owner role:

- Senior Workplan Architect

Allowed write scope:

- workplan updates only

Dependencies:

- none

Parallelization status:

- blocking

Done criteria:

- source authority is explicit
- no implementation depends on a missing ZIP file

Verification commands:

```bash
test -d /mnt/d/Projects/SCXMLExample/src/main/java/de/burger/it/scxmlexample/infrastructure/logging
```

Stop conditions:

- the ZIP file is required but still missing
- source files differ from the inspected source package

## Slice 02 - Module And Dependency Decision

Purpose:

- Decide whether logging lives in a new observability module or inside existing adapter modules.
- Decide whether `slf4j-api` is required or JDK logging is sufficient.

Prerequisites:

- Slice 01 complete.

Affected files:

- `settings.gradle.kts` if a module is added
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/verification-metadata.xml` only if a dependency is accepted and strict verification requires metadata
- optional new module build file, if selected

Owner role:

- Senior System Architect
- Senior DevOps

Allowed write scope:

- Gradle/module metadata needed for the selected dependency boundary

Dependencies:

- Slice 01

Parallelization status:

- blocking for implementation slices

Done criteria:

- selected module/package ownership is documented
- no concrete logging provider is added
- dependency verification impact is known

Verification commands:

```bash
./gradlew help --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- dependency verification cannot resolve the selected dependency
- adding the dependency would leak into domain or application
- a concrete logging provider is introduced transitively without explicit approval

## Slice 03 - Correlation Context Foundation

Purpose:

- Implement a small, testable correlation context based on the source `CorrelationIdManager` ownership idea.

Prerequisites:

- Slice 02 complete.

Affected files:

- selected observability package/module under `src/main/java`
- matching tests under `src/test/java`

Owner role:

- Senior Java Backend

Allowed write scope:

- correlation context implementation and tests only

Dependencies:

- Slice 02

Parallelization status:

- blocking for REST, gRPC, CLI, and bootstrap integration

Done criteria:

- generated IDs are explicit
- existing IDs can be preserved
- nested scopes do not clear outer context
- context cleanup is deterministic
- missing correlation is represented as absent or generated, not guessed from unrelated fields

Verification commands:

```bash
./gradlew :<selected-observability-module>:test --dependency-verification strict --console=plain --stacktrace
```

If no new module is created, replace the module selector with the selected adapter module test command.

Stop conditions:

- MDC or context behavior cannot be tested deterministically
- async/executor propagation is required but ownership is unclear

## Slice 04 - Sanitized Operation Logger

Purpose:

- Adapt the portable level strategy idea without parameter/result logging.

Prerequisites:

- Slice 03 complete.

Affected files:

- selected observability package/module under `src/main/java`
- matching tests under `src/test/java`

Owner role:

- Senior Java Backend
- Senior Security Sandbox Engineer

Allowed write scope:

- logging facade, event model, level strategies, sanitization tests

Dependencies:

- Slice 03

Parallelization status:

- can proceed before adapter slices once Slice 03 is complete

Done criteria:

- entry, completion, and failure events use deterministic field names
- sensitive values are excluded or sanitized
- trace/debug argument/result logging is not implemented
- exception logs preserve category and cause without exposing raw stack traces by default

Verification commands:

```bash
./gradlew :<selected-observability-module>:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- source behavior requires logging args/results to be considered complete
- sanitizer scope conflicts with existing `DiagnosticSanitizer`

## Slice 05 - REST Adapter Integration

Purpose:

- Integrate correlation scope and sanitized operation logging into REST request handling.

Prerequisites:

- Slices 03 and 04 complete.

Affected files:

- `forensic-analytics-rest/src/main/java/de/burger/forensics/analytics/rest/RepositoryAnalysisHttpHandler.java`
- `forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/rest/RepositoryAnalysisRestApiTest.java`
- optional REST-specific helper tests

Owner role:

- Senior Java Backend
- Senior Tester

Allowed write scope:

- REST adapter logging and tests only

Dependencies:

- Slice 03
- Slice 04

Parallelization status:

- can run in parallel with Slice 06 after shared logging APIs are stable

Done criteria:

- current `X-Correlation-Id` response behavior is preserved
- generated correlation IDs are covered by tests
- failure responses remain sanitized
- request body and raw diagnostics are not logged

Verification commands:

```bash
./gradlew :forensic-analytics-rest:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- REST behavior changes outside logging/correlation scope
- existing REST tests reveal response contract drift

## Slice 06 - gRPC Adapter Integration

Purpose:

- Add adapter-level logging to gRPC service methods without changing proto contracts.

Prerequisites:

- Slices 03 and 04 complete.

Affected files:

- `forensic-analytics-ingestion-grpc/src/main/java/de/burger/forensics/analytics/ingestion/grpc/ForensicIngestionGrpcService.java`
- `forensic-analytics-ingestion-grpc/src/test/java/de/burger/forensics/analytics/ingestion/grpc/ForensicIngestionGrpcServiceTest.java`
- `forensic-analytics-ingestion-grpc/src/test/java/de/burger/forensics/analytics/ingestion/grpc/quality/IngestionGrpcArchitectureTest.java`

Owner role:

- Senior gRPC/Protobuf Specialist
- Senior Java Backend
- Senior Tester

Allowed write scope:

- gRPC adapter logging and tests only

Dependencies:

- Slice 03
- Slice 04

Parallelization status:

- can run in parallel with Slice 05 after shared logging APIs are stable

Done criteria:

- gRPC status mapping remains unchanged
- no persistence dependency is introduced
- no proto field is repurposed as a correlation ID
- streaming upload logs do not expose payload bytes

Verification commands:

```bash
./gradlew :forensic-analytics-ingestion-grpc:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- correlation requirements imply a proto or metadata contract change
- streaming error behavior changes

## Slice 07 - CLI And Bootstrap Integration

Purpose:

- Add command/server lifecycle logs around CLI and server bootstrap boundaries.

Prerequisites:

- Slices 03 and 04 complete.

Affected files:

- `forensic-analytics-cli/src/main/java/de/burger/forensics/analytics/cli/ForensicAnalyticsCli.java`
- `forensic-analytics-cli/src/test/java/de/burger/forensics/analytics/cli/ForensicAnalyticsCliTest.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/ForensicAnalyticsServerApplication.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/GrpcIngestionServerFactory.java`
- `forensic-analytics-bootstrap/src/test/java/de/burger/forensics/analytics/bootstrap/ForensicAnalyticsServerApplicationTest.java`

Owner role:

- Senior Java Backend
- Senior Tester

Allowed write scope:

- CLI/bootstrap logging and tests only

Dependencies:

- Slice 03
- Slice 04

Parallelization status:

- can run in parallel with Slice 05 and Slice 06 if write scopes remain disjoint

Done criteria:

- CLI stdout/stderr behavior remains unchanged
- server startup/shutdown behavior remains unchanged
- logs do not expose environment secrets or local paths

Verification commands:

```bash
./gradlew :forensic-analytics-cli:test :forensic-analytics-bootstrap:test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- logging changes command output contract
- bootstrap failure handling changes

## Slice 08 - Architecture Guardrails

Purpose:

- Add or update architecture tests that enforce the selected logging boundary.

Prerequisites:

- Slices 02 through 07 complete or planned APIs stable.

Affected files:

- architecture tests in affected modules
- optional new quality test in selected observability module

Owner role:

- Senior System Architect
- Senior Tester

Allowed write scope:

- ArchUnit or equivalent architecture tests only

Dependencies:

- Slice 02
- adapter integration slices

Parallelization status:

- can start after selected module/package names are stable

Done criteria:

- domain/application remain independent from logging infrastructure
- adapters use only approved logging APIs
- no Spring AOP or AspectJ dependency is introduced accidentally

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- existing architecture rules conflict with the selected design
- rule changes would weaken current boundaries without justification

## Slice 09 - Optional Aspect/Annotation Decision

Purpose:

- Decide whether to preserve the source `@Loggable`/aspect model after baseline logging works.

Prerequisites:

- Slices 03 through 08 complete.
- Explicit request to evaluate annotation-driven logging.

Affected files:

- architecture docs
- ADRs
- build files only if Spring AOP or AspectJ is accepted
- adapter/infrastructure code only

Owner role:

- Senior System Architect
- Senior Java Backend
- Senior Documentation Engineer

Allowed write scope:

- decision record and dedicated implementation files only

Dependencies:

- Slice 08

Parallelization status:

- not parallelizable with baseline adapter integration

Done criteria:

- decision is documented
- no annotation is used in domain/application
- tests prove aspect behavior if adopted

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Spring/AspectJ adoption lacks explicit approval
- dependency verification cannot be satisfied
- proxy behavior makes evidence/control flow unclear

## Slice 10 - Documentation Synchronization

Purpose:

- Update architecture and quality documentation to match the implemented logging boundary.

Prerequisites:

- Implementation slices complete.

Affected files:

- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/10-quality-requirements.md`
- optional ADR for logging boundary decision
- `README.md` or `docs/README.md` only if public usage changes

Owner role:

- Senior Documentation Engineer
- Senior System Architect

Allowed write scope:

- documentation only

Dependencies:

- implemented behavior

Parallelization status:

- can begin after APIs and boundaries are stable

Done criteria:

- docs describe actual implemented behavior
- no docs claim logs are forensic evidence
- quality commands remain aligned with `QUALITY.md`

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- documentation would need to describe behavior that was not implemented
- ADR decision cannot be stated from verified changes

## Slice 11 - Full Quality Gate

Purpose:

- Verify the integrated change under the repository quality contract.

Prerequisites:

- Slices 01 through 10 complete as applicable.

Affected files:

- none, unless failures require a dedicated repair slice

Owner role:

- Senior Tester

Allowed write scope:

- none during verification

Dependencies:

- all implementation and documentation slices

Parallelization status:

- final blocking gate

Done criteria:

- minimum quality command passes
- full local quality gate passes or failure is reported with cause and blocker

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- quality gate fails
- failures cannot be attributed safely
- broad line-ending-only changes appear before commit preparation
