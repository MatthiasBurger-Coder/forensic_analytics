# Workflow: Move Repository Workspace Metadata to PostgreSQL

## Executive Summary

This workflow plans the migration of `repository-source-service` repository
checkout workspace metadata from the Docker-local H2 MVP adapter to the
existing `forensic-postgres` PostgreSQL database. The repository checkout bytes
remain in the service-owned Docker volume. PostgreSQL owns only the
repository-source metadata tables and idempotency records through
`repository-source-service` as the single writer.

The workflow does not implement product code. It defines executable slices for
`workflow execute`.

## Verified Baseline

- Active branch: `feature/workflow-workspace-postgres-20260529`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Repository root: `/mnt/d/Projects/forensic_analytics`
- Current owner service: `repository-source-service`
- Current runtime persistence: H2 file adapter
  `H2RepositorySourcePersistenceAdapter`
- Current checkout byte storage:
  `/var/lib/forensic-analytics/repository-workspaces`
- Existing PostgreSQL container material:
  `docker/postgres/docker-compose.yml`, service name `forensic-postgres`
- Quality source: `QUALITY.md`

## Interpreted Intent

The request is interpreted as:

- Move repository checkout workspace metadata to PostgreSQL.
- Use Liquibase to create and evolve the repository-source workspace schema.
- Keep the checked-out repository files on the existing service-owned Docker
  volume.
- Work out the remaining changes autonomously from verified repository state.

`workspace` means the repository checkout workspace aggregate owned by
`repository-source-service`, not the broader deferred platform workspace,
membership, project, asset, audit or retention domain.

## Target Picture

```text
query-report-api-service and other consumers
  -> repository-source-service owner API
     -> forensic-postgres / repository_source schema
        -> workspace metadata
        -> workspace branch metadata
        -> repository preparation records
        -> repository-source idempotency records
     -> repository-source-workspaces volume
        -> checkout bytes and source packages
```

Liquibase creates the service-owned schema and tables inside the already
available PostgreSQL database. The PostgreSQL server and database bootstrap
remain Docker/operator responsibility through `forensic-postgres`.

## Scope

- ADR and arc42 updates for the bounded PostgreSQL decision.
- Repository-source Gradle dependencies and dependency verification metadata.
- Repository-source typed persistence configuration.
- Liquibase changelog for repository-source workspace metadata.
- PostgreSQL JDBC adapter behind existing repository-source ports.
- Bootstrap wiring from `persistence.type=postgres`.
- Docker Compose integration with `forensic-postgres`.
- Regression tests for repository-source persistence behavior.
- Documentation updates for runtime and local operator flow.

## Non-Goals

- No platform workspace, membership, project, asset, audit or retention
  implementation.
- No public REST, gRPC or UI contract change unless implementation discovers a
  verified incompatibility.
- No cross-service database access.
- No storage of checkout bytes in PostgreSQL.
- No Graph DB, Vector DB, replay, report or LLM storage decision.
- No hidden H2 compatibility mode after the PostgreSQL cutover.
- No automatic import of local H2 files unless a later slice verifies and
  documents a one-off operator migration requirement.

## Requirement Classification

| Requirement | Classification | Trace |
|---|---|---|
| Store workspace metadata in PostgreSQL | Functional, architecture, persistence | User request, ADR-0013 |
| Create schema through Liquibase | Functional, build/runtime, quality | User request |
| Keep checkout bytes on a volume | Functional, security, data ownership | User request, service-boundary docs |
| Preserve repository-source as owner and single writer | Architecture constraint | ADR-0013, data ownership docs |
| Keep external API shape stable | Contract constraint | Contract-first governance |
| Preserve deterministic repository workspace behavior | Quality/evidence requirement | `QUALITY.md`, existing tests |

## Assumptions

- `forensic-postgres` is the PostgreSQL instance named by the request.
- Liquibase creates schema objects inside `forensic_analytics`; it does not
  create the PostgreSQL server or container.
- PostgreSQL schema name should be service-owned, for example
  `repository_source`.
- Existing local H2 metadata does not need automatic migration unless the user
  explicitly asks for state preservation before `workflow execute`.
- Checkout byte retention is provided by the existing
  `repository-source-workspaces` volume.

## Architecture Constraints

- `repository-source-service` remains the only writer for repository checkout
  workspace metadata.
- Other services may read workspace state only through repository-source owner
  APIs and public facade APIs.
