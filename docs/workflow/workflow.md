# Workflow: Move Repository Workspace Metadata to PostgreSQL

## Executive Summary

This workflow plans the migration of `repository-source-service` repository
checkout workspace metadata from the Docker-local H2 MVP adapter to the
existing `forensic-postgres` PostgreSQL database. The repository checkout bytes
remain in the service-owned Docker volume. PostgreSQL owns only the
repository-source metadata tables and idempotency records through
`repository-source-service` as the single writer.

The clarified runtime target is PostgreSQL for local runtime and production
operation. H2 remains allowed only for tests and deterministic fixtures. A
missing or unreachable PostgreSQL database must be reported by startup failure
or storage health/readiness `DOWN`; it must not silently fall back to H2.

The workflow now also plans an operator Settings path in the existing React UI
and public API layer for repository-source PostgreSQL configuration. That path
must be contract-first, must not expose or persist raw credentials without a
verified secrets boundary, and must not let the UI connect directly to the
database or repository-source private tables.

The workflow does not implement product code. It defines executable slices for
`workflow execute`.

## Verified Baseline

- Active branch: `feature/workflow-workspace-postgres-20260529`
- Workflow version: `2026-05-31`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Repository root: `/mnt/d/Projects/forensic_analytics`
- Current owner service: `repository-source-service`
- Current pre-cutover runtime persistence: H2 file adapter
  `H2RepositorySourcePersistenceAdapter`
- Current checkout byte storage:
  `/var/lib/forensic-analytics/repository-workspaces`
- Existing PostgreSQL container material:
  `docker/postgres/docker-compose.yml`, service name `forensic-postgres`
- Existing frontend module: `forensic-ui`
- Existing settings route placeholder:
  `forensic-ui/src/pages/settings/SettingsPage.tsx`
- Existing public API gateway service: `query-report-api-service`
- Existing public REST contract: `contracts/openapi/gateway-api.yaml`
- Quality source: `QUALITY.md`

## Interpreted Intent

The request is interpreted as:

- Move repository checkout workspace metadata to PostgreSQL.
- Use Liquibase to create and evolve the repository-source workspace schema.
- Keep the checked-out repository files on the existing service-owned Docker
  volume.
- Keep H2 available for tests and deterministic fixtures only.
- Make PostgreSQL the runtime and production persistence path.
- Report missing or unreachable PostgreSQL explicitly through startup failure
  or storage health/readiness instead of falling back to H2.
- Add a contract-first operator Settings path for database configuration in
  the existing UI and public API surface.
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

forensic-ui Settings
  -> query-report-api-service public Settings API
     -> contract-governed repository-source configuration handoff
        -> validated PostgreSQL connection settings
        -> sanitized readiness and validation diagnostics
