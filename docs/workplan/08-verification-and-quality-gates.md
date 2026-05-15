# Verification And Quality Gates

All commands must run from the WSL-mounted repository path on Windows hosts:

```bash
cd /mnt/d/Projects/forensic_analytics
```

Do not claim a command passed unless it was actually executed.

## Frontend Checks

After `forensic-ui` exists:

```bash
cd forensic-ui
npm install
npm run build
```

The current frontend has `test` but no `lint` script:

```bash
npm run test
```

After `package-lock.json` exists, `npm ci` is the repeatable install command.

Prefer adding focused frontend tests for:

- API client timeout and retry behavior;
- no automatic retry for repository-analysis POST;
- DTO-to-domain mapping;
- duplicate submit prevention;
- backend unavailable and stale data rendering;
- polling stop on terminal state.

## Backend Checks

Run the narrowest relevant backend test first. Examples:

```bash
./gradlew :forensic-analytics-application:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-bootstrap:test --dependency-verification strict --console=plain --stacktrace
```

Then run the repository minimum:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate when feasible:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

The implemented REST adapter uses JDK `HttpServer` and Gson. No Spring Boot plugin or Gradle plugin validation is required for this slice. Gson `2.13.2` is declared in `gradle/libs.versions.toml` and verified by strict Gradle dependency verification.

## Docker Checks

After Docker files exist:

```bash
docker build -t forensic-ui:local ./forensic-ui
```

This build was executed successfully for the current slice.

If a compose file exists:

```bash
docker compose config
```

and, when feasible:

```bash
docker compose up --build
```

Do not make Docker-dependent checks part of the default quality gate unless the repository documents that requirement.

## Always Run

Before commit:

```bash
git diff --check
git status --short
```

On Windows-hosted WSL worktrees, verify that status is not polluted by line-ending-only changes before staging.

## Reporting Failures

For each failed command, report:

- exact command;
- failing task or test;
- summary of the failure;
- whether it appears caused by the current change;
- remaining blocker.
