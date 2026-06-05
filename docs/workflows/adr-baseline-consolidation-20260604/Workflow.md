# Workflow: ADR Baseline Consolidation

## Executive Summary

This workflow consolidates the architecture decision baseline into the arc42
documentation structure. Workflow control artifacts remain outside
`docs/arc42/`; architecture output does not.

The governing principle is:

```text
Workflow.md = process control
docs/arc42/ = complete architecture documentation
```

All architecture, ADR, requirement, conflict-analysis and final-report outputs
created or updated by this workflow must be placed under `docs/arc42/`.

## Verified Baseline

- Workflow control file:
  `docs/workflows/adr-baseline-consolidation-20260604/Workflow.md`
- Canonical execution entrypoint:
  `docs/workflow/workflow.md`
- Active branch: `architecture/workflow-adr-baseline-consolidation-20260605`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Current repository contains flat arc42 chapter files under `docs/arc42/`.
- Current repository contains historical ADRs under `docs/adr/`.
- Current repository contains historical architecture documents under
  `docs/architecture/`.
- This workflow defines the target placement for the ADR baseline
  consolidation outputs. It does not move historical documents by itself.

Verified source-of-truth files:

- `AGENTS.md`
- `QUALITY.md`
- `.codex/AGENTS.md`
- `.codex/workflow/workflow-execution-rules.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/arc42-architecture-governance/SKILL.md`
- `docs/arc42/README.md`
- `docs/adr/README.md`

## Requirement Clarification Gate

Decision: `READY_FOR_WORKFLOW`

Confidence: 92 percent.

Original request:

- Keep `docs/workflows/adr-baseline-consolidation-20260604/Workflow.md`
  as the process file only.
- Route every other architecture, ADR, requirement, conflict-analysis and
  final-report document into `docs/arc42/`.
- Stop creating new authoritative documentation under `docs/architecture/`,
  `docs/adr/` and `docs/requirements/`.

Interpreted intent:

- Create a workflow control file that makes `docs/arc42/` the authoritative
  documentation root for this ADR baseline consolidation.
- Preserve the workflow file as process control.
- Prevent future workflow slices from scattering authoritative architecture
  output across separate documentation roots.

Explicit requirements:

- Only this workflow control file may remain outside `docs/arc42/`.
- New authoritative architecture documents must be created under
  `docs/arc42/`.
- ADRs must be placed under
  `docs/arc42/09-architecture-decisions/adr/`.
- ADR inventory must be placed under
  `docs/arc42/09-architecture-decisions/inventory/`.
- ADR conflict analysis must be placed under
  `docs/arc42/09-architecture-decisions/conflicts/`.
- The final report must be placed under
  `docs/arc42/09-architecture-decisions/reports/`.
- Requirement alignment output must be placed under
  `docs/arc42/01-introduction-and-goals/requirements/`.
- Persistence concepts must be documented under
  `docs/arc42/08-crosscutting-concepts/persistence/`.
- Service boundaries must be documented under
  `docs/arc42/05-building-block-view/`.
- Plugin/server context must be documented under
  `docs/arc42/03-system-scope-and-context/`.

Accepted assumptions:

- Existing `docs/adr/` and `docs/architecture/` files are historical repository
  inputs until an approved slice moves, mirrors or replaces their authoritative
  content.
- Compatibility pointer stubs are allowed only when a repository compatibility
  rule is verified.
- Compatibility pointer stubs must not duplicate architecture content.

Non-goals:

- No product source changes.
- No backend, frontend, Docker/runtime, gRPC, REST, persistence, analysis
  engine, Joern, JavaParser, BTM generator or analytics behavior changes.
- No runtime migration strategy.
- No invented implementation, runtime, deployment or migration flow.
- No automatic rewrite of historical ADR intent.
- No broad cleanup outside this workflow scope.

Open questions:

- None blocking for creating this workflow control file.

## Role Review

Senior Requirement Engineer:

- The workflow has a documentation and architecture-governance scope.
- The requirement is traceable to the user request and does not authorize
  product implementation.
- Requirements, ADR inventory and final reports are routed into arc42 chapters.

Senior System Architect:

- The workflow changes documentation placement governance only.
- The target arc42 structure separates constraints, context, strategy,
  building blocks, runtime, deployment, crosscutting concepts, decisions,
  quality requirements, risks and glossary.
- Existing architecture facts must not be invented while reorganizing
  documentation.

Senior Java Backend Developer:

- No Java backend implementation is in scope.
- Backend, persistence and service-boundary references in future outputs must
  be verified from repository source, ADRs, arc42, build files and tests before
  they are documented as implemented behavior.

Senior React Frontend Developer:

- No frontend implementation is in scope.
- Any future UI-related architecture statements must distinguish planned UI
  flows from implemented UI behavior.

Senior Tester:

- Documentation-only closure requires `git diff --check` and arc42 placement
  inspection.
- Product quality gates become required only if a future slice changes product
  source, tests, build logic, contracts or runtime behavior.

## Mandatory arc42 Documentation Structure

This workflow treats `docs/arc42/` as the authoritative documentation root for
all architecture, ADR, requirement, conflict-analysis and final-report
documents.

The workflow control documents that may remain outside `docs/arc42/` for this
workflow are:

```text
docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
docs/workflow/workflow.md
docs/workflow/context-pack.md
docs/workflow/context-pack.json
```

All other documents created or updated by this workflow must be placed under:

```text
docs/arc42/
```

Do not create new authoritative documentation under:

```text
docs/architecture/
docs/adr/
docs/requirements/
```

unless the repository already requires temporary compatibility stubs. If
compatibility stubs are needed, they must contain only a short pointer to the
authoritative arc42 location and must not contain duplicate architecture
content.

## arc42 Chapter Mapping

### 01 - Introduction and Goals

Use for requirement baseline and business or functional goals.

```text
docs/arc42/01-introduction-and-goals/
docs/arc42/01-introduction-and-goals/requirements/
```

Allowed documents:

```text
docs/arc42/01-introduction-and-goals/requirements/requirement-alignment-20260604.md
docs/arc42/01-introduction-and-goals/requirements/fa-mvp-0001-alignment.md
```

### 02 - Architecture Constraints

Use for hard boundaries and non-negotiable architecture constraints.

```text
docs/arc42/02-architecture-constraints/
```

Allowed documents:

```text
docs/arc42/02-architecture-constraints/persistence-constraints.md
docs/arc42/02-architecture-constraints/service-boundary-constraints.md
docs/arc42/02-architecture-constraints/workflow-governance-constraints.md
```

### 03 - System Scope and Context

Use for plugin/server boundaries, external systems and communication context.

```text
docs/arc42/03-system-scope-and-context/
```

Allowed documents:

```text
docs/arc42/03-system-scope-and-context/plugin-server-boundary.md
docs/arc42/03-system-scope-and-context/grpc-ingestion-context.md
```

### 04 - Solution Strategy

Use for the clean target architecture strategy.

```text
docs/arc42/04-solution-strategy/
```

Allowed documents:

```text
docs/arc42/04-solution-strategy/clean-target-architecture.md
docs/arc42/04-solution-strategy/no-runtime-migration-strategy.md
```

Important:

The solution strategy must explicitly state that this baseline is not a runtime
migration strategy.

### 05 - Building Block View

Use for service/module boundaries and the target service decomposition.

```text
docs/arc42/05-building-block-view/
```

Allowed documents:

```text
docs/arc42/05-building-block-view/service-boundaries.md
docs/arc42/05-building-block-view/workspace-repository-branch-analysis-model.md
```

### 06 - Runtime View

Use only if runtime flows are documented.

```text
docs/arc42/06-runtime-view/
```

Allowed documents:

```text
docs/arc42/06-runtime-view/plugin-submit-analysis-flow.md
docs/arc42/06-runtime-view/workspace-analysis-trigger-flow.md
```

No implementation flow may be invented if it does not exist yet.

### 07 - Deployment View

Use only if deployment-related documentation must be aligned.

```text
docs/arc42/07-deployment-view/
```

Allowed documents:

```text
docs/arc42/07-deployment-view/postgresql-runtime-deployment-baseline.md
```

Do not modify Docker or runtime configuration in this workflow.

### 08 - Crosscutting Concepts