```

Liquibase creates the service-owned schema and tables inside the already
available PostgreSQL database. The PostgreSQL server and database bootstrap
remain Docker/operator responsibility through `forensic-postgres`.

The Settings path is an operator configuration workflow. It is not forensic
evidence, not a direct database client, and not a public exposure of
repository-source private storage. Secret handling, persistence of settings and
runtime apply/restart semantics must be verified inside the dedicated Settings
slices before implementation claims production readiness.

## Scope

- ADR and arc42 updates for the bounded PostgreSQL decision.
- Repository-source Gradle dependencies and dependency verification metadata.
- Repository-source typed persistence configuration.
- Liquibase changelog for repository-source workspace metadata.
- PostgreSQL JDBC adapter behind existing repository-source ports.
- Bootstrap wiring from `persistence.type=postgres`.
- Docker Compose integration with `forensic-postgres`.
- Runtime default and Docker profile cutover to PostgreSQL.
- H2 retention only for tests and deterministic fixtures.
- Contract-governed Settings API for database configuration.
- React Settings page wiring through the existing frontend adapter layer.
- Regression tests for repository-source persistence behavior.
- Documentation updates for runtime and local operator flow.

## Non-Goals

- No platform workspace, membership, project, asset, audit or retention
  implementation.
- No public REST, gRPC or UI contract change outside the explicit
  contract-governed Settings slices.
- No cross-service database access.
- No storage of checkout bytes in PostgreSQL.
- No Graph DB, Vector DB, replay, report or LLM storage decision.
- No hidden H2 runtime compatibility mode after the PostgreSQL cutover.
- No direct database access from the UI.
- No committed, logged, browser-persisted or public-response database
  credentials.
- No automatic import of local H2 files unless a later slice verifies and
  documents a one-off operator migration requirement.

## Requirement Classification

| Requirement | Classification | Trace |
|---|---|---|
| Store workspace metadata in PostgreSQL | Functional, architecture, persistence | User request, ADR-0013 |
| Create schema through Liquibase | Functional, build/runtime, quality | User request |
| Keep checkout bytes on a volume | Functional, security, data ownership | User request, service-boundary docs |
| Preserve repository-source as owner and single writer | Architecture constraint | ADR-0013, data ownership docs |
| Keep H2 only for tests and fixtures | Functional, quality constraint | User clarification, S07 preflight |
| Report missing PostgreSQL explicitly | Resilience, observability | User clarification, S05/S07 health behavior |
| Configure database through UI Settings | Functional, UX, security, contract | User clarification, existing `forensic-ui` Settings placeholder |
| Govern external API shape through contracts | Contract constraint | Contract-first governance |
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
- H2-backed tests may remain in the repository when they are explicitly scoped
  as tests or fixtures and do not participate in runtime bootstrap selection.
- The Settings UI uses the existing `forensic-ui` route and API adapter style.
- Public Settings changes use `query-report-api-service` and
  `contracts/openapi/gateway-api.yaml` unless a slice verifies a different
  public contract owner before implementation.

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
- H2 remains test and fixture infrastructure only after the cutover.
- Operator-provided database settings must be validated and redacted before
  they cross public API, UI state, logs or diagnostics boundaries.
- UI Settings must call public application APIs only; it must not talk directly
  to PostgreSQL or repository-source private database tables.

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
- Runtime bootstrap must not include an H2 fallback path outside test fixtures.
- Settings backend work must define ownership, validation, redaction, apply
  semantics and storage of operator database configuration before it can change
  runtime behavior.

## Frontend Assessment

Frontend implementation is now planned for database Settings. The existing
`forensic-ui/src/pages/settings/SettingsPage.tsx` is a placeholder and the
existing frontend uses application ports plus API adapters under
`forensic-ui/src/application` and `forensic-ui/src/adapters/api`.

The Settings UI must:

- use the existing public API base path, not direct database connectivity;
- keep secrets out of local storage, URL parameters, diagnostics and rendered
  read-back values;
- show PostgreSQL validation status without displaying raw JDBC URLs with
  credentials;
- preserve accessible form controls and deterministic validation messages;
- remain separate from forensic evidence review and workspace data.

## Test Strategy

- Preserve existing application-service tests against in-memory ports.
- Add or replace persistence contract tests for PostgreSQL behavior.
- Keep default Gradle tests independent from a live external database unless a
  documented opt-in integration profile is introduced.
- Verify Liquibase changelog content and adapter mapping deterministically.
- Run repository-source targeted tests before the full local gate.
- Run query-report-api contract and service tests for public Settings API
  changes.
- Run frontend unit/build checks for Settings UI changes.
- Run Docker Compose `config` checks for changed deployment descriptors.

## Resilience Requirements

- PostgreSQL startup or connectivity failures must fail fast or report DOWN
  through a tested health/readiness path.
- H2 runtime fallback must not mask missing PostgreSQL outside test fixtures.
- Storage writes must stay transactional where workspace aggregate plus branch
  rows are updated together.
- Retried workspace operations must remain protected by existing idempotency
  records.
- Diagnostics must be sanitized and must not expose database URLs with
  credentials, table names as user-facing evidence, private workspace paths or
  raw Git output.
- Settings validation must distinguish invalid configuration, unreachable
  PostgreSQL and unsupported runtime apply semantics.

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

### Slice 07 - PostgreSQL Runtime Default and H2 Test Boundary

Purpose: make PostgreSQL the repository-source runtime and production
persistence path, keep H2 only as test or fixture infrastructure, and document
the operator policy for existing local H2 state.

```yaml
slice_id: S07
profile: FULL_PATH
owner: Data Ownership And Persistence Steward
secondary_reviewers:
  - Senior Java Backend Developer
  - Observability And Runtime Diagnostics
  - Senior Tester
  - Senior Documentation Engineer
