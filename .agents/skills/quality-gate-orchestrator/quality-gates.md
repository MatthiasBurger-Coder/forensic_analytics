# Quality Gates

## Minimum Quality Command

From `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Quality Gate

From `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Documentation And Governance Checks

Use as targeted checks:

```bash
git diff --check
```

Documentation-only checks do not replace the minimum quality command when claiming commit readiness.

## Quality Dimensions

- Build
- Unit Tests
- Integration Tests
- Contract Tests
- ArchUnit
- Coverage
- Sonar
- Dependency Verification
- Docker Build
- Security Checks
- Documentation Completeness

## Blocking Rules

- Failed required gates block commit and push.
- Missing required gate evidence blocks commit and push.
- Optional external checks may be documented as skipped only when they are not required by `QUALITY.md`, workflow or CI policy.
- `./gradlew clean check` is diagnostic unless the repository wires every required task into `check`.

## D8 And Q11 Mapping

`D8` is the blocking quality and release-readiness decision. It includes failed
build, failed tests, architecture violation, missing required documentation,
missing workflow version and failed required quality gates.

`Q11` is asynchronous execution reporting after a successful checkpoint path.
Q11 is non-blocking by default for commit, push, PR creation and release
preparation. Regulatory or compliance reporting blocks only when the active
workflow explicitly declares it as a D8 requirement.