Use for persistence, schema migration, service contracts and documentation
governance.

```text
docs/arc42/08-crosscutting-concepts/
docs/arc42/08-crosscutting-concepts/persistence/
docs/arc42/08-crosscutting-concepts/service-contracts/
docs/arc42/08-crosscutting-concepts/documentation-governance/
```

Allowed documents:

```text
docs/arc42/08-crosscutting-concepts/persistence/postgresql-liquibase-baseline.md
docs/arc42/08-crosscutting-concepts/persistence/h2-test-fixture-only.md
docs/arc42/08-crosscutting-concepts/service-contracts/grpc-contract-boundary.md
docs/arc42/08-crosscutting-concepts/documentation-governance/arc42-documentation-layout.md
```

### 09 - Architecture Decisions

Use for ADRs, ADR inventory, ADR conflict analysis and ADR reports.

```text
docs/arc42/09-architecture-decisions/
docs/arc42/09-architecture-decisions/adr/
docs/arc42/09-architecture-decisions/inventory/
docs/arc42/09-architecture-decisions/conflicts/
docs/arc42/09-architecture-decisions/reports/
```

Required workflow outputs:

```text
docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md
docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md
docs/arc42/09-architecture-decisions/adr/ADR-00XX-consolidated-architecture-baseline-without-migration.md
docs/arc42/09-architecture-decisions/reports/adr-baseline-consolidation-final-report-20260604.md
```

ADR files must no longer be created directly under:

```text
docs/adr/
```

unless an existing repository compatibility rule requires a pointer file.

### 10 - Quality Requirements

Use for quality gates and architecture quality requirements.

```text
docs/arc42/10-quality-requirements/
```

Allowed documents:

```text
docs/arc42/10-quality-requirements/architecture-quality-gates.md
docs/arc42/10-quality-requirements/documentation-quality-gates.md
```

### 11 - Risks and Technical Debt

Use for remaining contradictions, unresolved decisions and risks.

```text
docs/arc42/11-risks-and-technical-debt/
```

Allowed documents:

```text
docs/arc42/11-risks-and-technical-debt/open-adr-risks.md
docs/arc42/11-risks-and-technical-debt/outdated-documentation-debt.md
```

### 12 - Glossary

Use for shared terminology.

```text
docs/arc42/12-glossary/
```

Allowed documents:

```text
docs/arc42/12-glossary/architecture-terms.md
```

## Updated Allowed File Areas

Allowed file areas for this workflow:

```text
docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
docs/workflow/
docs/arc42/
README.md
QUALITY.md
```

`README.md` and `QUALITY.md` may only be changed if they contain directly
conflicting architecture, persistence, ADR or documentation-governance
statements.

Not allowed:

```text
src/
server/
client/
plugin/
services/
docker/
docker-compose.yml
build.gradle
settings.gradle
pom.xml
```

unless the change is documentation-only and does not alter executable behavior.

## Updated Target Output Paths

### ADR Inventory

Old path:

```text
docs/architecture/adr-inventory-20260604.md
```

New path:

```text
docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md
```

### ADR Conflict Analysis

Old path:

```text
docs/architecture/adr-conflict-analysis-20260604.md
```

New path:

```text
docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md
```

### Consolidated ADR

Old path:

```text
docs/adr/ADR-00XX-consolidated-architecture-baseline-without-migration.md
```

New path:

```text
docs/arc42/09-architecture-decisions/adr/ADR-00XX-consolidated-architecture-baseline-without-migration.md
```

### Requirement Alignment

Use:

```text
docs/arc42/01-introduction-and-goals/requirements/requirement-alignment-20260604.md
```

If FA-MVP-0001 is updated or analyzed separately, use:

```text
docs/arc42/01-introduction-and-goals/requirements/fa-mvp-0001-alignment.md
```

### Persistence Baseline

Use:

```text
docs/arc42/08-crosscutting-concepts/persistence/postgresql-liquibase-baseline.md
docs/arc42/08-crosscutting-concepts/persistence/h2-test-fixture-only.md
```

### Service Boundary Baseline

Use:

```text
docs/arc42/05-building-block-view/service-boundaries.md
```

### Plugin / Server Boundary

