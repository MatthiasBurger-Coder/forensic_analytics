# Microservice Skill Sharpening Workflow

## Status

Planned active workflow. This document converts the user-supplied
Microservice Skill Sharpening draft into the active repository workflow under
`docs/workflow`.

This workflow prepares governance, skills, roles, prompts and quality rules for
later microservice migration work. It does not extract production services,
move modules, create service directories, implement REST or gRPC endpoints, add
deployment descriptors, split persistence models or introduce shared Java code.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Active branch: `feature/workflow-microservice-skill-sharpening-20260516`
- Branch collision checks: no local or remote branch existed before creation.
- Authoritative agent rules: `AGENTS.md`
- Authoritative quality rules: `QUALITY.md`
- Workflow authoring rules: `.agents/skills/workflow-authoring/SKILL.md`
- Workflow execution rules: `.agents/skills/workflow-executor/SKILL.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Existing active workflow replaced: Git Branch Strategy workflow under
  `docs/workflow/**`
- New active workflow location: `docs/workflow/**`
- Root `README.md`: not present. Future README links must target an existing
  repository README such as `docs/README.md`, or explicitly create a root README
  as a documented slice.

## Requirement Source And Gate Decision

The user supplied a `workflow create` draft titled "Skill-Scharfung fur
Microservice-Migration" on 2026-05-16. The draft requests skill and governance
preparation for controlled future migration from the current modular monolith to
real microservices.

Three Amigos decision:

```text
READY_FOR_WORKFLOW
```

Gate findings:

- Business goal: prepare the agent, skill, prompt and governance landscape so
  future microservice migration can be executed in small, reviewed slices.
- Technical goal: define and later implement repository governance for bounded
  context decomposition, contract-first communication, no shared Java code
  modules, runtime independence, Three Amigos intake and quality gates.
- Scope: workflow, skill, role, prompt, architecture documentation, governance
  documentation, quality documentation and workplan alignment.
- Non-scope: production service extraction, module moves, endpoint
  implementation, persistence splitting, deployment descriptor creation and
  Docker/Kubernetes/Swarm finalization.
- EPIC traceability: no EPIC was named by the user. This is recorded as a
  non-blocking traceability gap because the request targets repository
  governance rather than runtime behavior.
- Architecture impact: governance-only workflow creation now. Later execution
  may update architecture governance and ADR/arc42 references.
- Quality impact: workflow creation requires documentation diff review and
  `git diff --check`. Later execution slices must use `QUALITY.md` gates.

## Target Outcome

After `workflow execute` completes this workflow:

- New or updated skills can evaluate service decomposition, contract
  governance, migration safety and runtime readiness.
- Existing workflow, architecture, Three Amigos and execution skills route
  microservice migration through explicit gates.
- Root governance states that microservices cannot share Java implementation,
  domain, DTO, repository, service or utility modules.
- Every future cross-service call must be contract-first through REST/OpenAPI,
  gRPC/protobuf or approved eventing.
- Runtime independence is mandatory: each service must be buildable, runnable,
  testable, configurable, observable, health-checkable and containerizable on
  its own before it is called a microservice.
- Later code changes use small migration slices with named owner agents,
  acceptance criteria, affected files, expected tests, rollback notes and
  quality-gate commands.

## Non-Goals

- Do not migrate existing modules into microservices.
- Do not create or finalize `services/**`, `contracts/**`, Docker Swarm or
  Kubernetes material as part of this workflow.
- Do not move production Java packages.
- Do not implement REST, gRPC or messaging endpoints.
- Do not split database models or persistence ownership.
- Do not introduce shared Java libraries such as `forensic-common`,
  `shared-domain`, `shared-dto`, `shared-service` or shared internal error
  model libraries.
- Do not treat the current Gradle modules as independently deployable
  microservices.
- Do not embed project-specific governance into portable `.codex` assets unless
  a slice explicitly reviews portability.

## Architecture And Governance Constraints

- Root `AGENTS.md` and `QUALITY.md` remain authoritative.
- `.codex/AGENTS.md` and `.codex/skills/**` are portable assets. Project
  governance belongs in root docs, `.agents/**`, ADRs, arc42 or workflow docs.
- ADR-0011 requires the Three Amigos gate before workflow authoring or
  execution.
- ADR-0015 requires skill registry and conflict auditing for new skills and
  governance changes that affect skill ownership.
- ADR-0006 currently allows Spring Boot only in `forensic-analytics-boot-app`.
  Any later service scaffold requiring its own Spring Boot application must
  include an ADR and architecture-test slice first.
- The current Gradle layout is a modular monolith. Shared domain, application,
  logging and observability modules are valid today, but they must not become
  shared Java libraries between independently deployable services.
- Kubernetes and Docker Swarm tooling is not verified. Future runtime-readiness
  slices may require it as a target, but must not invent commands or manifests.

## Service Target Picture For Later Work

The following service names are planning candidates only:

```text
forensic-analytics-server
repository-analysis-service
java-ast-scanner-service
joern-cpg-scanner-service
btm-generation-service
analysis-store-service
graph-replay-report-service
frontend-app
```

Execution of this workflow must not create these services. Later workflows must
verify bounded context ownership before any directory, module, build, container
or contract file is created.

## Microservice Migration Slice Format

Every later production code migration must use this format:

```text
Microservice Migration Slice

1. Branch
   - branch name:
   - branch type:

2. Three Amigos Decision
   - scope:
   - non-scope:
   - acceptance criteria:
   - risk level:
   - stop conditions:

3. Service Boundary
   - target service:
   - owned responsibility:
   - owned data:
   - inbound communication:
   - outbound communication:

4. Contract
   - protocol:
   - contract file:
   - version:
   - request model:
   - response model:
   - error model:

5. Implementation Slice
   - affected files:
   - agent owner:
   - expected change:
   - forbidden changes:

6. Tests
   - unit tests:
   - contract tests:
   - integration tests:
   - runtime start test:

7. Quality Gate
   - commands:
   - expected result:

8. Rollback
   - rollback strategy:
   - files to revert:

9. Result
   - implemented:
   - not implemented:
   - risks:
   - next slice:
```

## Slice Dependency Order

```text
00 -> 01 -> 02 -> 03 -> 04 -> 05 -> 06 -> 07 -> 08 -> 09 -> 10 -> 11 -> 12 -> 13
```

All implementation slices are sequential by default because they share
governance, role and skill files. Read-only specialist reviews may run in
parallel. Write-capable parallel work requires disjoint write scopes, stable
contracts and explicit handoff records.

## Workflow Slices

### Slice 00 - Repository And Rule Verification

Purpose:

- Reverify repository state before execution.
- Confirm branch, active workflow, rules, skill layout and quality authority.
- Record contradictions before any governance file is changed.

Prerequisites:

- Active branch is `feature/workflow-microservice-skill-sharpening-20260516`.
- Working tree changes are understood and belong to this workflow.

Affected files:

- `docs/workflow/execution-summary.md`
- `docs/workflow/conflict-review.md`

Owner role:

- Senior Workflow Architect

Review roles:

- Senior System Architect
- Senior Documentation Engineer
- Senior Tester

Allowed write scope:

- Workflow execution notes only.

Done criteria:

- Repository root and active branch are verified.
- Root `AGENTS.md`, `QUALITY.md`, `.codex/AGENTS.md`,
  `.codex/workflow/workflow-execution-rules.md`, `.agents/orchestrator/**`,
  relevant `.agents/skills/**` and relevant `.agents/roles/**` are read.
- The skill path convention is recorded.
- No production source code is changed.

Verification commands:

```bash
git status --short --branch
git diff -- docs/workflow
git diff --check
```

Stop conditions:

- Active branch is not the workflow branch.
- Unrelated or unclear uncommitted changes exist.
- The skill or role structure cannot be verified.
- Workflow execution would require guessing repository rules.

### Slice 01 - Three Amigos And Skill Registry Gate

Purpose:

- Run and record Three Amigos readiness for the execution workflow.
- Run a Skill Registry and Conflict Auditor review before new skills are added.

Prerequisites:

- Slice 00 complete.

Affected files:

- `docs/workflow/three-amigos-decision-record.md`
- `docs/workflow/conflict-review.md`
- `docs/skill-audit/**`

Owner role:

- Senior Requirement Engineer

Review roles:

- Senior System Architect
- Senior Tester
- Skill Registry and Conflict Auditor
- Senior Documentation Engineer

Allowed write scope:

- Workflow decision record and skill-audit documentation.

Done criteria:

- Decision is `READY_FOR_WORKFLOW` or execution stops.
- New skill ownership, overlap and authority are documented.
- Blocking conflicts are resolved before later slices continue.

Verification commands:

```bash
git diff -- docs/workflow docs/skill-audit
git diff --check
```

Stop conditions:

- Three Amigos is skipped.
- Skill ownership is unresolved.
- A new skill would duplicate existing authority without a documented boundary.

### Slice 02 - Service Decomposition Skill

Purpose:

- Add a skill that evaluates bounded-context service decomposition.
- Prevent treating a technical module as a microservice.

Prerequisites:

- Slice 01 complete.
- Target path convention from `skill-target-map.md` confirmed.

Affected files:

- `.agents/skills/service-decomposition-bounded-context/SKILL.md`
- `docs/skill-audit/**`

Owner role:

- Service Decomposition / Bounded Context Expert

Review roles:

- Senior System Architect
- Senior Documentation Engineer
- Microservice Senior Expert

Allowed write scope:

- New skill directory and skill-audit notes.

Done criteria:

- Skill defines mission, authority, forbidden scope, required inputs, outputs,
  collaboration rules and STOP rules.
- Skill checks business responsibility, ownership, data responsibility,
  independent deployability, communication boundaries and slice size.
- Skill states: "A technical module is not a microservice."
- Skill outputs a Service Boundary Decision Record.

Verification commands:

```bash
git diff -- .agents/skills/service-decomposition-bounded-context docs/skill-audit
git diff --check
```

Stop conditions:

- Execution cannot decide whether to use repository skill-directory convention.
- Service ownership would require guessing.

### Slice 03 - Contract Governance Skill

Purpose:

- Add a skill for REST/OpenAPI, gRPC/protobuf and event contract governance.
- Prevent hidden Java coupling across service boundaries.

Prerequisites:

- Slice 01 complete.

Affected files:

- `.agents/skills/contract-governance-expert/SKILL.md`
- `docs/skill-audit/**`

Owner role:

- Contract Governance Expert

Review roles:

- Senior Java Backend Engineer
- Senior gRPC/Proto Specialist
- Senior System Architect

Allowed write scope:

- New skill directory and skill-audit notes.

Done criteria:

- Skill defines contract-first as mandatory.
- Skill covers versioning, compatibility, DTO boundaries, error/status models,
  streaming, batch communication, idempotency, timeouts and generated code.
- Skill allows shared versioned OpenAPI/protobuf files but forbids shared Java
  implementation or business-code modules.

Verification commands:

```bash
git diff -- .agents/skills/contract-governance-expert docs/skill-audit
git diff --check
```

Stop conditions:

- A contract field, RPC method, REST route or event field cannot be verified.
- A slice would introduce shared Java DTO, domain, service or error-model code.

### Slice 04 - Microservice Migration Safety Gate Skill

Purpose:

- Add a gatekeeper skill for later production microservice migration code
  changes.
- Block big-bang migration and multi-service changes without contract-first
  slices.

Prerequisites:

- Slices 02 and 03 complete.

Affected files:

- `.agents/skills/microservice-migration-safety-gate/SKILL.md`
- `docs/skill-audit/**`

Owner role:

- Microservice Senior Expert

Review roles:

- Senior System Architect
- Senior Tester
- Senior DevOps Engineer

Allowed write scope:

- New skill directory and skill-audit notes.

Done criteria:

- Skill requires clear scope, target service, communication path, contract,
  data responsibility, tests, rollback or strangler strategy and slice size.
- Skill defines risk levels and release/rollback evidence.
- Skill stops when a change deeply modifies multiple services without a
  contract-first slice.

Verification commands:

```bash
git diff -- .agents/skills/microservice-migration-safety-gate docs/skill-audit
git diff --check
```

Stop conditions:

- A later code change would affect several services without stable contracts.
- No rollback or strangler strategy exists for behavior-changing migration.

### Slice 05 - Microservice Runtime Readiness Skill

Purpose:

- Add a skill that verifies whether a candidate service is independently
  runnable and deployable.

Prerequisites:

- Slice 01 complete.
- DevOps review confirms which deployment targets are verified and which remain
  planning constraints.

Affected files:

- `.agents/skills/microservice-runtime-readiness-expert/SKILL.md`
- `docs/skill-audit/**`

Owner role:

- Senior DevOps Engineer

Review roles:

- Microservice Senior Expert
- Senior Tester

Allowed write scope:

- New skill directory and skill-audit notes.

Done criteria:

- Skill checks own build, start command, configuration, Docker build,
  healthchecks, tests, ports, observability and no direct class coupling.
- Skill treats Docker Swarm and Kubernetes as readiness goals that must be
  verified from repository tooling before commands are documented.
- Skill states that a service is not a microservice until it can be built,
  started, tested and containerized independently.

Verification commands:

```bash
git diff -- .agents/skills/microservice-runtime-readiness-expert docs/skill-audit
git diff --check
```

Stop conditions:

- The workflow invents Kubernetes, Swarm or container commands.
- A service is presented as independently runnable without verified evidence.

### Slice 06 - Senior System Architect Authority

Purpose:

- Update architecture governance so the Senior System Architect owns service
  boundary decisions and conflict escalation for microservice migration work.

Prerequisites:

- Slices 02 through 05 complete.

Affected files:

- `.agents/roles/senior-system-architect.md`
- `.codex/agents/senior_system_architect.toml`, only if runtime agent metadata
  requires the same responsibility update
- `AGENTS.md`, only if root governance needs project-wide wording

Owner role:

- Senior System Architect

Review roles:

- Senior Workflow Architect
- Senior Documentation Engineer
- Microservice Senior Expert

Allowed write scope:

- Verified role and governance files only.

Done criteria:

- Architecture boundary validation, contract strategy validation, runtime
  independence validation and quality-gate strategy validation are required
  before any microservice extraction starts.
- Relationship to Backend, Frontend, UX, DevOps, Tester, Documentation,
  Microservice Senior Expert, Contract Governance and Service Decomposition is
  explicit.

Verification commands:

```bash
git diff -- .agents/roles/senior-system-architect.md .codex/agents/senior_system_architect.toml AGENTS.md
git diff --check
```

Stop conditions:

- The update would make the architect the sole authority for requirements,
  quality, security or release decisions.
- Root governance and role files disagree.

### Slice 07 - Workflow Authoring And Execution Rules

Purpose:

- Update workflow create and workflow execute guidance so microservice
  migration slices use branch-first, Three Amigos, service-boundary,
  contract-first and quality-gate rules.

Prerequisites:

- Slice 01 complete.

Affected files:

- `.agents/prompts/workflow-create.md`
- `.agents/prompts/workflow-execute.md`
- `.agents/prompts/slice-execute.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/workflow-executor/SKILL.md`
- `.agents/roles/senior-workflow-architect/SKILL.md`

Owner role:

- Senior Workflow Architect

Review roles:

- Senior Tester
- Senior DevOps Engineer
- Senior System Architect

Allowed write scope:

- Workflow, prompt and execution-skill guidance only.

Done criteria:

- `workflow create` records Three Amigos readiness.
- `workflow execute` requires owner agent, acceptance criteria, affected files,
  expected tests, rollback notes and quality-gate command before implementation.
- Implementation sequence is branch check, repository rules, Three Amigos,
  architecture boundary, contract-first slice, affected services, subagent
  ownership, minimal change, tests, quality gate, diff, commit preparation.

Verification commands:

```bash
git diff -- .agents/prompts .agents/skills/workflow-authoring .agents/skills/workflow-executor .agents/roles/senior-workflow-architect
git diff --check
```

Stop conditions:

- Workflow execution would allow implementation before owner, tests, rollback
  or quality gate are named.
- Quality commands are invented or downgraded.

### Slice 08 - Three Amigos Microservice Decision Record

Purpose:

- Extend Three Amigos intake for microservice slices.

Prerequisites:

- Slice 07 complete.

Affected files:

- `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/templates/**`
- `.agents/skills/three-amigos-requirement-gatekeeper/decision-rules.md`
- `.agents/prompts/workflow-create.md`

Owner role:

- Senior Requirement Engineer

Review roles:

- Senior System Architect
- Senior Tester
- Microservice Senior Expert

Allowed write scope:

- Three Amigos skill and prompt guidance.

Done criteria:

- Every future microservice slice has a Three Amigos Decision Record with
  Scope, Non-Scope, Acceptance Criteria, Service Boundary, Contract Impact,
  Test Impact, Risk Level and Stop Conditions.
- Required questions cover service problem, inputs, outputs, owned data,
  allowed dependencies, allowed communication, tests and forbidden changes.

Verification commands:

```bash
git diff -- .agents/skills/three-amigos-requirement-gatekeeper .agents/prompts/workflow-create.md
git diff --check
```

Stop conditions:

- A microservice slice lacks explicit non-scope, contract impact or test impact.
- Service ownership or communication is unclear.

### Slice 09 - Root Microservice Governance

Purpose:

- Add or refine central microservice governance where root rules require it.

Prerequisites:

- Slices 02 through 08 complete.

Affected files:

- `AGENTS.md`

Owner role:

- Senior System Architect

Review roles:

- Senior Documentation Engineer
- Microservice Senior Expert
- Contract Governance Expert

Allowed write scope:

- Root governance wording only.

Done criteria:

- Root rules define no shared Java code modules, contract-first communication,
  runtime independence and migration slice size.
- Existing root rules are not overwritten blindly.
- Current modular-monolith modules are not described as microservices.

Verification commands:

```bash
git diff -- AGENTS.md
git diff --check
```

Stop conditions:

- Root `AGENTS.md`, ADRs or implementation baseline conflict in a way that
  cannot be resolved with at least 95 percent confidence.

### Slice 10 - Architecture And Contract Governance Documentation

Purpose:

- Document microservice governance and contract governance for humans and
  agents.
- Synchronize arc42 decision references when required.

Prerequisites:

- Slice 09 complete.

Affected files:

- `docs/architecture/microservice-governance.md`, if this directory is accepted
  for the new governance page
- `docs/governance/contract-governance.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/adr/README.md`, only if ADR references require synchronization

Owner role:

- Senior Documentation Engineer

Review roles:

- Senior System Architect
- Senior DevOps Engineer
- Microservice Senior Expert

Allowed write scope:

- Architecture and governance documentation only.

Done criteria:

- Documentation states that target services are a future target picture, not an
  implemented structure.
- Allowed and forbidden coupling are clear.
- Contract-first and runtime-independence evidence are clear.
- Missing arc42 ADR references for accepted microservice-critical ADRs are
  reconciled or documented as blockers.

Verification commands:

```bash
git diff -- docs/architecture docs/governance docs/arc42 docs/adr
git diff --check
```

Stop conditions:

- The documentation would claim that services, contracts, Kubernetes or Swarm
  deployment are implemented when they are only planned.

### Slice 11 - Quality And Execution Documentation

Purpose:

- Verify and document quality gates for later microservice skill and migration
  work.

Prerequisites:

- Slice 10 complete.

Affected files:

- `QUALITY.md`
- `docs/README.md`, only if governance links need an existing README target
- `docs/workplan/**`, only for alignment notes

Owner role:

- Senior Tester

Review roles:

- Senior DevOps Engineer
- Senior Workflow Architect

Allowed write scope:

- Quality and documentation guidance only.

Done criteria:

- Quality commands remain sourced from `QUALITY.md`.
- Missing quality commands are documented instead of invented.
- `validatePlugins` is required only for Gradle plugin metadata, task
  inputs/outputs or plugin implementation changes.
- No Java, Gradle, dependency or framework versions are changed.

Verification commands:

```bash
git diff -- QUALITY.md docs/README.md docs/workplan
git diff --check
```

Stop conditions:

- Quality gate wording downgrades strict dependency verification.
- A command cannot be verified from repository docs or build files.

### Slice 12 - Consistency Review

Purpose:

- Compare all new and changed skills, prompts, roles, docs and root governance.
- Remove contradictions before final verification.

Prerequisites:

- Slices 02 through 11 complete.

Affected files:

- `docs/workflow/conflict-review.md`
- `docs/skill-audit/**`
- Any changed governance or skill file from prior slices, only for consistency
  fixes within the same approved scope

Owner role:

- Senior System Architect

Review roles:

- Senior Workflow Architect
- Senior Documentation Engineer
- Microservice Senior Expert
- Skill Registry and Conflict Auditor

Allowed write scope:

- Consistency notes and minimal corrections to files already touched by this
  workflow.

Done criteria:

- No contradictory branch rules.
- No contradictory no-common-code rules.
- No contradictory contract-first rules.
- No contradictory runtime-independence rules.
- No contradictory agent responsibilities.
- Stop rules are explicit.

Verification commands:

```bash
git diff --check
git status --short --branch
```

Stop conditions:

- Conflicts remain blocking after review.
- A correction would require changing unapproved production source files.

### Slice 13 - Final Verification And Commit Preparation

Purpose:

- Review all diffs, run required checks and prepare a commit message.

Prerequisites:

- Slice 12 complete.

Affected files:

- `docs/workflow/execution-summary.md`

Owner role:

- Senior Workflow Architect

Review roles:

- Senior Tester
- Senior System Architect
- git commit preparation skills

Allowed write scope:

- Final execution notes and commit-preparation material.

Done criteria:

- All diffs are inspected.
- `git diff --check` passes.
- Minimum or full quality gate from `QUALITY.md` is executed when required for
  commit readiness, or the workflow records why it could not be executed and
  treats the result as not fully clean.
- Commit message is prepared but not committed unless the user explicitly asks.
- Push is not performed unless explicitly requested and repository rules allow
  it.

Verification commands:

```bash
git diff --check
git status --short --branch
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Required quality gates fail.
- Broad line-ending-only changes appear.
- Commit or push would happen without explicit user authorization.

## Quality Gates

Minimum command from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate from `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Workflow-documentation creation checks:

```bash
git status --short --branch
git diff -- docs/workflow
git diff --check
```

Documentation checks do not replace the `QUALITY.md` gate for commit/push
readiness.

## Commit Plan

Do not commit during `workflow create`. If the user later requests commit
preparation after successful execution, use this draft message:

```text
feat(workflow): sharpen microservice migration skills and governance

Add workflow, skill and governance preparation for future microservice
migration slices.

What changed:
- add service decomposition and bounded-context skill planning
- add contract governance skill planning
- add microservice migration safety-gate planning
- add runtime readiness skill planning
- route system architect, workflow executor and Three Amigos updates
- document no-common-code, contract-first and runtime-independence rules

Quality:
- inspect workflow and governance diffs
- run git diff --check
- run available quality gates according to QUALITY.md where required
```

## Definition Of Done

This workflow is complete after execution when:

- a dedicated branch exists and remains active for the workflow;
- new microservice governance skills exist with verified ownership;
- existing workflow, architecture and Three Amigos skills are updated;
- root governance contains microservice rules when required;
- workflow creation records branch-first and Three Amigos readiness;
- workflow execution requires slice, contract and quality gates;
- no shared Java code module solution is allowed;
- runtime independence and contract-first rules are documented;
- skill consistency has been reviewed;
- final diffs and required checks have been inspected;
- commit preparation is documented without committing or pushing unless the user
  explicitly requests it.

Recommended next workflow after completion:

```text
workflow create microservice-target-architecture-and-first-extraction-slice
```
