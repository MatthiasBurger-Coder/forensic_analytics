# Workflow: Architecture Entry arc42 Placement

## Executive Summary

This workflow plans a documentation-only classification and synchronization of
architecture entries into the existing arc42 documentation.

The key correction is that a complete project progress assessment is not
required to determine where architecture entries belong. The official arc42
template and section guidance provide the placement rules. Existing repository
architecture entries can therefore be classified first, and the full assessment
source can be stored later only when its complete text is available.

The workflow must not invent missing assessment content. It must classify
verified architecture entries and explicitly mark unresolved terms, missing
source text or unverified readiness claims.

Authoritative arc42 output belongs under:

```text
docs/arc42/
```

The architecture source-map and assessment root remains:

```text
docs/architecture/
```

Do not create:

```text
docs/architecture/arc42/
```

## arc42 Placement Rule Source

The placement rules are based on the official arc42 template/documentation:

- Template download requested by the user:
  `https://github.com/arc42/arc42-template/raw/master/dist/arc42-template-EN-plain-markdownMP.zip`
- Section 4: `https://docs.arc42.org/section-4/`
- Section 5: `https://docs.arc42.org/section-5/`
- Section 8: `https://docs.arc42.org/section-8/`
- Section 9: `https://docs.arc42.org/section-9/`
- Section 11: `https://docs.arc42.org/section-11/`

Use these rules during execution:

| Entry type | arc42 target | Rule |
|---|---|---|
| Fundamental solution approach, target architecture strategy, top-level decomposition, quality-goal approach, organizational strategy | `docs/arc42/04-solution-strategy.md` | Keep it short and link to detailed sections. |
| Static structure, modules, components, source-code mapping, current modules versus target services | `docs/arc42/05-building-block-view.md` | Describe building blocks, responsibilities, interfaces and code locations where relevant. |
| Runtime scenario, interaction flow, important behavior, error or exception flow | `docs/arc42/06-runtime-view.md` | Use when the entry describes behavior over time rather than static structure. |
| Infrastructure, deployment topology, environments, containers, runtime platform readiness | `docs/arc42/07-deployment-view.md` | Use only for verified deployment topology or explicitly planned deployment material. |
| Crosscutting rule, concept, pattern, governance model, quality gate, data ownership concept, persistence concept, security, observability, evidence integrity | `docs/arc42/08-crosscutting-concepts.md` | Use for concepts that affect multiple building blocks. |
| Important decision, selected alternative, risky or expensive architecture choice, ADR reference | `docs/arc42/09-architecture-decisions.md` or numbered ADR | Avoid duplication; reference existing ADRs before creating new ones. |
| Known problem, unresolved gap, maturity concern, technical debt, risk and mitigation | `docs/arc42/11-risks-and-technical-debt.md` | Primary location for maturity/risk/debt entries. |
| Domain or technical term that needs consistent language | `docs/arc42/12-glossary.md` | Use when the entry is terminology rather than strategy, structure, concept, decision or risk. |

## Target Picture

The completed workflow will produce:

- a placement assessment matrix under
  `docs/architecture/assessments/2026-06-arc42-placement-assessment.md`;
- optional full assessment source storage under
  `docs/architecture/assessments/2026-06-architecture-progress-assessment.md`
  only when the complete source text is available;
- extracted strategy findings in `docs/arc42/04-solution-strategy.md`;
- current-module versus target-service decomposition notes in
  `docs/arc42/05-building-block-view.md`;
- governance, quality-gate, documentation-gate and migration concepts in
  `docs/arc42/08-crosscutting-concepts.md`;
- ADR-state references in `docs/arc42/09-architecture-decisions.md`;
- primary maturity, risk and technical-debt findings in
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

Decision: `READY_FOR_WORKFLOW`

Confidence: 92 percent.

Original request:

- Revise the active workflow to incorporate the insight that arc42 itself
  contains the placement knowledge for deciding where architecture entries
  belong.

Interpreted intent:

- Change the workflow from assessment-source-first extraction to
  arc42-rule-first classification of existing architecture entries.
- Keep full assessment storage as optional source capture, not as the blocking
  prerequisite for placement classification.

Change type:

- Documentation and architecture-governance workflow revision.

Affected process strand:

- `workflow create` now.
- `workflow execute` later for the revised documentation slices.

Affected architecture areas:

- arc42 documentation structure;
- architecture source-map classification;
- microservice target strategy;
- service decomposition and current-module mapping;
- gRPC ingestion and contract-first communication;
- workflow governance, skills, agents and quality gates;
- PostgreSQL and H2 persistence decisions;
- architecture risk and technical debt.

Explicit requirements:

- Use official arc42 template knowledge to classify architecture entries.
- Determine placement per architecture entry.
- Preserve `docs/arc42/**` as the authoritative arc42 root.
- Preserve `docs/architecture/**` as architecture source-map and assessment
  input.
- Do not block classification just because the full assessment source text is
  missing.
- Do not invent missing assessment content.

Accepted assumptions:

- Existing architecture entries are available in `docs/architecture/**`,
  `docs/arc42/**`, ADRs, EPIC files and workflow files.
- The user-provided assessment-placement instruction is a classification input
  but not the full assessment source.
- If the complete progress assessment text becomes available, it can be stored
  as source evidence without changing the placement rules.
- `SCA` remains unresolved until a verified source expands it. The placement
  matrix may record it as an unresolved term and candidate concept, but arc42
  extracts must not define its meaning by guess.

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

- None blocking for the revised workflow.

Execution prerequisites:

- Official arc42 placement rules must be available from this workflow, the
  official documentation URLs, or the downloaded official template.
- Existing architecture entries must be read from verified repository files
  before classification.

## Execution Profile

```text
executionProfile=FULL_PATH
reason=The workflow plans architecture documentation, ADR references, service-boundary risks, persistence decisions and workflow structure updates.
requiredFullReviews=Senior Workflow Architect, Senior Requirement Engineer, Senior System Architect, Senior Java Backend Developer, Senior React Frontend Developer, Senior Tester, ADR Steward, Senior Documentation Engineer
roleReviewBudget=full workflow-create review
allowedImpactChecks=Senior React Frontend Developer may report N/A impact for implementation, but must review that no UI behavior or UX claim is introduced.
requiredQualityChecks=documentation-only checks from QUALITY.md plus git diff inspection
stopConditions=wrong arc42 root, unclear SCA meaning for authoritative extracts, unverifiable service readiness claim, unverified ADR decision, product-scope change
```

## Role Review

Senior Workflow Architect:

- Branch-first workflow creation was completed before mutating workflow files.
- The workflow now starts with an arc42 placement assessment matrix instead of
  requiring complete assessment source text.
- The workflow has five documentation slices with concrete dependencies and
  write scopes.

Senior Requirement Engineer:

- The revised workflow stays inside documentation and architecture-governance
  scope.
- Requirement traceability is improved because placement is based on official
  arc42 rules and verified repository entries.
- Missing full assessment source is now documented as optional source capture,
  not as a blocker for classification.

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
- UI or client behavior must not be added to the extracts unless it is
  verified from `forensic-ui`, `cli-client`, public API documentation or
  existing architecture docs.

Senior Tester:

- Documentation-only execution requires path checks, JSON syntax validation,
  `git diff --check` and diff inspection.
- The repository Gradle quality gate is not required for documentation-only
  slices unless execution changes product source, tests, build logic,
  contracts, runtime, deployment or quality policy.

ADR Steward:

- The workflow may reference accepted ADRs and the consolidated ADR state.
- If a classification or extract introduces a new architecture decision or
  contradicts an accepted ADR, execution must stop for ADR review instead of
  editing ADR history silently.

Senior Documentation Engineer:

- Documentation must be English.
- Planned behavior, target architecture, current implementation evidence,
  source assessment claims and unresolved risks must remain distinguishable.
- The placement assessment must not duplicate complete arc42 chapters.

## Architecture Constraints

- Use `docs/arc42/**` for authoritative arc42 output.
- Use `docs/architecture/assessments/**` for placement assessments and
  optional full assessment source text.
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
- `docs/architecture/assessments/2026-06-arc42-placement-assessment.md`.
- `docs/architecture/assessments/2026-06-architecture-progress-assessment.md`
  only when complete source text exists.
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
The placement matrix must preserve those distinctions instead of flattening
them into one service-readiness claim.

## Frontend Assessment

The verified frontend root is `forensic-ui`, and `cli-client` is the public
API client boundary. The requested workflow does not change frontend behavior.
Frontend-related statements in the placement matrix or extracts must stay
limited to verified client-facing architecture or explicitly planned behavior.

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
creates the placement assessment matrix. They should be applied sequentially
during workflow execution to keep arc42 diff review simple.

## Ordered Slices

### Slice 01 - Create arc42 Placement Assessment Matrix

Purpose:

- Classify existing architecture entries with official arc42 placement rules.
- Create the placement matrix that later slices use as their source for arc42
  updates.
- Optionally store the complete architecture progress assessment source if it
  is available, without blocking classification when it is not.

Prerequisites:

- Active branch must be `docs/workflow-architecture-assessment-20260606`.
- Official arc42 placement rules must be available from this workflow or the
  official arc42 sources listed above.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Documentation Engineer
secondary_reviewers:
  - Senior Requirement Engineer
  - Senior System Architect
affected_files:
  - docs/architecture/assessments/2026-06-arc42-placement-assessment.md
  - docs/architecture/assessments/2026-06-architecture-progress-assessment.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: P1
file_locks:
  - docs/architecture/assessments/2026-06-arc42-placement-assessment.md
  - docs/architecture/assessments/2026-06-architecture-progress-assessment.md
contract_locks: []
architecture_locks:
  - architecture-entry-placement
  - architecture-source-assessment
quality_gates:
  targeted:
    - test -f docs/architecture/assessments/2026-06-arc42-placement-assessment.md
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: placement assessment only; no authoritative arc42 chapter update in this slice
  adr: no ADR change expected