- Domain and application packages remain framework-free and database-free.
- JDBC, Liquibase and PostgreSQL code stays in bootstrap or outbound adapter
  packages.
- Private database table names, workspace paths, raw Git output and credentials
  must not appear in public DTOs, diagnostics or UI responses.
- H2 remains historical MVP evidence only after the cutover.

## Backend Assessment

The implementation must replace H2-specific persistence semantics:

- H2 `MERGE INTO ... KEY` must become PostgreSQL `INSERT ... ON CONFLICT`.
- H2 CLOB columns must become PostgreSQL-compatible `TEXT` or explicitly typed
  timestamp columns where the mapping is tested.
- Schema creation must move out of adapter startup and into Liquibase.
- Existing ports can remain stable:
  `RepositoryWorkspaceRepository`, `RepositoryPreparationRepository`,
  `RepositorySourceIdempotencyRepository`.
- Bootstrap must select PostgreSQL from typed configuration and fail fast on
  missing unsafe or ambiguous settings.

## Frontend Assessment

No frontend implementation is planned. Existing UI and API client code should
remain unaffected because public workspace routes and DTOs do not change. The
React role remains a mandatory Three Amigos participant with N/A implementation
impact unless a later slice changes public response shape or error semantics.

## Test Strategy

- Preserve existing application-service tests against in-memory ports.
- Add or replace persistence contract tests for PostgreSQL behavior.
- Keep default Gradle tests independent from a live external database unless a
  documented opt-in integration profile is introduced.
- Verify Liquibase changelog content and adapter mapping deterministically.
- Run repository-source targeted tests before the full local gate.
- Run Docker Compose `config` checks for changed deployment descriptors.

## Resilience Requirements

- PostgreSQL startup or connectivity failures must fail fast or report DOWN
  through a tested health/readiness path.
- Storage writes must stay transactional where workspace aggregate plus branch
  rows are updated together.
- Retried workspace operations must remain protected by existing idempotency
  records.
- Diagnostics must be sanitized and must not expose database URLs with
  credentials, table names as user-facing evidence, private workspace paths or
  raw Git output.

## Ordered Slices

### Slice 01 - Governance Decision and Architecture Documents

Purpose: record PostgreSQL as the bounded repository-source workspace metadata
store and align architecture documentation before implementation.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers:
  - Senior Requirement Engineer
  - Data Ownership And Persistence Steward
  - ADR Steward