affected_files:
  - repository-source-service/src/main/resources/application.properties
  - repository-source-service/src/main/resources/application-docker.properties
  - repository-source-service/src/main/resources/application-test.properties
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceConfiguration.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceProperties.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServicePropertiesConfiguration.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/HealthHttpServerLifecycle.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/H2RepositorySourcePersistenceAdapter.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceH2PersistenceApplicationTest.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java
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
  - repository-source-service/src/main/resources/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/README.md
  - docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
  - docs/architecture/data-ownership.md
contract_locks: []
architecture_locks:
  - h2-mvp-retirement
  - repository-source-postgres-runtime-default
  - storage-readiness
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: required
stop_conditions:
  - H2 remains selectable as an active runtime or Docker fallback.
  - H2 tests are removed without PostgreSQL-independent test coverage for default gates.
  - Missing or unreachable PostgreSQL is reported as successful readiness.
  - Existing H2 data preservation is required but no explicit migration source and acceptance criteria are provided.
  - Runtime properties default to H2 outside test scope.
```

### Slice 08 - Database Settings Contract and Backend Handoff

Purpose: add the contract-first public Settings API and backend handoff for
operator-managed repository-source PostgreSQL configuration without exposing
secrets or bypassing service ownership.

```yaml
slice_id: S08
profile: FULL_PATH
owner: Contract-First API Steward
secondary_reviewers:
  - Senior Requirement Engineer
  - Senior System Architect
  - Senior Java Backend Developer
  - Data Ownership And Persistence Steward
  - Security And Threat Modeling
  - Observability And Runtime Diagnostics
  - Senior Tester
