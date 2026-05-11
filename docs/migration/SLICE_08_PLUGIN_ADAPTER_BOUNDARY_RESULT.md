# Slice 08 Plugin Adapter Boundary Result

Date: 2026-05-10

## Scope Executed

- Updated `forensics_tracing` so Gradle and Maven connectors can build an opt-in engine ingestion request artifact.
- Added build-tool-neutral engine request models under `de.burger.forensics.plugin.btmgen.common`.
- Added deterministic `engine-request.json` writing after successful local BTM generation.
- Kept legacy local BTM generation and analysis-store output as the default behavior.
- Added Gradle extension/task mapping for `engineRequestEnabled` and `engineRequestFile`.
- Added Maven parameter mapping for `forensics.engineRequestEnabled` and `forensics.engineRequestFile`.
- Extended connector capability parity tracking with `ENGINE_REQUEST`.
- Updated `forensics_tracing` README documentation.

## Boundary Decision

Slice 08 implements the "Engine Request" path, not a direct gRPC network client.

This avoids adding an unverified gRPC/protobuf runtime dependency to the build-tool plugin and keeps the adapter boundary explicit. The generated request describes stable payload IDs, payload kinds, content types, and local artifact files for later `forensic_analytics` ingestion.

## Legacy Behavior

Legacy mode remains the default:

- `engineRequestEnabled=false`
- local `.btm` generation still runs as before
- analysis-store artifacts remain controlled by the existing `analysisStoreEnabled` settings
- no network upload happens during Gradle or Maven execution

## Scope Deliberately Not Executed

- No direct gRPC client was added to `forensics_tracing`.
- No build-tool adapter was moved into `forensic_analytics`.
- No plugin functionality was removed.
- No Java, Gradle, Maven, dependency, or baseline version was changed.

## Workplan Alignment

This slice follows Slice 08 from `MIGRATION_WORKPLAN.md`:

- `forensics_tracing` now builds an engine request artifact.
- Legacy mode remains available and is still the default.
- Build-tool adapters remain in `forensics_tracing`.

## Verification

Executed in `D:\Projects\forensics_tracing`:

```text
.\gradlew.bat test --tests "*BtmGenerationRequestTest" --tests "*BtmGenerationRunnerTest" --tests "*EngineIngestionRequestWriterTest" --tests "*BtmGenExtensionTest" --tests "*GenerateBtmTaskTest" --tests "*BtmGenPluginTest" --tests "*MavenBtmGenParametersTest" --tests "*BuildToolConnectorParityTest" --tests "*PluginAdapterArchitectureTest" --dependency-verification strict --console=plain --stacktrace
```

Result:

- Targeted common, Gradle adapter, Maven adapter, parity, and architecture tests passed.

Full repository gate:

```text
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed.

Plugin validation:

```text
.\gradlew.bat validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

Result: passed.

Sonar:

- Skipped because `SONAR_TOKEN` was not set in the local environment.

Executed in `D:\Projects\forensic_analytics` for the result documentation branch:

```text
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed.
