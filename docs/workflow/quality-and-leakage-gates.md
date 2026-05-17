# Quality And Leakage Gates

## Minimum Commands

Run from `/mnt/d/Projects/forensic_analytics` in WSL:

```bash
git status --short --branch
git diff --check
git diff --cached --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Gate

Run when feasible:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## validatePlugins Trigger

Run only if Gradle plugin metadata, task inputs, task outputs or plugin
implementation classes changed:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

This workflow is not expected to change plugin code or Gradle plugin metadata.

## Producer Leakage Audit

```bash
rg -n "GenerateBtmTask|BtmGenMojo|btmGen|generateBtmRules|forensics:btmgen|forensics:analyze|RtTraceHelper|RtTrace|MethodLoggingAspect|AspectJ|cleanupPolicy|analysisStoreDirectory|joernExecutable|joernParseExecutable|joernSliceExecutable" docs/epics docs/arc42 docs/adr docs/README.md
```

Allowed matches must be marked as historical reference, external producer
example, explicit exclusion or source comparison notes.

## Product-Scope Audit

```bash
git diff --name-only origin/main...HEAD
```

Blocked changed paths for this workflow:

- `forensic-analytics-*/`
- `services/`
- `forensic-ui/`
- `frontend/`
- `contracts/`
- `deployment/`
- `examples/`
- `data/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/`
- `src/`

## Sensitive-Data Audit

```bash
rg -n "secret|credential|token|password|raw runtime|raw trace|stack trace|LLM prompt|source payload" docs/epics docs/README.md docs/arc42 docs/adr docs/architecture
```

Any match must be reviewed to ensure the documentation protects sensitive
runtime values, source content, stack traces and LLM prompt material.

## Marker Audit

Use a marker scan against changed documentation. The search expression is kept
split here so this file does not create a self-match during simple text scans.

```bash
rg -n '\b(TO''DO|T''BD|FIX''ME|X''XX|PLACE''HOLDER|pend''ing)\b' docs/epics docs/README.md docs/arc42 docs/adr docs/architecture
```

Matches are allowed only when they are historical quoted material or explicitly
documented open decisions.
