# Quality And Leakage Gates

## Workflow Creation Gate

Run after regenerating `docs/workflow`:

```bash
git status --short --branch
git diff --stat
git diff --name-status
git diff --check
```

## Minimum Repository Gate

From `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Gate

From `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Service-Local Gate Template

Use after service-local implementation changes:

```bash
./gradlew :services:<service-name>:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:<service-name>:jacocoTestReport :services:<service-name>:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew :services:<service-name>:bootJar --dependency-verification strict --console=plain --stacktrace
```

## Contract Leakage Checks

Use after contract or service-boundary changes:

```bash
rg -n "project\\(" services/*/build.gradle.kts
for service in services/*-service; do
    package_root=$(find "$service/src/main/java/de/burger/forensics/analytics/services" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null | head -n 1)
    [ -n "$package_root" ] || continue
    rg -n "import de\\.burger\\.forensics\\.analytics\\.services\\." "$service/src/main/java" "$service/src/test/java" 2>/dev/null | rg -v "services\\.$package_root\\." || true
done
rg -n "[f]ile:/|/(mnt|home)/|(^|[^A-Za-z])[A-Za-z]:[\\/]" contracts docs/workflow docs/architecture --glob "!docs/workflow/quality-and-leakage-gates.md"
rg -n "(?i)(api[_-]?key|private[_-]?key|bearer [A-Za-z0-9]|password\\s*[:=]|token\\s*[:=]|secret\\s*[:=])" docs/workflow docs/architecture --glob "!docs/workflow/quality-and-leakage-gates.md"
```

The first command should remain empty for service build files. The second
command must be reviewed for direct cross-service implementation imports and
must not report imports from another service package root.
The third command must remain empty for forbidden local path leakage. The
fourth command must remain empty for obvious inline secret assignments.

## Contract-Test Stop Rule

If a slice changes `contracts/**` and the repository still has no executable
service-level contract-test task for that contract type, the slice must add the
contract-test command and execute it before claiming readiness.
`docs/contracts/contract-test-plan.md` records that service-level contract test
tasks are not yet generally available.

## Frontend Gate

After frontend API-adapter changes in `forensic-ui`, run:

```bash
cd forensic-ui && npm ci && npm test && npm run build
```

Stop before moving work into `frontend/frontend-web-app` unless that root has
package tooling and verified test/build commands.

## Slice Checkpoint Gate

Before every `workflow execute` slice checkpoint commit:

```bash
git status --short --branch
git diff --stat
git diff --name-status
git diff --cached --check
git diff --cached
```

Review untracked files explicitly. `git diff --check` does not inspect new
untracked files until they are staged, so staging must be deliberate and
slice-scoped.

## Evidence Integrity Checks

Review affected output for these forbidden claims:

- static facts presented as runtime execution;
- Joern semantic edges presented as observed runtime calls;
- generated BTM files presented as observed evidence;
- LLM output presented as confirmed facts;
- missing facts silently converted into complete results;
- workspace paths exposed outside Repository Analysis;
- artifact references without checksums or provenance.

## Docker And Deployment Gates

Only run Docker, Swarm or Kubernetes checks after the corresponding files
exist in the slice. Do not claim deployment readiness from contract files or
README text alone.

## Failure Reporting

Every failed gate must report:

- command executed;
- failing task or test;
- concise failure summary;
- whether the failure is caused by the current slice;
- remaining blocker and responsible owner.
