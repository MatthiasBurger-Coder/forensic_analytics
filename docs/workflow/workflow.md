# Workflow: Architecture Progress Assessment Distribution

## Executive Summary

This workflow plans the documentation-only distribution of a project progress
and architecture assessment.

The full assessment must be stored as a source assessment document:

```text
docs/architecture/assessments/2026-06-architecture-progress-assessment.md
```

The assessment must not be copied wholesale into one arc42 chapter. Verified
architecture findings must be extracted into the existing authoritative arc42
structure under:

```text
docs/arc42/
```

The user-provided path `docs/architecture/arc42/**` was checked against the
repository and is not the current authoritative arc42 root. The repository
README classifies `docs/architecture/` as a historical and current
architecture source-map root, while authoritative arc42 architecture output
belongs under `docs/arc42/**`.

## Target Picture

The completed workflow will produce:

- one complete architecture assessment source document under
  `docs/architecture/assessments/`;
- extracted strategy findings in `docs/arc42/04-solution-strategy.md`;
- current-module versus target-service decomposition notes in
  `docs/arc42/05-building-block-view.md`;
- governance, quality-gate, documentation-gate and migration concepts in
  `docs/arc42/08-crosscutting-concepts.md`;
- ADR-state references in `docs/arc42/09-architecture-decisions.md`;
- the primary maturity, risk and technical-debt assessment in
  `docs/arc42/11-risks-and-technical-debt.md`.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Active workflow branch:
  `docs/workflow-architecture-assessment-20260606`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Current repository arc42 root: `docs/arc42/`
- Current architecture source-map root: `docs/architecture/`
- Current EPIC source root: `docs/epics/`
- Current ADR root for numbered ADRs:
  `docs/arc42/09-architecture-decisions/adr/`
- Current Gradle modules were verified from `settings.gradle.kts`.

Verified source-of-truth files:

- `AGENTS.md`
- `QUALITY.md`
- `.codex/AGENTS.md`
- `.codex/workflow/workflow-execution-rules.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/skills/execution-profile-router/SKILL.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/arc42-architecture-governance/SKILL.md`
- `.agents/skills/adr-steward/SKILL.md`
- `.agents/skills/documentation-sync/SKILL.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md`
- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md`
- `docs/architecture/README.md`
- `docs/architecture/current-state.md`
- `docs/architecture/target-microservices-architecture.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/data-ownership.md`
- `docs/arc42/README.md`
- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`
- ADR-0010, ADR-0013, ADR-0017, ADR-0023, ADR-0024 and ADR-0025 under
  `docs/arc42/09-architecture-decisions/adr/`

## Requirement Clarification Gate

Decision: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 86 percent.

Original request:

- Create a workflow for storing a project progress and architecture
  assessment as an assessment document.
- Do not copy the complete assessment into one arc42 chapter.
- Extract architecture findings into arc42 chapters 4, 5, 8, 9 and 11.
- Treat chapter 11 as the main location for maturity, architecture risk and
  technical debt.

Interpreted intent:

- Create an executable documentation workflow that preserves the full
  assessment as source evidence and distributes only the relevant
  architecture findings into the verified arc42 structure.

Change type:

- Documentation and architecture-governance workflow.

Affected process strand:

- `workflow create` now.
- `workflow execute` later for the planned documentation slices.

Affected architecture areas:

- microservice target strategy;
- service decomposition and current-module mapping;
- gRPC ingestion and contract-first communication;
- worker and migration concepts;
- workflow governance, skills, agents and quality gates;
- PostgreSQL and H2 persistence decisions;
- architecture risk and technical debt.

Explicit requirements:

- Store the complete assessment as
  `docs/architecture/assessments/2026-06-architecture-progress-assessment.md`.
- Extract findings to `docs/arc42/04-solution-strategy.md`.
- Extract findings to `docs/arc42/05-building-block-view.md`.
- Extract findings to `docs/arc42/08-crosscutting-concepts.md`.
- Extract findings to `docs/arc42/09-architecture-decisions.md`.
- Extract findings primarily to
  `docs/arc42/11-risks-and-technical-debt.md`.
