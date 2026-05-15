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

If configured:

```bash
npm run test
npm run lint
```

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

If Spring Boot or another REST framework plugin/dependency is added, verify dependency verification metadata and any new plugin validation requirements from the actual build files.

## Docker Checks

After Docker files exist:

```bash
docker build -t forensic-ui:local ./forensic-ui
```

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
