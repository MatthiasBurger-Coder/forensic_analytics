# Quality And Leakage Gates

## Authority

`QUALITY.md` is the authoritative quality contract. This workflow may classify
which gates apply, but it must not weaken a required gate or mark a failed
required gate as optional.

## Default Governance-Only Gate

Required for every slice in this workflow:

```bash
git diff --check
```

Required before a slice checkpoint commit during `workflow execute`:

```bash
git diff --cached --check
```

Required when JSON files are created or changed:

```bash
python3 -m json.tool <json-file>
```

## Gradle Gate Escalation

This workflow forbids product source, frontend source, tests, build logic,
contracts and `QUALITY.md` changes by default.

If a future slice unexpectedly touches one of those areas, execution must stop
as a scope conflict unless the workflow is refined. If refined, the minimum
quality command from `QUALITY.md` applies:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

If the refined change affects broad product behavior, build health, package
coverage or final release readiness, the full local gate applies:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Profile Matrix

| Profile | Typical scope | Required checks |
|---|---|---|
| `FAST_PATH` | Documentation-only typo, Mermaid or process text without behavioral change | `git diff --check`, targeted documentation review |
| `NORMAL_PATH` | Isolated governance skill or metadata change without architecture, branch, quality or routing authority change | `git diff --check`, targeted registry/routing checks, JSON validation when applicable |
| `FULL_PATH` | Workflow governance, skill/role structure, branch rules, quality rules, routing, S3D, `.codex` interaction, arc42/ADR impact | `git diff --check`, role review, registry conflict review, arc42/ADR check, slice-specific validation |

## Leakage Gates

Every slice must verify that the changed files remain inside the workflow
scope. Product implementation leakage is blocked.

Forbidden by this workflow:

- Java production source changes;
- Java test source changes;
- React or frontend source changes;
- gRPC/protobuf, OpenAPI or event contract implementation changes;
- Docker, runtime, deployment or persistence changes;
- Gradle build logic or dependency changes;
- analytics behavior changes.

If any of these appear in `git diff --name-status`, execution stops and reports
the scope conflict.

## Required Reporting

For every slice, the execution report records:

- commands executed;
- whether each command passed, failed or was not applicable;
- whether Gradle was required;
- whether any product leakage was detected;
- remaining risk and blocker status.