affected_files:
  - contracts/openapi/gateway-api.yaml
  - contracts/openapi/README.md
  - contracts/grpc/repository-analysis.proto
  - contracts/grpc/README.md
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/**
  - query-report-api-service/src/main/resources/**
  - query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
affected_modules:
  - query-report-api-service
  - repository-source-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S07
parallel_group: P8
file_locks:
  - contracts/openapi/**
  - contracts/grpc/**
  - query-report-api-service/src/main/**
  - query-report-api-service/src/test/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
contract_locks:
  - public-settings-api
  - repository-source-settings-handoff
architecture_locks:
  - settings-ownership
  - secret-redaction
  - repository-source-configuration-boundary
quality_gates:
  targeted:
    - ./gradlew :query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - REST or gRPC Settings fields, methods, status codes or error models would need to be guessed.
  - Settings ownership or persistence of operator configuration is unclear.
  - Raw database passwords are returned to the UI, stored in browser state, logged or committed.
  - The UI or query-report-api-service reads repository-source private PostgreSQL tables directly.
  - Runtime apply or restart semantics for changed database settings are undocumented.
```

### Slice 09 - React Database Settings UI

Purpose: replace the Settings placeholder with an operator workflow for
viewing sanitized PostgreSQL configuration status, validating new database
settings and submitting them through the public Settings API.

```yaml
slice_id: S09
profile: FULL_PATH
owner: Senior React Frontend Developer
secondary_reviewers:
  - Senior UX Designer
  - Security And Threat Modeling
  - Senior Tester
affected_files:
  - forensic-ui/src/pages/settings/SettingsPage.tsx
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/application/**
  - forensic-ui/src/domain/**
  - forensic-ui/src/app/App.test.tsx
  - forensic-ui/src/styles.css
affected_modules:
  - forensic-ui
affected_contracts:
  - contracts/openapi/gateway-api.yaml
dependencies:
  - S08
parallel_group: P9
file_locks:
  - forensic-ui/src/pages/settings/**
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/application/**
  - forensic-ui/src/domain/**
  - forensic-ui/src/app/**
  - forensic-ui/src/styles.css
contract_locks:
  - public-settings-api
architecture_locks:
  - frontend-api-adapter-boundary
  - secret-redaction
quality_gates:
  targeted:
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - Database credentials are written to local storage, URL parameters, diagnostics or rendered read-back fields.
  - The UI calls PostgreSQL or repository-source private endpoints directly.
  - Settings validation cannot distinguish invalid input, unreachable PostgreSQL and unsupported apply semantics.
  - Existing workspace and analysis UI flows regress without test coverage.
```

### Slice 10 - End-to-End Verification and Release Readiness

Purpose: run the targeted and repository quality gates, inspect diffs and
record final workflow execution evidence.

```yaml
slice_id: S10
profile: FULL_PATH
owner: Senior Tester
secondary_reviewers:
  - Quality Gate Orchestrator
  - Senior DevOps Engineer
  - Senior System Architect
  - Senior React Frontend Developer
affected_files:
  - docs/workflow/execution-report.md
  - docs/workflow/quality-and-leakage-gates.md
affected_modules:
  - repository-source-service
  - query-report-api-service
  - forensic-ui
affected_contracts: []
dependencies:
  - S09
parallel_group: P10
file_locks:
  - docs/workflow/execution-report.md
  - docs/workflow/quality-and-leakage-gates.md
contract_locks: []
architecture_locks:
  - release-readiness
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
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
  - Settings UI or API readiness is claimed without contract and frontend verification.
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
                -> S09
                  -> S10
```

No slices are safely parallelizable because the persistence decision, build
configuration, schema, adapter, runtime wiring, H2 test-boundary cutover,
Settings contract, Settings UI and release evidence form a single ordered
cutover.

## Role Ownership Map

| Role | Ownership |
|---|---|
| Senior Requirement Engineer | Requirement interpretation, EPIC drift and assumption tracking |
| Senior System Architect | ADR, arc42, hexagonal and service-boundary validation |
| Senior Java Backend Developer | Repository-source configuration, Liquibase, JDBC adapter and tests |
| Senior React Frontend Developer | Settings UI implementation, API adapter wiring and frontend tests |
| Senior UX Designer | Settings interaction design, accessibility and operator workflow clarity |
| Senior Tester | Regression strategy, quality gate selection and final verification |
| Data Ownership And Persistence Steward | One-writer persistence model and H2 retirement policy |
| Senior Analysis Storage Architect | Schema responsibility, metadata/provenance storage checks |
| Senior DevOps Engineer | Docker Compose, local runtime, dependency verification and operator commands |
| Security And Threat Modeling | Credentials, network, diagnostics and private path leakage review |
| Observability And Runtime Diagnostics | Health/readiness and sanitized failure reporting |
| Contract-First API Steward | Public Settings API and repository-source handoff governance |

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
Frontend Settings slices must run the verified `forensic-ui` test and build
commands.
Live PostgreSQL startup checks are optional unless the executing slice records
Docker availability and intentionally runs the runtime scenario.

## Documentation Synchronization Points

- S01 updates ADR and arc42 before implementation.
- S06 updates Docker runtime documentation.
- S07 updates H2 test-boundary and PostgreSQL runtime-default policy.
- S08 updates contract and service-boundary documentation for Settings.
- S09 updates frontend Settings behavior and UI-facing documentation when
  needed.
- S10 updates workflow execution evidence after verification.

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
- Settings ownership, security model or runtime apply semantics are unclear.
- UI Settings would expose or persist database credentials.
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
- H2 remains available only for tests and deterministic fixtures.
- Missing or unreachable PostgreSQL is reported through startup failure or
  storage health/readiness instead of fallback.
- Public Settings API and React Settings UI are implemented through
  contract-governed slices without exposing database credentials.
- Required quality gates pass and are recorded.