- Keep the assessment and the arc42 extracts separate.

Accepted assumptions:

- The text supplied in the request is a placement and extraction instruction,
  not the full project progress assessment.
- During `workflow execute`, Slice 01 must verify that the full assessment text
  is available from user input or a repository file before creating the
  assessment document.
- The acronym `SCA` must be expanded from the assessment source or existing
  repository context before it is documented in arc42. The workflow must not
  guess whether it means source-code analysis, software composition analysis or
  another concept.
- `docs/architecture/assessments/` is acceptable as a source-assessment
  location because `docs/architecture/README.md` classifies
  `docs/architecture/` as a source-map root, not as authoritative arc42 output.
- arc42 extracts must use `docs/arc42/**`, not
  `docs/architecture/arc42/**`.

Non-goals:

- No backend implementation.
- No frontend implementation.
- No Docker, runtime, gRPC, REST, persistence, analysis-engine, Joern,
  JavaParser, BTM generator or analytics behavior changes.
- No new service extraction.
- No contract mutation.
- No build logic mutation.
- No claim that target services are production-ready without verified start,
  health, container and deployment evidence.
- No rewrite or renumbering of ADR history.
- No invented maturity score, module status, service status, table name,
  endpoint, graph label, runtime behavior or evidence fact.

Open questions:

- None blocking for workflow authoring.

Execution prerequisites:

- Slice 01 must stop if the full assessment source text is unavailable.
- Slices that mention `SCA` must stop until the acronym is verified.

## Execution Profile

```text
executionProfile=FULL_PATH
reason=The workflow plans architecture documentation, ADR references, service-boundary risks, persistence decisions and workflow structure updates.
requiredFullReviews=Senior Workflow Architect, Senior Requirement Engineer, Senior System Architect, Senior Java Backend Developer, Senior React Frontend Developer, Senior Tester, ADR Steward, Senior Documentation Engineer
roleReviewBudget=full workflow-create review
allowedImpactChecks=Senior React Frontend Developer may report N/A impact for implementation, but must review that no UI behavior or UX claim is introduced.
requiredQualityChecks=documentation-only checks from QUALITY.md plus git diff inspection
stopConditions=missing assessment source, wrong arc42 root, unclear SCA meaning, unverifiable service readiness claim, unverified ADR decision, product-scope change
```

## Role Review

Senior Workflow Architect:

- Branch-first workflow creation was completed before mutating workflow files.
- The workflow is split into five documentation slices with concrete
  dependencies and non-overlapping write scopes where possible.
- The workflow regenerates `docs/workflow` and records context-pack metadata.

Senior Requirement Engineer:

- The request is documentation and architecture-governance scope.
- The full assessment remains source evidence; extracted arc42 content remains
  architecture interpretation.
- The workflow preserves EPIC v0.2 as the product requirement baseline and
  records the missing full assessment source as an execution prerequisite.

Senior System Architect:

- The verified authoritative arc42 root is `docs/arc42/**`.
- The target microservice landscape, service-boundary rules and data-ownership
  rules must be taken from existing arc42, ADR and architecture source-map
  documents before new statements are added.
- Current modules must be described as current or transitional evidence, not
  as completed production microservices.

Senior Java Backend Developer:

- No Java backend implementation is in scope.
- Backend-facing statements about gRPC ingestion, services, worker behavior,
  persistence or contracts must be verified from ADRs, contracts, service
  READMEs, build files or source before they are described as implemented.

Senior React Frontend Developer:

- No frontend implementation is in scope.
- UI or client behavior must not be added to the assessment extracts unless it
  is verified from `forensic-ui`, `cli-client`, public API documentation or
  existing architecture docs.

Senior Tester:

- Documentation-only execution requires path checks, JSON syntax validation,
  `git diff --check` and diff inspection.
- The repository Gradle quality gate is not required for documentation-only
  slices unless execution changes product source, tests, build logic,
  contracts, runtime, deployment or quality policy.

ADR Steward:

- The workflow may reference accepted ADRs and the consolidated ADR state.
- If an extract introduces a new architecture decision or contradicts an
  accepted ADR, execution must stop for ADR review instead of editing ADR
  history silently.

Senior Documentation Engineer:

- Documentation must be English.
- Planned behavior, target architecture, current implementation evidence,
  source assessment claims and unresolved risks must remain distinguishable.
- The workflow must not duplicate the complete assessment inside arc42.

## Architecture Constraints

- Use `docs/arc42/**` for authoritative arc42 output.
- Use `docs/architecture/assessments/**` only for the full source
  assessment.
- Do not create `docs/architecture/arc42/**`.
- Do not treat `docs/architecture/**` source-map files as newer authoritative
  arc42 output where checked arc42 files already exist.
- Do not describe current service roots as production-ready microservices
  without verified independent build, start, test, configuration, healthcheck,
  container and deployment evidence.
- Do not claim shared domain or application modules are active implementation
  unless execution verifies that from the current source tree.
- Keep PostgreSQL bounded to `repository-source-service` workspace metadata
  under ADR-0024.
- Keep H2 limited to deterministic tests and direct fixtures under ADR-0023.
- Keep gRPC and REST changes contract-first under ADR-0010.
- Keep per-service data ownership under ADR-0013.

## Scope

In scope:

- `docs/workflow/**` workflow-control files.
- `docs/architecture/assessments/2026-06-architecture-progress-assessment.md`
  during workflow execution.
- `docs/arc42/04-solution-strategy.md`.
- `docs/arc42/05-building-block-view.md`.
- `docs/arc42/08-crosscutting-concepts.md`.
- `docs/arc42/09-architecture-decisions.md`.
- `docs/arc42/11-risks-and-technical-debt.md`.

Out of scope:

- Product source code.
- Tests, unless a future implementation slice changes product behavior.
- Build scripts.
- Runtime configuration.
- Contract files under `contracts/**`.
- Deployment descriptors.
- ADR renumbering or history rewrite.

## Backend Assessment

The current Gradle build registers these modules:

```text
analysis-orchestrator-service
analysis-store-service
btm-generation-service
cli-client
forensic-gateway-service
forensic-ingestion-service
graph-replay-service
ingestion-service
java-ast-analysis-service
java-parser-analysis-service
joern-analysis-service
joern-cpg-analysis-service
observability-stack
query-report-api-service
report-generation-service
repository-analysis-service
repository-source-service
testbed
```

Existing architecture documents classify some modules as target service
evidence, some as predecessor evidence and some as optional or planned roots.
The workflow must preserve those distinctions instead of flattening them into
one service-readiness claim.

## Frontend Assessment

The verified frontend root is `forensic-ui`, and `cli-client` is the public
API client boundary. The requested workflow does not change frontend behavior.
Frontend-related statements in the assessment extracts must stay limited to
verified client-facing architecture or explicitly planned behavior.

## Test Strategy

For this documentation-only workflow:

- run `python3 -m json.tool docs/workflow/context-pack.json`;
- run path checks for every workflow-control file;
- run `git diff --check`;
- inspect changed files with `git diff --name-only`;
- inspect the diff before completion.

Do not run or claim the Gradle gate for documentation-only slices unless a
later slice changes product source, tests, build logic, contracts, runtime,
deployment or quality policy.

The repository minimum quality command remains:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full local quality gate remains:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Resilience Requirements

This workflow does not change runtime resilience behavior. Documentation must
preserve the existing architecture distinction between target resilience,
verified runtime behavior and open readiness gaps.

## Slice Dependency Graph

```text
Slice 01
  -> Slice 02
  -> Slice 03
  -> Slice 04
  -> Slice 05
```

Slice 02, Slice 03 and Slice 04 may be reviewed independently after Slice 01
verifies the full assessment source. They should be applied sequentially during
workflow execution to keep arc42 diff review simple.

## Ordered Slices

### Slice 01 - Store Full Architecture Assessment Source

Purpose:

- Create the full architecture progress assessment source document without
  copying it into arc42.

Prerequisites:

