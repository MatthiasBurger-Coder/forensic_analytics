# Verified Baseline

## Verification Date

2026-05-15

## Repository Root

The repository root was verified through WSL as:

```text
/mnt/d/Projects/forensic_analytics
```

The matching Windows path is:

```text
D:/Projects/forensic_analytics
```

## Inspected Repository Files

The read-only inspection covered:

- `AGENTS.md`
- `QUALITY.md`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `docs/workplan/`
- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md`
- `docs/arc42/02-architecture-constraints.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/10-quality-requirements.md`
- `docs/adr/ADR-0001-plugins-are-producers.md`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`
- `.agents/roles/`

## Inspected Current Implementation

The read-only implementation inspection covered representative integration points:

- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/ForensicAnalyticsBackendComponents.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/ForensicAnalyticsServerApplication.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/GrpcIngestionServerFactory.java`
- `forensic-analytics-rest/src/main/java/de/burger/forensics/analytics/rest/RepositoryAnalysisHttpHandler.java`
- `forensic-analytics-rest/src/main/java/de/burger/forensics/analytics/rest/RestApiServerFactory.java`
- `forensic-analytics-ingestion-grpc/src/main/java/de/burger/forensics/analytics/ingestion/grpc/ForensicIngestionGrpcService.java`
- `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto`
- `forensic-analytics-cli/src/main/java/de/burger/forensics/analytics/cli/ForensicAnalyticsCli.java`
- `forensic-analytics-application/src/test/java/de/burger/forensics/analytics/application/analysis/quality/AnalysisContractArchitectureTest.java`
- `forensic-analytics-ingestion-grpc/src/test/java/de/burger/forensics/analytics/ingestion/grpc/quality/IngestionGrpcArchitectureTest.java`

## Source Logging Material

The user-referenced file was:

```text
D:/Projects/SCXMLExample/src/main/java/de/burger/it/scxmlexample/infrastructure/logging.zip
```

That ZIP file was not present. The inspected parent directory contained:

```text
bootstrap/
config/
logging/
```

The inspected logging source files were:

- `CentralLoggingAspect.java`
- `LevelLogger.java`
- `LevelLoggerRegistry.java`
- `Loggable.java`
- `correlation/CorrelationIdManager.java`
- `strategy/TraceLevelLogger.java`
- `strategy/DebugLevelLogger.java`
- `strategy/InfoLevelLogger.java`
- `strategy/WarnLevelLogger.java`
- `strategy/ErrorLevelLogger.java`

## Verified Quality Gate

`QUALITY.md` defines the minimum verification command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

`QUALITY.md` defines the full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Repository commands on this Windows host must run through WSL from `/mnt/d/Projects/forensic_analytics`.

## Verified Build Baseline

The repository is a Gradle multi-project build with these included modules:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-cli`
- `forensic-analytics-testbed`
- `forensic-analytics-persistence`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-rest`
- `forensic-analytics-bootstrap`

The root build configures Java 25, JUnit 6, JaCoCo, strict dependency verification, and `checkPackageCoverage`.

`gradle/libs.versions.toml` currently does not define SLF4J, Log4j2, Spring AOP, or AspectJ aliases.

## Verified Architecture Facts

The architecture documentation and tests establish these constraints:

- Domain and application logic must remain independent from frameworks and external tools.
- Inbound adapters map transport DTOs into application commands.
- Runtime data is sensitive by default.
- Ambiguous mappings must be reported, not silently accepted.
- Existing ArchUnit tests already forbid Spring, gRPC, persistence, and selected infrastructure dependencies from application analysis contracts.
- The gRPC adapter must not depend on persistence.

## Current Logging And Correlation State

No existing Forensic Analytics source logging framework usage was found by repository search for logging, SLF4J, Log4j, Logback, Java logging, AspectJ, logger creation, or MDC terms outside generated/build output.

The REST adapter already has a local `X-Correlation-Id` response/request header flow in `RepositoryAnalysisHttpHandler`. That ID is not currently stored in an MDC or shared logging context.

The gRPC protocol has request/session/build identity fields such as `request_id`, `session_id`, `build_id`, `project_id`, and `schema_version`. It does not define a dedicated correlation ID field in the inspected proto.

## Existing Workplan Replacement

The previous active `docs/workplan` described an already executed governance workplan. It was deleted and regenerated for this logging integration plan as required by the repository workplan-authoring rule.