stop_conditions:
  - official arc42 placement rules cannot be verified from workflow or official sources
  - the slice would invent missing assessment content
  - the slice would store authoritative arc42 output under docs/architecture/arc42
  - a placement decision would require guessing implementation facts
```

Done criteria:

- The placement assessment matrix exists.
- The matrix lists architecture entries, source evidence, target arc42 section,
  placement rationale and unresolved gaps.
- The optional full assessment source is stored only when complete source text
  is available; otherwise the matrix records it as unavailable.
- No arc42 chapter receives the complete assessment text.

### Slice 02 - Add Primary Risk And Technical Debt Findings

Purpose:

- Extract maturity, risk and debt findings into arc42 chapter 11 as the main
  architecture-risk location.

Prerequisites:

- Slice 01 completed and placement matrix exists.

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
  - a risk claim cannot be traced to the placement matrix, arc42, ADR, architecture source-map docs or current repository files
  - current shared domain/application modules are claimed without source-tree verification
  - target service readiness is claimed without verified readiness evidence
  - monolithic module debt is described as active implementation when verified files classify it as predecessor or historical evidence
```

Required extraction topics:

- current architecture maturity where verified by repository evidence or
  explicitly marked as assessment classification;
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

- Slice 01 completed and placement matrix exists.

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
  - SCA meaning is required for an authoritative statement but remains ambiguous
  - current modules are treated as completed production microservices without readiness evidence
  - worker model, gRPC ingestion or service decomposition statements cannot be verified
  - a new service boundary is introduced without ADR or workflow scope
```

Required extraction topics:

- microservice target ecosystem;
- gRPC ingestion as target communication/ingestion strategy;
- worker model and migration strategy, limited to verified or explicitly
  planned behavior;
- SCA-related placement only after acronym verification, otherwise unresolved
  in the matrix;
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

- Slice 01 completed and placement matrix exists.
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
- SCA as a crosscutting migration concept only after acronym verification;
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
  - placement matrix and arc42 extracts collapse into duplicated chapter content
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
- Slice 01 is the dependency gate for all arc42 extraction slices.

## Role Ownership Map

| Area | Owner | Reviewers |
|---|---|---|
| Workflow structure | Senior Workflow Architect | Senior Requirement Engineer, Senior Tester |
| Placement assessment matrix | Senior Documentation Engineer | Senior Requirement Engineer, Senior System Architect |
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

- Keep placement and optional assessment-source material under
  `docs/architecture/assessments/`.
- Keep authoritative arc42 extracts under `docs/arc42/**`.
- Keep numbered ADR references under
  `docs/arc42/09-architecture-decisions/adr/`.
- Keep EPIC source references under `docs/epics/**`.
- Keep process-control workflow files under `docs/workflow/**`.
- Keep official arc42 rule references in the placement matrix so future
  readers can see why each entry belongs in a chapter.

## Stop Conditions

Stop workflow execution if:

- the workflow would create `docs/architecture/arc42/**`;
- an arc42 extract would require guessing implementation, service readiness,
  persistence schema, contract shape, endpoint, table, graph label, runtime
  behavior or evidence fact;
- the `SCA` acronym is required for an authoritative statement but cannot be
  verified;
- assessment claims conflict with ADR-0022, ADR-0023, ADR-0024 or ADR-0025 and
  the intended source of truth is unclear;
- a slice introduces a new architecture decision without ADR review;
- a slice modifies product source, tests, build logic, contracts, runtime or
  deployment files outside the workflow scope.

## Uncertainty Escalation Rules

- Missing full assessment text is recorded in the placement matrix and does
  not block classification.
- Ambiguous `SCA` meaning is recorded as unresolved and stops only the
  authoritative statement that would need its expansion.
- ADR conflict routes to ADR Steward and Senior System Architect.
- Service-boundary ambiguity routes to Senior System Architect and
  Microservice Senior Expert.
- Persistence-ownership ambiguity routes to Data Ownership and Persistence
  Steward.
- Contract ambiguity routes to Contract Governance and gRPC/Protobuf review.
- Quality-gate ambiguity routes to Senior Tester and quality-gate governance.

## Commit And Push Plan

No commit or push is authorized by this workflow revision request.

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
- the workflow clearly distinguishes placement classification, optional full
  assessment source storage and authoritative arc42 extraction.

## Handoff To Workflow Execute

Run the next phase with:

```text
workflow execute
```

Before editing arc42 chapters, workflow execution must read this complete
workflow, create the placement assessment matrix in Slice 01 and execute the
slices in dependency order.

## arc42 Check Status

Checked during workflow revision:

- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`

Required during workflow execution:

- create the placement matrix first;
- update chapters 4, 5, 8, 9 and 11 according to the placement matrix;
- keep chapter 11 as the primary risk and technical-debt chapter;
- keep placement and optional assessment source documents outside arc42;
- do not create `docs/architecture/arc42/**`.