- Active branch must be `docs/workflow-architecture-assessment-20260606`.
- Full assessment source text must be available from user input or a verified
  repository file.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Documentation Engineer
secondary_reviewers:
  - Senior Requirement Engineer
  - Senior System Architect
affected_files:
  - docs/architecture/assessments/2026-06-architecture-progress-assessment.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: P1
file_locks:
  - docs/architecture/assessments/2026-06-architecture-progress-assessment.md
contract_locks: []
architecture_locks:
  - architecture-source-assessment
quality_gates:
  targeted:
    - test -f docs/architecture/assessments/2026-06-architecture-progress-assessment.md
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: source assessment only; no arc42 chapter update in this slice
  adr: no ADR change expected
stop_conditions:
  - full assessment source text is unavailable
  - the slice would invent missing assessment content
  - the slice would store authoritative arc42 output under docs/architecture/arc42
  - the assessment text is not clearly separated from extracted arc42 findings
```

Done criteria:

- The complete assessment source exists at the requested assessment path.
- The document clearly labels itself as an assessment source, not as an arc42
  chapter or ADR.
- No arc42 chapter receives the complete assessment text.

### Slice 02 - Add Primary Risk And Technical Debt Findings

Purpose:

- Extract maturity, risk and debt findings into arc42 chapter 11 as the main
  architecture-risk location.

Prerequisites:

- Slice 01 completed or verified assessment source is available.

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers:
  - Senior Requirement Engineer
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/arc42/11-risks-and-technical-debt.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: P2
file_locks:
  - docs/arc42/11-risks-and-technical-debt.md
contract_locks: []
architecture_locks:
  - microservice-risk
  - service-boundary-risk
  - data-ownership-risk
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: docs/arc42/11-risks-and-technical-debt.md
  adr: reference ADR-0010, ADR-0013, ADR-0017, ADR-0023, ADR-0024 and ADR-0025 when relevant
stop_conditions:
  - a risk claim cannot be traced to the assessment, arc42, ADR, architecture source-map docs or current repository files
  - current shared domain/application modules are claimed without source-tree verification
  - target service readiness is claimed without verified readiness evidence
  - monolithic module debt is described as active implementation when the verified files classify it as predecessor or historical evidence
```

Required extraction topics:

- current architecture maturity;
- distributed monolith risk;
- target architecture being ahead of verified current module/runtime
  readiness;
- shared-domain or shared-application risk, phrased as verified current state
  only if execution proves active shared modules;
- unstable or early gRPC contract stabilization risk;
- wrong service-cut risk;
- per-service persistence-boundary risk;
- migration debt from predecessor, placeholder, optional or monolithic
  remaining modules.

Done criteria:

- Chapter 11 contains the primary risk and technical-debt summary.
- Risk wording separates verified current state, target architecture and
  unresolved assessment findings.

### Slice 03 - Synchronize Strategy And Building Blocks

Purpose:

- Add target strategy and current-module versus target-service decomposition
  findings to arc42 chapters 4 and 5.

Prerequisites:

- Slice 01 completed or verified assessment source is available.
- The meaning of `SCA` is verified before documenting it.

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers:
  - Senior Java Backend Developer
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/arc42/04-solution-strategy.md
  - docs/arc42/05-building-block-view.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: P3
file_locks:
  - docs/arc42/04-solution-strategy.md
  - docs/arc42/05-building-block-view.md
contract_locks: []
architecture_locks:
  - target-microservices-strategy
  - current-to-target-service-decomposition
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: docs/arc42/04-solution-strategy.md and docs/arc42/05-building-block-view.md
  adr: reference ADR-0017 and ADR-0025 when relevant
stop_conditions:
  - SCA meaning is ambiguous
  - current modules are treated as completed production microservices without readiness evidence
  - worker model, gRPC ingestion or service decomposition statements cannot be verified
  - a new service boundary is introduced without ADR or workflow scope
