# Quality And Leakage Gates

## Required Baseline

`QUALITY.md` is the authoritative quality contract.

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Every slice must also run:

```bash
git diff --check
```

## Leakage Rules

Gateway, CLI, E2E tests and WildFly reports must not expose:

- credentials, tokens, authorization headers or secret-like values;
- private workspace paths in public Gateway or CLI outputs;
- raw stdout or raw stderr from Git, Docker, service runtimes or workers;
- raw exception stack traces in public contracts;
- private repository checkout paths;
- unresolved facts as confirmed evidence.

## Slice-Specific Gates

| Slice | Required gate |
|---|---|
| S01 | Targeted real repository E2E test and `:forensic-analytics-testbed:test`. |
| S02 | Targeted WildFly hardening test proving opt-in skip by default; optional external run only with prerequisites. |
| S03 | Gateway OpenAPI contract test, CLI contract test and contract leakage checks. |
| S04 | Documentation-only `git diff --check`; no deployment readiness claim. |
| S05 | Caller inventory commands and documentation diff check. |
| S06 | CLI tests, Gateway service tests and repository minimum quality command. |
| S07 | Repository minimum command; full local gate if any module or build registration changes. |
| S08 | Final repository minimum command and documentation diff check. |

## Optional External Checks

WildFly, Docker image build, Docker Compose runtime startup, Docker Swarm and
Kubernetes checks are optional unless the slice changes those runtime behaviors
or documentation makes them required. Skipped optional checks must be reported
as `SKIPPED` with the concrete reason.
