# Cross-Repo Handoff Smoke

Date: 2026-05-11

## Purpose

This smoke verifies the local handoff from `forensics_tracing` to `forensic_analytics` by generating an `engine-request.json` in the tracing repository and importing it through the analytics CLI.

This smoke is not part of the standard local quality gate because it depends on two repositories and on the producer repository exposing the required Gradle task.

## Prerequisites

- `forensics_tracing` is checked out on the intended source branch.
- `forensic_analytics` is checked out on the intended target branch.
- The tracing checkout can generate an engine request without requiring Docker, Joern, gRPC upload, or external services.
- The analytics checkout can run `:forensic-analytics-cli:run`.

Use environment-specific paths instead of hard-coding local checkout locations:

```powershell
$env:FORENSICS_TRACING_REPO = "<path-to-forensics_tracing>"
$env:FORENSIC_ANALYTICS_REPO = "<path-to-forensic_analytics>"
$env:ENGINE_REQUEST_FILE = "build\forensics\engine-request.json"
$env:HANDOFF_OUTPUT_DIR = "build\forensics\handoff-smoke"
```

## Producer Command

Run in `forensics_tracing`:

```powershell
.\gradlew.bat generateBtmRules `
  -Pforensics.engineRequestEnabled=true `
  -Pforensics.engineRequestFile=$env:ENGINE_REQUEST_FILE `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

Expected producer output:

```text
build/forensics/engine-request.json
```

## Consumer Command

Run in `forensic_analytics` after the producer command succeeds:

```powershell
.\gradlew.bat :forensic-analytics-cli:run `
  --args="ingest-request --request $env:FORENSICS_TRACING_REPO\$env:ENGINE_REQUEST_FILE --output $env:HANDOFF_OUTPUT_DIR" `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

Expected consumer output:

```text
build/forensics/handoff-smoke/engine-request-import-summary.txt
```

Expected summary fields:

```text
status=COMPLETED
uploadedPayloads>=1
```

## Result On 2026-05-11

The smoke is blocked before analytics import.

Observed producer state:

- `forensics_tracing` checkout was on `main`.
- The checkout had local uncommitted changes, so it was not a pristine `main` verification.
- Source inspection found the engine request model, writer, payload kinds, and Gradle/Maven configuration fields.
- The documented root Gradle task `generateBtmRules` was not available.

Commands executed:

```powershell
.\gradlew.bat generateBtmRules -Pforensics.engineRequestEnabled=true -Pforensics.engineRequestFile=build\forensics\engine-request.json --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat tasks --all --console=plain | Select-String -Pattern 'generateBtm|btm|Btm'
```

Failure summary:

```text
Task 'generateBtmRules' not found in root project 'forensics-tracing'.
```

The task listing showed only BTM-related publication marker tasks, not a runnable `generateBtmRules` task.

Because no producer request was generated, the analytics CLI import command was not executed for the real cross-repo smoke.

## Known Limits

- This document does not introduce a direct compile dependency on `forensics_tracing`.
- This document does not require Docker, Joern, gRPC, or external services.
- This document does not treat stale or synthetic request files as verified producer output.