affected_files:
  - docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md
  - docs/arc42/05-building-block-view.md
  - docs/arc42/07-deployment-view.md
  - docs/arc42/08-crosscutting-concepts.md
  - docs/arc42/09-architecture-decisions.md
  - docs/arc42/11-risks-and-technical-debt.md
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: P1
file_locks:
  - docs/adr/**
  - docs/arc42/**
  - docs/architecture/**
contract_locks: []
architecture_locks:
  - repository-source-data-ownership
  - relational-store-decision
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: required
  adr: required
stop_conditions:
  - PostgreSQL ownership conflicts with ADR-0013.
  - The decision tries to turn PostgreSQL into shared cross-service storage.
  - The broader platform workspace domain is confused with repository checkout workspace metadata.
```

### Slice 02 - Gradle Dependencies and Typed PostgreSQL Configuration

Purpose: add verified PostgreSQL and Liquibase dependencies, dependency
verification metadata and typed repository-source configuration.

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior DevOps Engineer
  - Senior Tester
affected_files:
  - gradle/libs.versions.toml
  - gradle/verification-metadata.xml
  - repository-source-service/build.gradle.kts
  - repository-source-service/src/main/resources/application.properties
  - repository-source-service/src/main/resources/application-docker.properties
  - repository-source-service/src/main/resources/application-test.properties
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceProperties.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServicePropertiesConfiguration.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S01
parallel_group: P2
file_locks:
  - gradle/libs.versions.toml
  - gradle/verification-metadata.xml
  - repository-source-service/build.gradle.kts
  - repository-source-service/src/main/resources/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
contract_locks: []
architecture_locks:
  - repository-source-bootstrap-boundary
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Dependency verification metadata cannot be updated for new artifacts.
  - PostgreSQL credentials are committed as secrets instead of configuration.
  - Domain or application code receives Spring, JDBC, Liquibase or PostgreSQL dependencies.
```

### Slice 03 - Liquibase Repository-Source Schema

Purpose: create the repository-source-owned PostgreSQL schema and metadata
tables through Liquibase changelogs.

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior Analysis Storage Architect
  - Senior Tester
affected_files:
  - repository-source-service/src/main/resources/db/changelog/repository-source-workspace.postgresql.yaml
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/PostgresRepositorySourceLiquibaseTest.java
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S01
  - S02
parallel_group: P3
file_locks:
  - repository-source-service/src/main/resources/db/changelog/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/**
contract_locks: []
architecture_locks:
  - repository-source-postgres-schema
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Liquibase changelog invents data not present in the existing repository-source domain model.
  - Schema fields cannot be traced to existing H2 columns or verified domain records.
  - Checkout bytes or source package bytes are moved into PostgreSQL.
```

### Slice 04 - PostgreSQL Persistence Adapter

Purpose: implement a PostgreSQL outbound adapter for existing repository-source
ports while preserving deterministic save/load semantics.

```yaml
slice_id: S04
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior Analysis Storage Architect
  - Senior System Architect
  - Senior Tester
affected_files:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourcePostgresPersistenceApplicationTest.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/quality/RepositorySourceServiceArchitectureTest.java
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S03
parallel_group: P4
file_locks:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/quality/**
contract_locks: []
architecture_locks:
  - hexagonal-outbound-adapter-boundary
  - repository-source-persistence-ports
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Adapter exposes PostgreSQL classes outside adapter/bootstrap packages.
  - SQL upsert semantics cannot preserve existing H2-tested behavior.
  - Persistence failures are hidden as empty or successful results.
```

### Slice 05 - Bootstrap, Liquibase Execution and Health Wiring

Purpose: wire PostgreSQL as the active repository-source persistence option,
run Liquibase before repository creation and make storage readiness observable.

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior DevOps Engineer
  - Observability And Runtime Diagnostics
  - Senior Tester
affected_files:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceConfiguration.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/HealthHttpServerLifecycle.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S04
parallel_group: P5
file_locks:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
contract_locks: []
architecture_locks:
  - repository-source-bootstrap-boundary
  - storage-readiness
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Liquibase can run after application use cases start accepting requests.
  - Health reports UP when mandatory PostgreSQL persistence is unreachable.
  - Database exception details leak credentials or private SQL diagnostics to public responses.
```

### Slice 06 - Docker Compose and Local PostgreSQL Runtime

Purpose: connect repository-source Docker runtime to `forensic-postgres` while
preserving the checkout workspace volume.

```yaml
slice_id: S06
profile: FULL_PATH
owner: Senior DevOps Engineer
secondary_reviewers:
  - Senior Java Backend Developer
  - Security And Threat Modeling
  - Senior Tester
affected_files:
  - docker/postgres/docker-compose.yml
  - docker/postgres/.env.example
  - deployment/docker-compose/services/repository-source-service.compose.yml
  - deployment/docker-compose/repository-to-btm.local.yml
  - deployment/docker-compose/setup.sh
  - deployment/docker-compose/README.md
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S05
parallel_group: P6
file_locks:
  - docker/postgres/**
  - deployment/docker-compose/**
contract_locks: []
architecture_locks:
  - local-docker-network
  - repository-source-private-volume
quality_gates:
  targeted:
    - docker compose --env-file docker/postgres/.env.example -f docker/postgres/docker-compose.yml config
    - docker compose -f deployment/docker-compose/services/repository-source-service.compose.yml -f deployment/docker-compose/forensic-analytics.local.yml config
    - docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
  required:
    - git diff --check
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - The repository-source checkout volume is removed or mounted into another service.
  - Repository-source reads PostgreSQL through a host-only path when container DNS is required.
  - Secrets are added to committed Compose files.
```

### Slice 07 - H2 MVP Retirement and Migration Policy

Purpose: remove H2 from active repository-source runtime and document the
operator policy for existing local H2 state.

```yaml
slice_id: S07
profile: FULL_PATH
owner: Data Ownership And Persistence Steward
secondary_reviewers:
  - Senior Java Backend Developer
  - Senior Tester
  - Senior Documentation Engineer
affected_files:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/H2RepositorySourcePersistenceAdapter.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceH2PersistenceApplicationTest.java
  - repository-source-service/README.md
  - docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
  - docs/architecture/data-ownership.md
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S06
parallel_group: P7
file_locks:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - repository-source-service/README.md
  - docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
  - docs/architecture/data-ownership.md
contract_locks: []
architecture_locks:
  - h2-mvp-retirement
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: required
stop_conditions:
  - H2 remains as an undocumented runtime fallback.
  - Existing H2 data preservation is required but no explicit migration source and acceptance criteria are provided.
  - Removing H2 breaks default quality gates without a PostgreSQL-independent test strategy.
```

### Slice 08 - End-to-End Verification and Release Readiness

Purpose: run the targeted and repository quality gates, inspect diffs and
record final workflow execution evidence.

```yaml
slice_id: S08
profile: FULL_PATH
owner: Senior Tester
secondary_reviewers:
  - Quality Gate Orchestrator
  - Senior DevOps Engineer
  - Senior System Architect
affected_files:
  - docs/workflow/execution-report.md
  - docs/workflow/quality-and-leakage-gates.md
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S07
parallel_group: P8
file_locks:
  - docs/workflow/execution-report.md
  - docs/workflow/quality-and-leakage-gates.md
contract_locks: []
architecture_locks:
  - release-readiness
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - docker compose --env-file docker/postgres/.env.example -f docker/postgres/docker-compose.yml config
    - docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
    - git diff --check
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Any required quality gate fails.
  - Diff inspection finds unrelated or unowned changes.
  - PostgreSQL runtime is claimed without executed runtime evidence.
```

## Dependency Summary

```text
S01
  -> S02
    -> S03
      -> S04
        -> S05
          -> S06
            -> S07
              -> S08
```

No slices are safely parallelizable because the persistence decision, build
configuration, schema, adapter, runtime wiring and H2 retirement form a single
ordered cutover.

## Role Ownership Map

| Role | Ownership |
|---|---|
| Senior Requirement Engineer | Requirement interpretation, EPIC drift and assumption tracking |
| Senior System Architect | ADR, arc42, hexagonal and service-boundary validation |
| Senior Java Backend Developer | Repository-source configuration, Liquibase, JDBC adapter and tests |
| Senior React Frontend Developer | Mandatory N/A impact check unless public DTOs change |
| Senior Tester | Regression strategy, quality gate selection and final verification |
| Data Ownership And Persistence Steward | One-writer persistence model and H2 retirement policy |
| Senior Analysis Storage Architect | Schema responsibility, metadata/provenance storage checks |
| Senior DevOps Engineer | Docker Compose, local runtime, dependency verification and operator commands |
| Security And Threat Modeling | Credentials, network, diagnostics and private path leakage review |
| Observability And Runtime Diagnostics | Health/readiness and sanitized failure reporting |

No callable subagents were used while authoring this workflow; the matching
skills and role files were applied as local review checklists.

## Quality Gate Expectations

Minimum repository quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Slice-specific Docker model checks are required when Compose files change.
Live PostgreSQL startup checks are optional unless the executing slice records
Docker availability and intentionally runs the runtime scenario.

## Documentation Synchronization Points

- S01 updates ADR and arc42 before implementation.
- S06 updates Docker runtime documentation.
- S07 updates H2 retirement and migration policy.
- S08 updates workflow execution evidence after verification.

## Stop Conditions

Stop workflow execution when:

- PostgreSQL ownership is unclear or becomes cross-service shared storage.
- Liquibase table names or columns would need to be guessed.
- The repository checkout volume is removed or mounted by a non-owner service.
- Public API shape changes without contract governance.
- Domain/application code depends on JDBC, Liquibase, PostgreSQL or Spring.
- Credentials, private paths or raw Git output would leak into public
  diagnostics.
- Existing H2 state must be preserved but migration inputs are not explicitly
  verified.
- Required Gradle, dependency verification or Compose checks fail.

## Handoff to Workflow Execute

This workflow is ready for `workflow execute` under the documented assumptions.
Implementation must execute slices in order and must not change production code
before the owning role review for each slice is complete.

## Definition of Done

- PostgreSQL decision is documented and bounded to repository-source workspace
  metadata.
- Liquibase creates the service-owned repository-source schema.
- Repository-source persistence uses PostgreSQL for workspace metadata,
  branch metadata, repository preparation and idempotency records.
- Checkout bytes remain on the repository-source workspace volume.
- H2 is no longer an active hidden runtime fallback.
- Public APIs and UI behavior remain stable unless a contract-governed slice
  explicitly changes them.
- Required quality gates pass and are recorded.