```

Required extraction topics:

- microservice target ecosystem;
- gRPC ingestion as target communication/ingestion strategy;
- worker model and migration strategy, limited to verified or explicitly
  planned behavior;
- SCA migration strategy after acronym verification;
- current Gradle modules versus FA-MSA-001 target services;
- predecessor/current/optional/planned service roots, preserving existing
  documentation distinctions.

Done criteria:

- Chapter 4 summarizes target strategy without claiming unverified runtime
  readiness.
- Chapter 5 maps current modules and target services using verified source-map
  and ADR evidence.

### Slice 04 - Synchronize Crosscutting Governance And Decisions

Purpose:

- Add governance, quality-gate, documentation-gate, migration-concept and ADR
  reference findings to arc42 chapters 8 and 9.

Prerequisites:

- Slice 01 completed or verified assessment source is available.
- ADR references are verified from current numbered ADR files under the arc42
  ADR chapter.

```yaml
slice_id: S04
profile: FULL_PATH
owner: ADR Steward
secondary_reviewers:
  - Senior System Architect
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/arc42/08-crosscutting-concepts.md
  - docs/arc42/09-architecture-decisions.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: P4
file_locks:
  - docs/arc42/08-crosscutting-concepts.md
  - docs/arc42/09-architecture-decisions.md
contract_locks: []
architecture_locks:
  - agent-governance
  - adr-reference-state
  - persistence-decision-state
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: docs/arc42/08-crosscutting-concepts.md and docs/arc42/09-architecture-decisions.md
  adr: no new ADR expected unless execution discovers a new decision
stop_conditions:
  - an accepted ADR is contradicted
  - a new architecture decision is introduced without ADR Steward approval
  - PostgreSQL is described as broader analytics persistence instead of bounded repository-source metadata
  - H2 is described as runtime fallback
  - workflow governance wording changes process authority instead of documenting existing rules
```

Required extraction topics:

- `workflow create` and `workflow execute` governance;
- Skill/Agent model;
- quality gates and documentation gates;
- SCA as a crosscutting migration concept after acronym verification;
- consolidated ADR state;
- PostgreSQL runtime persistence bounded to repository-source workspace
  metadata;
- no H2 runtime fallback;
- gRPC communication and contract-first governance;
- independent service deployment as a target requirement, not a verified
  readiness claim.

Done criteria:

- Chapter 8 captures crosscutting governance and migration concepts.
- Chapter 9 references current accepted ADR state without rewriting ADR
  history.

### Slice 05 - Documentation Closure And Handoff

Purpose:

- Verify consistency, update the workflow execution report, and prepare the
  handoff back to workflow execution or commit preparation.

Prerequisites:

- Slices 01 through 04 completed or explicitly stopped with documented
  blockers.

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior Tester
secondary_reviewers:
  - Senior Documentation Engineer
  - Senior Requirement Engineer
  - Senior System Architect
affected_files:
  - docs/workflow/execution-report.md
affected_modules: []
affected_contracts: []
dependencies:
  - S02
  - S03
  - S04
parallel_group: P5
file_locks:
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - documentation-closure
quality_gates:
  targeted:
    - python3 -m json.tool docs/workflow/context-pack.json
    - test -f docs/workflow/workflow.md
    - test -f docs/workflow/context-pack.md
    - test -f docs/workflow/context-pack.json
    - git diff --check
    - git diff --name-only
  required:
    - git diff --check
documentation:
  arc42: verify chapters 4, 5, 8, 9 and 11
  adr: verify no ADR history rewrite
stop_conditions:
  - documentation contradicts EPIC, ADR or verified repository state
  - assessment source and arc42 extracts collapse into duplicated chapter content
  - JSON context-pack validation fails
  - diff contains product source, build, contract, runtime or deployment changes
```

Done criteria:

- Documentation-only checks pass or blockers are reported.
- The execution report lists changed files, verification commands and any
  unresolved gaps.
- The workflow is ready for commit preparation only after diff inspection.

## Parallelization Opportunities

- Slice 02, Slice 03 and Slice 04 have distinct arc42 write scopes and may be
  reviewed independently after Slice 01, but execution should apply them
  sequentially for clearer architecture review.
- No slice may run before Slice 01 verifies the full assessment source.

## Role Ownership Map

