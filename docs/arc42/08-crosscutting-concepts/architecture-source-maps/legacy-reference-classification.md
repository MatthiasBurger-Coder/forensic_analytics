# Legacy Reference Classification

## Status

S01 classification status: `COMPLETED`.

Deletion closure status: `S06_ARCHITECTURE_CLOSURE_IN_PROGRESS`. S02, S03 and
S04 cleared the pre-deletion runtime, regression and executable-documentation
stoppers. S05 deleted the tracked legacy source trees in checkpoint `d8d9dab`.
S06 closes architecture, arc42 and ADR evidence before S07 release readiness.

## Verification

S01 verified repository state on branch
`architecture/workflow-legacy-module-retirement-20260522`:

- `settings.gradle.kts` includes only top-level service projects projects.
- Before S05, `git ls-files "forensic-analytics-*" | wc -l` returned `450`
  tracked legacy source-tree files.
- After S05 checkpoint `d8d9dab`, `git ls-files "forensic-analytics-*"`
  returns no tracked legacy source-tree files.
- No active non-legacy build file contains a
  `project(":forensic-analytics-*")` dependency.
- No active non-legacy Java source imports legacy monolith packages.
- `rg -n "forensic-analytics-" docker .dockerignore contracts docs --glob "!docs/workflow/**"`
  returns `283` focused documentation/runtime/contract matches.
- `git diff --check` passed before S01 edits.

## Classification Categories

### Removable Implementation Or Runtime Documentation

These references were stale executable or runtime documentation and had to be
removed or retargeted before S05 source-tree deletion was accepted:

- `docs/README.md`: legacy module inventory, gRPC/REST/Bootstrap/Boot app
  runtime sections, legacy Gradle commands and legacy jar paths.
- `.dockerignore`: `forensic-analytics-boot-app/build/libs` Docker-context
  exceptions.
- historical `docker/boot-app/Dockerfile` path when present: default jar path
  under the legacy Boot app; the active repository no longer contains this file.
- `docker/boot-app/README.md`: legacy Boot app `bootJar`, image and run
  commands.
- `docs/arc42/10-quality-requirements/testing/wildfly-hardening.md`: legacy `forensic-analytics-testbed`
  test path and `:forensic-analytics-testbed:test` commands.
- `docs/arc42/08-crosscutting-concepts/service-contracts/contract-test-plan.md`: legacy
  `:forensic-analytics-rest:test` command.

S02 and S04 completed the runtime, Docker and contract-documentation cleanup
for these files. They must not be reintroduced as runnable-looking commands for
unregistered or deleted legacy Gradle projects.

### Historical Architecture Baseline

These references are architecture history, predecessor evidence or stale
state-of-record material. They may remain only when rewritten as dated,
superseded or historical evidence:

- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-coupling-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-caller-retirement-plan.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-runtime-isolation.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-migration-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monorepo-service-build-strategy.md`
- `docs/arc42/03-system-scope-and-context.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/skill-audit/README.md`

Any wording that says legacy modules are registered in the verified project
model, quality-gate participants, rollback runtime units, or the operative
implementation baseline is a post-S05 architecture blocker. S06 owns final
architecture and arc42 closure and must leave remaining names explicitly marked
as historical predecessor, contract compatibility or product/runtime namespace
evidence.

### Compatibility Vocabulary

These references can remain only as contract compatibility or provenance
vocabulary. They must not be treated as proof that a legacy Java implementation
is active:

- `contracts/openapi/gateway-api.yaml`: `forensic-analytics-cli gateway-submit`
  predecessor compatibility mode.
- `contracts/cli/gateway-cli-contract.md`: legacy CLI command vocabulary and
  predecessor consumer wording.
- `contracts/grpc/forensic-ingestion.proto`: predecessor proto provenance and
  compatibility surface.
- `contracts/openapi/README.md`: legacy REST contract-test note when rewritten
  as historical rollback provenance only.
- `docs/arc42/09-architecture-decisions/adr/**`: authoritative ADR context for
  Boot, REST, logging, observability and target service decisions. The former
  `docs/adr/` root now keeps only a compatibility pointer.

Contract wording must not be removed or reinterpreted without contract
governance. Compatibility terms such as `gateway-submit`, `--gateway`,
`GatewayOpenApiContractTest`, `AnalyzeRepository` compatibility, and deprecated
field references stay until a contract decision supersedes them.

### Product Or Runtime Namespace

These names are product/container/runtime names, not legacy Gradle source-tree
references:

- `docker/joern/docker-compose.joern.yml`: `forensic-analytics-joern`.
- Runtime storage names such as `forensic-analytics-workspaces`.

Do not remove these names solely because they contain `forensic-analytics-`.

### Active Blockers

The following block final deletion closure until rewritten, retargeted or
explicitly marked as historical:

- Any public README, arc42, architecture map, testing doc, contract-test plan,
  Docker doc or Dockerfile that presents `forensic-analytics-*` modules as
  runnable, registered, active test targets or current runtime paths.
- Any `:forensic-analytics-*` Gradle command outside an explicitly historical
  or superseded note.
- Any rollback or regression claim that depends only on source trees planned
  for deletion.
- Any persistence ownership claim that treats `forensic-analytics-persistence`
  deletion as owner proof without a verified service owner or explicit
  deprecation decision.

## S02 Handoff

S02 cleaned stale executable/runtime references in:

- `.dockerignore`
- historical `docker/boot-app/Dockerfile` path when present
- `docker/boot-app/README.md`
- `docs/README.md`
- `docs/arc42/10-quality-requirements/testing/wildfly-hardening.md`
- `docs/arc42/08-crosscutting-concepts/service-contracts/contract-test-plan.md`

Contract compatibility vocabulary remains protected unless a
contract-governance review approves a behavior-neutral wording change.

## S03 Handoff

S03 used service-local gates only. It did not rely on any
`:forensic-analytics-*` Gradle task because the active project model no longer
included those modules.

Stale legacy task commands were replaced with service or root gates, and
remaining module-local test references were marked as historical predecessor
evidence or deprecated behavior.

## S06 Handoff

S06 must reconcile architecture and arc42 documents after physical deletion.
It must preserve ADR history, update current-state claims, and keep evidence
categories explicit. It must not claim service runtime, Docker, healthcheck,
Swarm or Kubernetes readiness without verified repository commands and
artifacts.
