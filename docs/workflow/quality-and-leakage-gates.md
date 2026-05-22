# Quality And Leakage Gates

## Authoritative Quality Source

`QUALITY.md` is the authoritative quality contract.

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Every slice must also run:

```bash
git diff --check
```

## Slice Gate Summary

| Slice | Gate |
|---|---|
| S00-S02 | Documentation and inventory checks plus `git diff --check`. |
| S03 | Contract checks plus minimum quality command when contract/test/build files change. |
| S04 | Data ownership documentation checks; minimum quality command if persistence code changes. |
| S05-S11 | Targeted service/module tests, minimum quality command and `git diff --check`. |
| S12 | Minimum quality command if Java logging/observability code changes; documentation-only otherwise. |
| S13 | Testbed/service integration tests, minimum quality command and `git diff --check`. |
| S14 | Caller-free searches, full local quality gate and `git diff --check`. |
| S15 | Per-service build commands, full local quality gate and final diff review. |

## Leakage Gates

Service outputs, diagnostics, logs, reports and CLI responses must not expose:

- credentials, tokens, secrets or userinfo in repository URLs;
- local absolute workspace paths;
- private service database details;
- raw stdout or stderr from untrusted processes;
- raw source content unless explicitly requested by an owner API contract;
- raw runtime values unless redaction and retention rules are approved;
- stack traces or internal exception messages in public API responses;
- LLM output as verified evidence.

## Evidence Integrity Gates

- Static JavaParser and Joern facts remain static or semantic evidence.
- Runtime execution facts require observed runtime data.
- Missing evidence is represented as missing, incomplete, unknown or
  unavailable.
- Reports distinguish confirmed evidence, derived analysis, gaps, hypotheses
  and generated content.
- Graph, report, vector and LLM projections do not become sources of truth.

## Dependency Verification

Gradle dependency verification must remain strict. Do not use:

```bash
--dependency-verification off
```

If metadata is missing, update `gradle/verification-metadata.xml` only through
the repository's checksum strategy and rerun the failing command with strict
verification.

## Optional External Checks

Docker, Docker Compose, Joern, Docker Swarm, Kubernetes, Jenkins, Artifactory,
SonarCloud and live LLM checks are optional unless a slice explicitly makes
them required and verifies local prerequisites. A skipped optional check must be
reported as `SKIPPED` with a reason, not as success.