| Area | Owner | Reviewers |
|---|---|---|
| Workflow structure | Senior Workflow Architect | Senior Requirement Engineer, Senior Tester |
| Assessment source document | Senior Documentation Engineer | Senior Requirement Engineer, Senior System Architect |
| Risk and debt chapter | Senior System Architect | Senior Documentation Engineer, Senior Tester |
| Strategy and building blocks | Senior System Architect | Senior Java Backend Developer, Senior Documentation Engineer |
| Crosscutting concepts and ADR references | ADR Steward | Senior System Architect, Senior Documentation Engineer |
| Quality closure | Senior Tester | Senior Documentation Engineer, Senior Requirement Engineer |

Callable subagents were not used during this workflow creation turn. The role
files listed in the verified baseline were used as local review checklists.

## Quality-Gate Expectations

Documentation-only workflow execution must run:

```bash
python3 -m json.tool docs/workflow/context-pack.json
test -f docs/workflow/workflow.md
test -f docs/workflow/context-pack.md
test -f docs/workflow/context-pack.json
git diff --check
git diff --name-only
```

If execution changes product source, tests, build logic, contracts, runtime,
deployment or quality policy, the slice must stop and reclassify the quality
gate from `QUALITY.md`.

## Documentation Synchronization Points

- Keep the full assessment source under `docs/architecture/assessments/`.
- Keep authoritative arc42 extracts under `docs/arc42/**`.
- Keep numbered ADR references under
  `docs/arc42/09-architecture-decisions/adr/`.
- Keep EPIC source references under `docs/epics/**`.
- Keep process-control workflow files under `docs/workflow/**`.

## Stop Conditions

Stop workflow execution if:

- the full assessment source is missing;
- the workflow would create `docs/architecture/arc42/**`;
- an arc42 extract would require guessing implementation, service readiness,
  persistence schema, contract shape, endpoint, table, graph label, runtime
  behavior or evidence fact;
- the `SCA` acronym cannot be verified;
- assessment claims conflict with ADR-0022, ADR-0023, ADR-0024 or ADR-0025 and
  the intended source of truth is unclear;
- a slice introduces a new architecture decision without ADR review;
- a slice modifies product source, tests, build logic, contracts, runtime or
  deployment files outside the workflow scope.

## Uncertainty Escalation Rules

- Missing full assessment text stops Slice 01 and asks for the assessment
  source.
- Ambiguous `SCA` meaning stops the slice that would document it.
- ADR conflict routes to ADR Steward and Senior System Architect.
- Service-boundary ambiguity routes to Senior System Architect and
  Microservice Senior Expert.
- Persistence-ownership ambiguity routes to Data Ownership and Persistence
  Steward.
- Contract ambiguity routes to Contract Governance and gRPC/Protobuf review.
- Quality-gate ambiguity routes to Senior Tester and quality-gate governance.

## Commit And Push Plan

No commit or push is authorized by this workflow creation request.

During later `workflow execute`, commit and push are allowed only when the
checked workflow explicitly authorizes a slice checkpoint. Slice checkpoint
push is not `push auto` and must not create or merge a pull request.

## Definition Of Done

The workflow is done when:

- `docs/workflow/workflow.md` is complete and checked;
- `docs/workflow/context-pack.md` exists;
- `docs/workflow/context-pack.json` is valid JSON;
- `docs/workflow/execution-report.md` exists as the execution reporting
  target;
- the active branch is still
  `docs/workflow-architecture-assessment-20260606`;
- `git diff --check` passes;
- the workflow clearly distinguishes full assessment storage from arc42
  extraction.

## Handoff To Workflow Execute

Run the next phase with:

```text
workflow execute
```

Before editing the assessment or arc42 chapters, workflow execution must read
this complete workflow, verify Slice 01 source availability and execute the
slices in dependency order.

## arc42 Check Status

Checked during workflow creation:

- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`

Required during workflow execution:

- update chapters 4, 5, 8, 9 and 11 according to the slice plan;
- keep chapter 11 as the primary risk and technical-debt chapter;
- keep the assessment document outside arc42;
- do not create `docs/architecture/arc42/**`.
