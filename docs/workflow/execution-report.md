# Execution Report

## Current Status

Status: S07 completed; final quality and release-readiness closure passed.

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-final-legacy-source-retirement-20260523-v2` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Process strand | `workflow execute` |
| Last update | `2026-05-23` |

## Workflow Creation Evidence

Read-only checks performed during workflow creation:

```bash
git rev-parse --show-toplevel
git status --short
git show-ref --verify --quiet refs/heads/architecture/workflow-legacy-module-retirement-20260522
git branch --show-current
./gradlew projects --dependency-verification strict --console=plain --stacktrace
git ls-files "forensic-analytics-*" | wc -l
rg -n 'project\(\":forensic-analytics-' settings.gradle.kts build.gradle.kts services --glob '*.gradle.kts' --glob '!**/build/**'
git ls-files "*.java" | grep -v "^forensic-analytics-" | xargs -r rg -n -P '^import\s+de\.burger\.forensics\.analytics\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\.request|ingestion\.grpc)\b'
```

Verified results:

- Repository root is `/mnt/d/Projects/forensic_analytics`.
- Active branch is `architecture/workflow-legacy-module-retirement-20260522`.
- Working tree was clean before workflow regeneration.
- Gradle project listing passed and listed only `services:*` projects.
- `450` tracked files remain under `forensic-analytics-*`.
- Active Gradle build leakage scan found no `project(":forensic-analytics-*")`
  references outside legacy source trees.
- Active Java source leakage scan found no legacy monolith imports outside
  legacy source trees.

## Subagent Review Summary

| Role | Result |
|---|---|
| Senior Requirement Engineer | READY_FOR_WORKFLOW with docs-drift and data-ownership stop conditions. |
| Senior System Architect | Stale workflow/architecture docs are the main blocker; Gradle deregistration is already complete. |
| Senior Java Backend Developer | Legacy directories are orphaned source trees; deletion can be one final source-tree removal slice after docs and gates. |
| Senior React Frontend Developer | No direct frontend impact; `forensic-ui` uses public Gateway API only. |
| Senior Tester | Replace stale legacy-module gates with service/root gates and run full `QUALITY.md` gate before closure. |

## S00 Execution Preflight

Status: completed.

Responsible role: Senior Execution Orchestrator with Senior System Architect
and Senior Tester review.

Executed commands:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/architecture/workflow-legacy-module-retirement-20260522
git status --short --branch
python3 -m json.tool docs/workflow/context-pack.json
./gradlew projects --dependency-verification strict --console=plain --stacktrace
git diff --check
git ls-files "forensic-analytics-*" | wc -l
git ls-files "*build.gradle.kts" | grep -v "^forensic-analytics-" | xargs -r rg -n 'project\(\":forensic-analytics-'
git ls-files "*.java" | grep -v "^forensic-analytics-" | xargs -r rg -n -P '^import\s+de\.burger\.forensics\.analytics\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\.request|ingestion\.grpc)\b'
```

Results:

- Active branch is `architecture/workflow-legacy-module-retirement-20260522`.
- Local workflow branch ref exists.
- Working tree was clean at S00 start.
- `docs/workflow/context-pack.json` is valid JSON.
- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`
  passed and listed only `services:*` projects.
- `git diff --check` passed.
- Active build leakage scan found no non-legacy
  `project(":forensic-analytics-*")` references.
- Active Java source leakage scan found no legacy monolith imports outside
  legacy source trees.
- `git ls-files "forensic-analytics-*" | wc -l` returned `450`; this is the
  expected pre-S05 deletion baseline after the v2 topology correction.

Subagent reviews:

- Senior Swarm Orchestrator: READY. S00 metadata is complete. Version 2 keeps
  the dependency graph acyclic with topological groups
  `S00 | S01 | S02+S03 | S04 | S05 | S06 | S07` after the pre-deletion
  documentation stopper was discovered during S04 preflight.
- Senior System Architect: READY. No S00 architecture blocker; arc42 has known
  stale legacy references that belong to S01, S02, S04 and S06.
- Senior Tester: READY. S00 gates are sufficient for preflight only; later
  deletion and release slices still require their targeted and full gates.

S3D note:

- S02 and S03 both lock `docs/testing/**`. They must not run in parallel unless
  a later S3D pass refines locks or serializes them. This does not block S00.

## S01 Legacy Reference Classification

Status: completed.

Responsible role: Senior System Architect with Senior Requirement Engineer,
Senior DevOps and Senior Tester review.

Changed files:

- `docs/architecture/legacy-reference-classification.md`
- `docs/workflow/execution-report.md`

Executed commands:

```bash
git status --short --branch
rg -n "forensic-analytics-" docker .dockerignore contracts docs --glob "!docs/workflow/**"
rg -n "forensic-analytics-" docker .dockerignore contracts docs --glob "!docs/workflow/**" | wc -l
git diff --check
```

Results:

- S01 reference scan completed.
- Focused scan found `283` matches before classification and `298` matches
  after adding the classification artifact.
- `git diff --check` passed.
- No active non-legacy Gradle build reference or service Java import blocker
  was reintroduced.

Subagent reviews:

- Senior Requirement Engineer: READY. S01 remains aligned with FA-MSA-001 and
  ADR-0017. Source-tree deletion is not S01 work.
- Senior System Architect: READY for classification. Physical deletion closure
  remains blocked until active-blocker references are removed or rewritten.
- Senior DevOps: READY. S02 cleanup files are `.dockerignore`,
  `docker/boot-app/Dockerfile`, `docker/boot-app/README.md`, `docs/README.md`,
  `docs/testing/wildfly-hardening.md` and
  `docs/contracts/contract-test-plan.md`.
- Senior Tester: BLOCKED for S03/deletion readiness, not for S01
  classification. Stale legacy Gradle task commands and current-state claims
  must be replaced or marked historical before later slices pass.

Classification summary:

- Removable runtime/build documentation: README, Boot Docker files,
  `.dockerignore`, WildFly hardening commands and legacy REST contract-test
  command.
- Historical architecture baseline: current-state, current-build/test,
  current-coupling, monolith-retirement, service-boundary, migration-map,
  arc42 and related architecture docs.
- Compatibility vocabulary: Gateway/OpenAPI/CLI/gRPC predecessor wording and
  ADR history.
- Product/runtime namespace: `forensic-analytics-joern` and
  `forensic-analytics-workspaces` are not legacy Gradle source-tree references.
- Active blockers: runnable-looking `:forensic-analytics-*` commands, current
  claims that legacy modules are registered or active quality-gate
  participants, and rollback/regression claims that depend only on source trees
  planned for deletion.

S01 handoff:

- S02 must clean stale executable runtime/Docker/contract docs.
- S03 must use service-local gates only and confirm replacement or deprecation
  coverage.
- S04 must clear active service/deployment documentation blockers before
  deletion.
- S06 must reconcile architecture and arc42 current-state claims.
- `docs/skill-audit/README.md` contained a stale historical audit sentence.
  S06 expands its file scope to `docs/skill-audit/**` only for that baseline
  wording so the final closure does not leave a contradictory current-state
  claim outside architecture docs.

## S02 Runtime, Docker And Contract Documentation Cleanup

Status: completed.

Responsible role: Senior DevOps with Senior System Architect, contract
governance, Senior React Frontend and Senior Tester review.

Changed files:

- `.dockerignore`
- `docker/boot-app/Dockerfile`
- `docker/boot-app/README.md`
- `docs/README.md`
- `docs/contracts/contract-test-plan.md`
- `docs/testing/wildfly-hardening.md`
- `docs/workflow/execution-report.md`

Executed commands:

```bash
git diff --check
rg -n ":forensic-analytics-" .dockerignore docker/boot-app docs/README.md docs/testing/wildfly-hardening.md docs/contracts/contract-test-plan.md
rg -n "forensic-analytics-boot-app|BOOT_APP_JAR|bootJar|build/libs/forensic-analytics-boot-app" .dockerignore docker/boot-app docs/README.md
./gradlew :services:testbed:test --tests "*WildFlyRepositoryHardeningTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test --tests "*GatewayOpenApiContractTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :services:btm-generation-service:generateProto --dependency-verification strict --console=plain --stacktrace
./gradlew :services:btm-generation-service:test --tests "*BtmGenerationContractTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Results:

- `git diff --check` passed.
- No stale executable `:forensic-analytics-*` Gradle command remains in the
  S02 target documentation scope.
- No stale Boot app jar, `BOOT_APP_JAR`, `bootJar`, or deleted boot-app build
  output reference remains in `.dockerignore`, `docker/boot-app`, or
  `docs/README.md`.
- `:services:testbed:test --tests "*WildFlyRepositoryHardeningTest"` passed.
- `:services:query-report-api-service:test --tests "*GatewayOpenApiContractTest"`
  passed.
- `:services:btm-generation-service:generateProto` passed.
- `:services:btm-generation-service:test --tests "*BtmGenerationContractTest"`
  passed.
- `:services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test`
  passed.
- The repository minimum gate
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`
  passed.

Subagent and role reviews:

- Senior DevOps: READY. Remove only the legacy Boot jar allowlist, delete the
  stale Boot Dockerfile, retire the Boot container README, and replace
  legacy REST/testbed commands with service-local checks.
- Senior System Architect: READY. S02 may remove or reword executable legacy
  runtime references without public API shape changes or ADR history rewrites.
- Contract Governance: READY. Updating only
  `docs/contracts/contract-test-plan.md` to use the service-local
  `GatewayOpenApiContractTest` command is behavior-neutral; public REST,
  CLI and gRPC contract files remain unchanged.
- Senior React Frontend: READY. No `forensic-ui` changes are required because
  OpenAPI paths, DTO fields, error envelopes and status shapes are unchanged.
- Senior Tester: initial BLOCKED because the cleanup had not yet been applied;
  targeted service verification passed and the blocker was documentation
  state, not failing tests.

S02 handoff:

- S03 completed first to resolve the stale service-testbed minimum-gate
  blocker that surfaced during S02 checkpoint readiness.
- S04 must first clear the remaining active service and deployment
  documentation command blockers. S05 may delete the legacy source trees only
  after S02, S03 and S04 are checkpointed.

Rollback reference:

- Revert the S02 checkpoint commit before S05 if runtime, Docker or
  contract-test documentation cleanup must be withdrawn.

arc42Updated: completed in S06
adrUpdated: checked; no ADR update required because no public contract shape,
runtime ownership or deployment behavior changed in S02.

## S03 Service Regression Coverage Confirmation

Status: completed.

Responsible role: Senior Tester with Senior Java Backend, Microservice Senior
Expert and Senior DevOps review.

Changed files:

- `services/testbed/README.md`
- `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/RepositoryAnalysisMiniEndToEndTest.java`
- `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/RepositoryAnalysisRealRepositoryEndToEndTest.java`
- `docs/workflow/execution-report.md`

Executed commands:

```bash
./gradlew :services:testbed:test --tests de.burger.forensics.analytics.services.testbed.RepositoryAnalysisRealRepositoryEndToEndTest --dependency-verification strict --console=plain --stacktrace
./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:repository-source-service:test :services:repository-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:ingestion-service:test :services:forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:java-parser-analysis-service:test :services:java-ast-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:joern-analysis-service:test :services:joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:analysis-orchestrator-service:test :services:analysis-store-service:test :services:btm-generation-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:observability-stack:test :services:testbed:test --dependency-verification strict --console=plain --stacktrace
git diff --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Results:

- The initial repository minimum gate failure was reproduced as
  `:services:testbed:test` /
  `RepositoryAnalysisRealRepositoryEndToEndTest.realRepositoryFixtureBehaviorIsRetainedOnlyAsLegacyRollbackEvidence`.
- The failure was caused by a stale service-testbed assertion against old S17
  workflow text in `docs/workflow/workflow.md`, not by S02 runtime or contract
  documentation changes.
- `services/testbed` now describes predecessor testbed coverage as
  service-root regression evidence and historical rollback evidence pending
  deletion, not as an active legacy Gradle module or current quality-gate
  participant.
- Testbed assertions now pin the active S03 service-regression coverage
  wording and keep the unsupported local/file repository input and no-parity
  guards.
- All S03 targeted service test bundles passed.
- `git diff --check` passed.
- The repository minimum gate
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`
  passed.

Subagent and role reviews:

- Senior Tester: READY_TO_FIX. Retarget stale S17 assertions to active S03
  workflow wording and rerun `:services:testbed:test` plus the minimum gate.
- Senior Java Backend: READY. The fix is backend-neutral when limited to
  `services/testbed` README and test assertions and does not change
  production service behavior.
- Microservice Senior Expert: BLOCKED until unsafe README wording is removed.
  The service-local structure is acceptable, but legacy modules must not be
  described as active or current quality-gate participants.
- Senior DevOps: READY with `PRODUCT_BUILD_AFFECTING` classification because
  test code changed. S03 requires targeted service gates and the repository
  minimum gate; the full local gate remains the S07 release-readiness gate.

S03 handoff:

- S02 checkpoint can be resumed after S03 because the minimum gate blocker was
  removed.
- S04 must clear active service and deployment documentation command blockers.
  S05 may delete the legacy source trees only after S02, S03 and S04 are
  checkpointed and deletion prechecks still prove no legacy module-local test
  is the only known coverage for behavior still claimed as supported.

Rollback reference:

- Revert the S03 checkpoint commit before S05 if service-regression coverage
  confirmation must be withdrawn.

arc42Updated: completed in S06
adrUpdated: checked

## Workflow Topology Correction

Status: applied in workflow version
`fa-msa-001-final-legacy-source-retirement-20260523-v2`.

Reason:

- S04 preflight found stale runnable legacy Gradle commands and active/current
  legacy evidence wording in service and deployment documentation.
- Deleting source trees before cleaning those references would leave active
  documentation pointing at non-existent Gradle tasks.

Correction:

- New S04: Legacy Command Documentation Stopper Cleanup.
- Former S04 source-tree deletion moved to S05.
- Former S05 architecture documentation and ADR closure moved to S06.
- Former S06 final quality gate and release readiness moved to S07.

Corrected dependency graph:

```text
S00 -> S01
S01 -> S02
S01 -> S03
S02 + S03 -> S04
S04 -> S05
S05 -> S06
S06 -> S07
```

## S04 Legacy Command Documentation Stopper Cleanup

Status: completed.

Responsible role: Senior Documentation Engineer with Senior DevOps, Senior
System Architect, Senior Tester and Microservice Runtime Readiness review.

Changed files:

- `services/analysis-orchestrator-service/README.md`
- `services/joern-analysis-service/README.md`
- `services/README.md`
- `docs/architecture/current-build-and-test-map.md`
- `docs/architecture/current-coupling-map.md`
- `docs/architecture/legacy-reference-classification.md`
- `docs/architecture/monolith-caller-retirement-plan.md`
- `docs/architecture/monolith-runtime-isolation.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-migration-map.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/skill-audit/README.md`
- `docs/workflow/workflow.md`
- `docs/workflow/quality-and-leakage-gates.md`
- `docs/workflow/context-pack.md`
- `docs/workflow/context-pack.json`
- `docs/workflow/execution-report.md`

Executed commands:

```bash
rg -n "^\s*\./gradlew\s+:forensic-analytics-|:forensic-analytics-(boot-app|adapter-joern-docker|engine|application|domain)|bootstrap module can start|existing bootstrap module remains available|current-state evidence|current quality-gate evidence|current multi-project build includes|current implementation baseline|current workflow state|current repository state|verified current behavior|active as legacy quality-gate|active as rollback|remain active|remains active|retained active|active rollback|remain registered|active legacy callers|S15 through S18|S13 through S18|S19|S20|72 direct|653 production|628 test|13 test dependencies" services/analysis-orchestrator-service/README.md services/joern-analysis-service/README.md services/README.md docs/architecture/current-build-and-test-map.md docs/architecture/current-coupling-map.md docs/architecture/legacy-reference-classification.md docs/architecture/monolith-caller-retirement-plan.md docs/architecture/monolith-runtime-isolation.md docs/architecture/service-boundaries.md docs/architecture/service-migration-map.md docs/arc42/05-building-block-view.md docs/arc42/07-deployment-view.md docs/arc42/08-crosscutting-concepts.md docs/skill-audit/README.md
python3 -m json.tool docs/workflow/context-pack.json
git diff --check
./gradlew projects --dependency-verification strict --console=plain --stacktrace
./gradlew :services:analysis-orchestrator-service:test :services:analysis-orchestrator-service:bootJar :services:analysis-orchestrator-service:bootRun :services:joern-analysis-service:test :services:joern-analysis-service:bootJar :services:joern-analysis-service:bootRun --dry-run --dependency-verification strict --console=plain --stacktrace
```

Results:

- The legacy command and active/current legacy evidence scan produced no
  matches after cleanup.
- `docs/workflow/context-pack.json` is valid JSON.
- `git diff --check` passed.
- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`
  passed and listed only `services:*` projects.
- The service-local `analysis-orchestrator-service` and
  `joern-analysis-service` test, `bootJar` and `bootRun` dry-run command
  passed, proving the documented service-local Gradle paths exist without
  starting long-running services.

Subagent and role reviews:

- Senior DevOps: READY. Replace runnable legacy Gradle commands with verified
  service-local commands and run the project-model and dry-run checks.
- Senior Documentation Engineer: READY. Treat stale legacy module references
  as historical/non-executable evidence and avoid inventing replacement
  Gradle tasks.
- Senior System Architect: READY for corrected sequence. The documentation
  stopper must be cleared before physical deletion; architecture review expanded
  S04 to include exact stale current/active claims in architecture and audit
  documents, stale arc42 bootstrap/logging runtime claims, and superseded old
  S15-S20 retirement sequencing/count evidence in architecture documentation.
- Senior Tester: READY with documentation-only scope; no product test code
  changed in S04.

S04 handoff:

- S05 may run physical source-tree deletion after checkpointing this slice and
  rerunning deletion prechecks.
- S06 still owns final architecture/ADR closure after deletion evidence exists.
- S07 still owns the full local quality gate and release-readiness evidence.

Rollback reference:

- Revert the S04 checkpoint commit before S05 if active service or deployment
  documentation must temporarily restore legacy command wording for audit
  reasons.

arc42Updated: limited section 07 pre-delete command truthfulness update; final
closure pending S06
adrUpdated: checked; no ADR update required because S04 changes only
documentation truthfulness and does not change runtime behavior or public
contracts

## S05 Execution Result

Status: COMPLETED

Scope executed:

- Removed the 16 tracked legacy source trees:
  `forensic-analytics-adapter-javaparser`,
  `forensic-analytics-adapter-joern-docker`,
  `forensic-analytics-adapter-repository-source`,
  `forensic-analytics-application`, `forensic-analytics-boot-app`,
  `forensic-analytics-bootstrap`, `forensic-analytics-cli`,
  `forensic-analytics-domain`, `forensic-analytics-engine`,
  `forensic-analytics-ingestion-grpc`,
  `forensic-analytics-ingestion-request`,
  `forensic-analytics-logging`, `forensic-analytics-observability`,
  `forensic-analytics-persistence`, `forensic-analytics-rest`, and
  `forensic-analytics-testbed`.
- No service source, contract, runtime, Docker, or active Gradle build files were
  modified in this slice.

Verification:

- `git ls-files "forensic-analytics-*"`: passed; no tracked legacy source-tree
  files remain in the index.
- `git ls-files "*build.gradle.kts" | grep -v "^forensic-analytics-" | xargs -r rg -n "project\\(\\\":forensic-analytics-"`:
  passed; no active Gradle project dependencies point to retired legacy modules.
- `git ls-files "*.java" | grep -v "^forensic-analytics-" | xargs -r rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\\.request|ingestion\\.grpc)\\b"`:
  passed; no active Java source imports retired legacy packages.
- `git diff --check` and `git diff --cached --check`: passed.
- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`:
  passed; Gradle lists only the `services` project hierarchy.
- `./gradlew test --dependency-verification strict --console=plain --stacktrace`:
  passed.

Role result:

- Senior Java Backend Developer: READY. The retired source trees were orphaned
  after S04 and were removed as the final physical legacy-tree deletion slice.
- Senior DevOps: READY. The post-delete project-model gate confirms only active
  service projects remain registered.
- Senior System Architect: READY. No active non-legacy Gradle project or Java
  import depends on the removed trees.
- Microservice Senior Expert: READY. No active service tree, contract directory,
  Docker path or service-owned domain changed; the diff introduces no shared
  Java module or service-to-service project dependency.
- Senior Tester: READY. The repository minimum test gate passed after deletion.

S05 handoff:

- S06 owns final arc42, ADR and architecture closure using the S05 deletion
  evidence.
- S07 owns the full local quality gate and release-readiness evidence.

Rollback reference:

- Revert the S05 checkpoint commit if an active service dependency on a retired
  source tree is later discovered.

arc42Updated: completed in S06
adrUpdated: completed in S06

## S06 Execution Result

Status: completed.

Scope executed:

- Added ADR-0022 for final modular-monolith source-tree retirement.
- Updated ADR cross-references while preserving historical decision context.
- Updated arc42 sections 03, 05, 06, 07, 08, 09, 10 and 11 so deleted
  `forensic-analytics-*` source trees are historical predecessor evidence, not
  active runtime/build evidence.
- Updated architecture maps and status documents for the post-S05 service-only
  Gradle topology.
- Expanded S06 workflow scope to include `docs/skill-audit/**` for one stale
  audit-baseline wording fix tied to the post-S05 closure.
- Expanded S06 workflow scope to include `contracts/**` and
  `services/testbed/README.md` for provenance-only wording fixes discovered by
  reviewer recheck. The edits do not change OpenAPI, CLI or gRPC fields,
  endpoints, service names, enum values or compatibility behavior.
- Updated workflow context-pack metadata and hashes with S05 checkpoint
  `d8d9dab` and `trackedLegacyFileCount: 0`.

Verification:

- `python3 -m json.tool docs/workflow/context-pack.json`: passed.
- `git diff --check`: passed.
- `git ls-files "forensic-analytics-*" | wc -l`: `0`.
- Active Gradle legacy project-reference scan: `0`.
- Active Java legacy import scan: `0`.
- `rg -n ":forensic-analytics-" docs docker contracts .dockerignore --glob "!docs/workflow/**"`:
  classification scan returned historical/forbidden-example matches only.
- `rg -n "forensic-analytics-" docs docker contracts .dockerignore --glob "!docs/workflow/**"`:
  classification scan returned historical, ADR, contract compatibility,
  product/runtime namespace or predecessor-provenance matches only.

Reviewer result:

- Senior Requirement Engineer: READY. ADR-0022 required and preserves ADR-0017
  target landscape while closing the pre-S05 tracked-source assumption.
- Architecture Reviewer: READY after stale active-evidence, service-slice label,
  logging/observability and contract/testbed wording fixes.
- Documentation Reviewer: READY after execution-report, ADR, service-boundaries,
  arc42, current-state, workflow-scope and contract/testbed wording fixes.
- Contract / ingestion handoff reviewer: READY. Contract and testbed edits are
  wording/provenance only; no wire/API field, endpoint, service name, enum value
  or compatibility behavior changed.
- Quality Reviewer: READY. S06 docs-only gates are sufficient; ADR-0022 must be
  staged with the final S06 commit.

S06 handoff:

- S07 owns the full local quality gate and final release-readiness evidence.
- S07 must not re-open legacy source-tree deletion unless a new active
  dependency is found.

## S07 Quality Gate And Release Readiness

Status: completed.

Scope:

- Verified the final service-only Gradle topology after all legacy
  `forensic-analytics-*` source trees were physically removed.
- Preserved S16 predecessor CLI deprecation evidence in
  `contracts/cli/gateway-cli-contract.md` so the active testbed can still prove
  that local `analyze` and `ingest-request` vocabulary is not silently routed to
  the public API after S05.
- Added focused regression coverage for:
  - repository-to-BTM start-command boundary validation;
  - defensive analysis-orchestrator enum-to-protobuf mappings;
  - Joern filesystem workspace resolution, artifact collection, provenance
    completeness and semantic artifact byte-read integrity.
- Did not re-register legacy Gradle projects, restore legacy source roots, add
  shared Java modules or claim independent production runtime/deployment
  readiness beyond the verified service build and tests.

Gate repair notes:

- Initial isolated S07 full gate failed in
  `RepositoryAnalysisTestbedTest.targetCliDocumentsLegacyLocalCommandDeprecationWithoutInProcessRouting`
  because `contracts/cli/gateway-cli-contract.md` lost the exact historical
  `S16` / `legacy in-process adapters` evidence fragments that the active
  testbed asserts.
- The testbed failure was fixed by wording-only contract provenance; no CLI,
  REST, gRPC, service behavior or wire contract changed.
- The next full gate passed all tests and JaCoCo module tasks but failed the
  root `checkPackageCoverage` package threshold for three packages:
  `analysisorchestrator.adapter.in.grpc`,
  `analysisorchestrator.application` and
  `joernanalysis.adapter.out.filesystem`.
- Coverage was repaired with focused tests rather than lowering thresholds or
  excluding packages. Post-repair package branch coverage was:
  - `analysisorchestrator.adapter.in.grpc`: `83.90%`;
  - `analysisorchestrator.application`: `85.19%`;
  - `joernanalysis.adapter.out.filesystem`: `80.44%`.

Verification:

- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`:
  passed; project model contains only the root, `:services` and active service
  subprojects.
- `git ls-files "forensic-analytics-*" | wc -l`: `0`.
- `./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace --no-daemon`:
  passed after the CLI contract provenance fix.
- `./gradlew :services:analysis-orchestrator-service:test :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace --no-daemon`:
  passed after targeted coverage tests were added.
- `./gradlew :services:analysis-orchestrator-service:jacocoTestReport :services:joern-analysis-service:jacocoTestReport checkPackageCoverage --dependency-verification strict --console=plain --stacktrace --no-daemon`:
  passed.
- `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace --no-daemon`:
  passed; `206` tasks executed.

Reviewer result:

- Senior Tester: READY to run the S07 gate; no extra targeted tests were
  required before the first full gate.
- Microservice Runtime Readiness Reviewer: READY to run S07 with the constraint
  that this slice must not claim production runtime, Docker image, Compose,
  Swarm or Kubernetes readiness beyond verified gates.
- Senior DevOps: initial gate was BLOCKED by a daemon/lock abort; after daemon
  cleanup and `--no-daemon` isolation, the gate proceeded to actionable test
  and coverage failures.
- Quality Reviewer: identified the package coverage blockers and recommended
  keeping the new focused tests, then rerunning targeted service tests and the
  full gate.

S07 closure:

- Final local quality gate passed.
- Legacy tracked source-tree count remains `0`.
- No remaining S07 stop condition is open.

## Slice Execution Status

| Slice | Status | Notes |
|---|---|---|
| S00 | Completed | Branch, context pack, Gradle project model, leakage baseline and `git diff --check` verified. |
| S01 | Completed | Classification written to `docs/architecture/legacy-reference-classification.md`; deletion closure remains blocked until S02/S03/S04 cleanup and S06/S07 closure gates. |
| S02 | Completed | Runtime, Docker and contract-test documentation now points to service-local ownership; no public contract files changed. |
| S03 | Completed | Service-regression coverage assertions now target active S03 wording; all S03 targeted gates and the repository minimum gate passed. |
| S04 | Completed | Active service and deployment documentation blockers cleared; service-local Gradle dry-runs and project-model gate passed. |
| S05 | Completed | Removed all 16 tracked legacy source trees; post-delete leakage scans, project-model gate and repository test gate passed. |
| S06 | Completed | Closed arc42, ADR, architecture, workflow, contract-provenance and testbed wording after deletion evidence; local gates and reviewer rechecks passed. |
| S07 | Completed | Final full local quality gate passed after testbed provenance and package coverage repairs; legacy tracked source-tree count remains `0`. |

## Open Stop Conditions For Execution

- Do not re-register any legacy Gradle project.
- Do not use stale `:forensic-analytics-*` test tasks.
- Stop if a remaining legacy reference cannot be classified.
- Stop if deleting a legacy tree removes the only known coverage for supported
  behavior.
- Stop if contract compatibility wording is behavior-relevant and not reviewed.
- Stop if full local quality gate fails.