Use:

```text
docs/arc42/03-system-scope-and-context/plugin-server-boundary.md
```

### Final Report

Old path:

```text
docs/workflows/adr-baseline-consolidation-20260604/final-report.md
```

New path:

```text
docs/arc42/09-architecture-decisions/reports/adr-baseline-consolidation-final-report-20260604.md
```

The final report is documentation output and therefore belongs under arc42.
Only the workflow control file itself remains under `docs/workflows/`.

## Ordered Slices

### Slice 01 - Branch and Workflow Isolation

Purpose: Ensure the workflow control file exists on a dedicated workflow
branch and no active product workflow is modified.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Workflow Architect
secondary_reviewers:
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
  - docs/workflow/**
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G1
file_locks:
  - docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
  - docs/workflow/**
contract_locks: []
architecture_locks:
  - arc42 documentation placement governance
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: placement governance defined
  adr: no ADR history rewrite in this slice
stop_conditions:
  - active branch is main, master, develop or another shared branch
  - unrelated local changes appear
  - docs/workflow/workflow.md is not the ADR baseline execution entrypoint
```

Allowed output:

```text
docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
docs/workflow/**
```

These are workflow-control artifacts. Architecture outputs still belong under
`docs/arc42/`.

### Slice 02 - ADR Inventory

Purpose: Inventory existing ADRs and record their active, superseded,
historical or conflicting status without rewriting ADR history.

```yaml
slice_id: S02
profile: FULL_PATH
owner: ADR Steward
secondary_reviewers:
  - Senior System Architect
  - Senior Documentation Engineer
affected_files:
  - docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: G2
file_locks:
  - docs/arc42/09-architecture-decisions/inventory/
contract_locks: []
architecture_locks:
  - ADR inventory provenance
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: ADR inventory
  adr: read-only source input
stop_conditions:
  - an ADR file cannot be verified exactly
  - ADR status would require guessing
```

Expected output:

```text
docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md
```

### Slice 03 - Conflict Analysis

Purpose: Analyze contradictions between ADRs, arc42, architecture documents,
requirements and current repository evidence.

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers:
  - Senior Requirement Engineer
  - Senior Documentation Engineer
affected_files:
  - docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md
affected_modules: []
affected_contracts: []
dependencies:
  - S02
parallel_group: G3
file_locks:
  - docs/arc42/09-architecture-decisions/conflicts/
contract_locks: []
architecture_locks:
  - ADR and arc42 consistency
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: conflict analysis
  adr: read-only source input
stop_conditions:
  - architecture conflict cannot be resolved from verified source
  - planned behavior would be documented as implemented behavior
```

Expected output:

```text
docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md
```

### Slice 04 - Consolidated ADR Creation

Purpose: Create the consolidated architecture baseline ADR under the arc42 ADR
chapter without introducing runtime migration language.

```yaml
slice_id: S04
profile: FULL_PATH
owner: ADR Steward
secondary_reviewers:
  - Senior System Architect
  - Senior Requirement Engineer
  - Senior Tester
affected_files:
  - docs/arc42/09-architecture-decisions/adr/ADR-00XX-consolidated-architecture-baseline-without-migration.md
affected_modules: []
affected_contracts: []
dependencies:
  - S03
parallel_group: G4
file_locks:
  - docs/arc42/09-architecture-decisions/adr/
contract_locks: []
architecture_locks:
  - consolidated architecture baseline
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: consolidated ADR
  adr: new authoritative ADR location under arc42
stop_conditions:
  - next ADR number cannot be verified
  - baseline text introduces migration, fallback, legacy or sunset mechanics
```

Expected output:

```text
docs/arc42/09-architecture-decisions/adr/ADR-00XX-consolidated-architecture-baseline-without-migration.md
```

### Slice 05 - Supersede or Clarify Existing ADRs

Purpose: Move or mirror authoritative ADR content into the arc42 ADR chapter
and prevent conflicting authoritative ADR content outside arc42.

```yaml
slice_id: S05
profile: FULL_PATH
owner: ADR Steward
secondary_reviewers:
  - Senior System Architect
  - Senior Documentation Engineer
affected_files:
  - docs/arc42/09-architecture-decisions/adr/
affected_modules: []
affected_contracts: []
dependencies:
  - S04
parallel_group: G5
file_locks:
  - docs/arc42/09-architecture-decisions/adr/
contract_locks: []
architecture_locks:
  - ADR authoritative location
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: ADR placement
  adr: authoritative content under arc42
stop_conditions:
  - existing ADR intent would be rewritten without explicit decision
  - compatibility pointer content duplicates architecture content
```

Expected output:

```text
docs/arc42/09-architecture-decisions/adr/
```

If existing ADR files are outside arc42, move or mirror their authoritative
content into the arc42 ADR chapter. Do not leave conflicting authoritative ADR
content outside arc42.

### Slice 06 - Requirement Alignment

Purpose: Align requirements with the consolidated architecture baseline and
place the alignment under the arc42 introduction and goals chapter.

```yaml
slice_id: S06
profile: FULL_PATH
owner: Senior Requirement Engineer
secondary_reviewers:
  - Senior System Architect
  - Senior Tester
affected_files:
  - docs/arc42/01-introduction-and-goals/requirements/requirement-alignment-20260604.md
affected_modules: []
affected_contracts: []
dependencies:
  - S04
parallel_group: G5
file_locks:
  - docs/arc42/01-introduction-and-goals/requirements/
contract_locks: []
architecture_locks:
  - requirement to ADR traceability
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: requirement alignment
  adr: consolidated baseline referenced
stop_conditions:
  - requirement source cannot be verified
  - assumption would be converted into requirement
```

Expected output:

```text
docs/arc42/01-introduction-and-goals/requirements/requirement-alignment-20260604.md
```

### Slice 07 - Documentation Consistency Pass

Purpose: Align arc42 sections with the consolidated ADR baseline while keeping
historical or compatibility material clearly non-authoritative.

```yaml
slice_id: S07
profile: FULL_PATH
owner: Senior Documentation Engineer
secondary_reviewers:
  - Senior System Architect
  - Senior Requirement Engineer
affected_files:
  - docs/arc42/02-architecture-constraints/
  - docs/arc42/03-system-scope-and-context/
  - docs/arc42/04-solution-strategy/
  - docs/arc42/05-building-block-view/
  - docs/arc42/08-crosscutting-concepts/
  - docs/arc42/09-architecture-decisions/
  - docs/arc42/10-quality-requirements/
  - docs/arc42/11-risks-and-technical-debt/
  - docs/arc42/12-glossary/
affected_modules: []
affected_contracts: []
dependencies:
  - S05
  - S06
parallel_group: G6
file_locks:
  - docs/arc42/
contract_locks: []
architecture_locks:
  - arc42 consistency
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: consistency pass
  adr: consolidated baseline referenced
stop_conditions:
  - new authoritative document is created outside docs/arc42/
  - runtime or deployment flow is invented
```

Expected output may affect:

```text
docs/arc42/02-architecture-constraints/
docs/arc42/03-system-scope-and-context/
docs/arc42/04-solution-strategy/
docs/arc42/05-building-block-view/
docs/arc42/08-crosscutting-concepts/
docs/arc42/09-architecture-decisions/
docs/arc42/10-quality-requirements/
docs/arc42/11-risks-and-technical-debt/
docs/arc42/12-glossary/
```

Do not create new authoritative documents outside arc42.

### Slice 08 - Final Quality Review

Purpose: Verify arc42 placement, documentation consistency and final workflow
closure.

```yaml
slice_id: S08
profile: FULL_PATH
owner: Senior Tester
secondary_reviewers:
  - Senior Documentation Engineer
  - Senior System Architect
affected_files:
  - docs/arc42/09-architecture-decisions/reports/adr-baseline-consolidation-final-report-20260604.md
affected_modules: []
affected_contracts: []
dependencies:
  - S07
parallel_group: G7
file_locks:
  - docs/arc42/09-architecture-decisions/reports/
contract_locks: []
architecture_locks:
  - arc42 placement verification
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: final report
  adr: final baseline status reported
stop_conditions:
  - arc42 documentation placement cannot be verified safely
  - production code changed during this workflow
```

Expected output:

```text
docs/arc42/09-architecture-decisions/reports/adr-baseline-consolidation-final-report-20260604.md
```

## Slice Dependency Summary

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

S05 and S06 may be reviewed in parallel after S04 only when their file locks
remain disjoint.

## Additional Quality Gate: arc42 Placement

Every slice must verify:

```text
All newly created or updated architecture documents are located under docs/arc42/.
Only workflow-control artifacts are allowed outside docs/arc42/.
No new authoritative ADR, requirement, architecture, conflict-analysis or report document exists outside docs/arc42/.
```

If this cannot be guaranteed:

```text
STOP: arc42 documentation placement cannot be verified safely.
```

## Test Strategy

Documentation-only targeted checks:

```bash
git diff --check
test -f docs/workflows/adr-baseline-consolidation-20260604/Workflow.md
test -f docs/workflow/workflow.md
git diff --name-only | sort
```

Slice-specific placement checks during workflow execution:

```bash
git diff --name-only | rg -v '^(docs/workflows/adr-baseline-consolidation-20260604/Workflow.md|docs/workflow/.*|docs/arc42/.*|README.md|QUALITY.md)$' && false || true
git diff --name-only | rg '^(src/|server/|client/|plugin/|services/|docker/|build.gradle|settings.gradle|pom.xml)' && false || true
```

The minimum repository quality command from `QUALITY.md` remains:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full local quality gate from `QUALITY.md` remains:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not run or claim product quality gates for documentation-only slices unless
the slice changes product source, tests, build logic, contracts, runtime,
persistence, deployment or quality policy.

## Stop Conditions

Stop and report if:

- the active branch is `main`, `master`, `develop` or another shared branch;
- unrelated local changes appear;
- the workflow control file is created outside
  `docs/workflows/adr-baseline-consolidation-20260604/Workflow.md`;
- `docs/workflow/workflow.md` does not mirror this ADR baseline workflow for
  executor discovery;
- a new authoritative architecture, ADR, requirement, conflict-analysis or
  final-report document is created outside `docs/arc42/`;
- existing ADR history would be rewritten without an explicit architecture
  decision;
- a runtime, deployment, migration, fallback, legacy, sunset or compatibility
  mechanic would need to be invented;
- production source, tests, build logic, contracts, Docker/runtime or analytics
  implementation changes become necessary;
- PostgreSQL, Liquibase or H2 semantics cannot be verified from repository
  ADRs, arc42, source, tests or documentation;
- compatibility stubs duplicate architecture content instead of pointing to
  the authoritative arc42 location.

## Definition Of Done

This workflow is done only when:

- the workflow control file exists under
  `docs/workflows/adr-baseline-consolidation-20260604/Workflow.md`;
- the canonical execution entrypoint exists under `docs/workflow/workflow.md`;
- every other generated or updated architecture document is under
  `docs/arc42/`;
- ADRs are placed under `docs/arc42/09-architecture-decisions/adr/`;
- ADR inventory is placed under
  `docs/arc42/09-architecture-decisions/inventory/`;
- ADR conflict analysis is placed under
  `docs/arc42/09-architecture-decisions/conflicts/`;
- the final report is placed under
  `docs/arc42/09-architecture-decisions/reports/`;
- requirements are aligned under
  `docs/arc42/01-introduction-and-goals/requirements/`;
- persistence concepts are documented under
  `docs/arc42/08-crosscutting-concepts/persistence/`;
- service boundaries are documented under
  `docs/arc42/05-building-block-view/`;
- plugin/server context is documented under
  `docs/arc42/03-system-scope-and-context/`;
- no authoritative architecture document outside arc42 contradicts the
  consolidated baseline;
- no active workflow was touched;
- no production code was changed;
- PostgreSQL is runtime persistence;
- Liquibase is the schema evolution mechanism;
- H2 is test/local-fixture only;
- no migration language remains authoritative;
- no SCA, Legacy, New, Fallback or Sunset mechanics are authoritative in this
  baseline.

## Handoff To Workflow Execute

`workflow execute` may execute this workflow only after reading this complete
file and verifying all slice metadata. Execution must preserve the arc42
placement gate for every slice and must stop before any product implementation
or unverifiable architecture claim.
