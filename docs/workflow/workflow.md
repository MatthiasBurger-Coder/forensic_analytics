# Skill And Agent Integrity Correction Workflow

## Status

Planned active workflow. This document converts the user-supplied Skill and
Agent Integrity Correction draft into the active repository workflow under
`docs/workflow`.

This workflow governs repository agent, skill, prompt, workflow and governance
documentation. It does not change production Java code, runtime behavior,
contracts, persistence schemas, graph semantics, replay output, LLM integration
or deployment descriptors.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Active branch: `feature/workflow-skill-agent-integrity-correction-20260516`
- Branch type: `feature`, selected because the workflow creates a new
  governance workflow rather than a bug fix, documentation-only update or pure
  architecture decision.
- Branch collision checks: no local branch, no remote-tracking branch and no
  remote head with the requested branch name were found before creation.
- Authoritative agent rules: `AGENTS.md`
- Authoritative quality rules: `QUALITY.md`
- Workflow authoring rules: `.agents/skills/workflow-authoring/SKILL.md`
- Workflow execution rules: `.agents/skills/workflow-executor/SKILL.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Workflow create prompt: `.agents/prompts/workflow-create.md`
- Existing active workflow replaced: Microservice Skill Sharpening workflow
  under `docs/workflow/**`.
- New active workflow location: `docs/workflow/**`.

## Requirement Source And Gate Decision

The user supplied a `workflow create` draft for Skill and Agent Integrity
Correction on 2026-05-16. Repository documentation must be English, so this
workflow records the requirement in English while preserving the requested
governance intent.

Three Amigos decision:

```text
READY_FOR_WORKFLOW
```

Gate findings:

- Business goal: make agent and skill governance coherent, auditable and safe
  for future workflow creation and execution.
- Technical goal: harden branch-first workflow creation, Three Amigos intake,
  architecture authority, orchestration boundaries, microservice invariants,
  handoffs, quality gates and traceability.
- Scope: workflow documents, root governance, `.agents/**`, prompts,
  governance docs, architecture docs, ADR references, workplan docs,
  `QUALITY.md` and README references where verified.
- Non-scope: production code implementation, service extraction, runtime
  behavior changes, API endpoint changes, storage changes, deployment manifests
  and evidence model changes.
- EPIC traceability: no EPIC was named by the user. This is recorded as a
  non-blocking traceability gap because the request targets repository
  governance, not runtime platform behavior.
- Architecture impact: governance-only workflow creation now. Later execution
  may update root governance, role hierarchy, ADR references and arc42
  governance sections.
- Quality impact: workflow creation requires documentation diff review and
  `git diff --check`. Later execution slices must use the `QUALITY.md` gates
  appropriate to their write scopes.

## Target Outcome

After `workflow execute` completes this workflow:

- `workflow create` is branch-first and cannot modify workflow artifacts before
  a dedicated branch exists and is active.
- Branch type selection happens before branch creation.
- Three Amigos review is mandatory before workflow authoring continues.
- The Senior System Architect is the highest technical architecture authority
  over backend, frontend, DevOps, testing, documentation and microservice
  governance.
- The Workflow or Workplan Executor orchestrates slices but does not decide
  architecture.
- The Agent Swarm Orchestrator coordinates subagents but does not override
  specialist decisions.
- Microservice rules are documented as hard invariants.
- Skill and agent overlaps are inventoried, resolved or explicitly marked for
  manual review.
- Every slice has owner, reviewers, quality gate, stop rule and handoff rule.
- The skill landscape is checked against V-Model, Scrum, DevOps/DORA,
  DDD/Hexagonal Architecture and Team Topologies.

## Non-Goals

- Do not implement production Java behavior.
- Do not create microservice directories, endpoints, deployment descriptors,
  persistence migrations or contract files unless a later governance slice
  proves the file is documentation-only and explicitly in scope.
- Do not move packages or modules.
- Do not create shared Java implementation, domain, DTO, event or test-fixture
  modules.
- Do not weaken existing root `AGENTS.md`, `QUALITY.md`, ADR or arc42 rules.
- Do not embed project-specific governance into portable `.codex` assets unless
  a dedicated portability review approves that target.
- Do not commit or push during workflow creation. Commit and push are reserved
  for the final execution slice after required gates pass and the workflow or
  user explicitly authorizes them.

## Governance Hierarchy

The intended decision order is:

```text
Three Amigos Requirement Gatekeeper
  -> Senior System Architect
  -> Microservice Senior Expert
  -> Workflow / Workplan Executor
  -> Agent Swarm Orchestrator
  -> Specialist subagents or role reviews
```

Authority boundaries:

- Three Amigos validates readiness, risks, acceptance criteria and testability.
- Senior System Architect owns final architecture decisions and may block
  architecture-sensitive work.
- Microservice Senior Expert owns service-autonomy and no-shared-code
  invariant review.
- Workflow / Workplan Executor owns slice planning, dependency order, progress
  checks and stop-rule enforcement.
- Agent Swarm Orchestrator owns coordination, parallelization, conflict
  detection and handoff monitoring.
- Specialist subagents or role reviews implement or review only within the
  approved slice scope.

## Branch-First Rule For Workflow Creation

Every future `workflow create` must follow this order:

```text
1. Determine branch type.
2. Check local and remote branch conflicts.
3. Create the dedicated workflow branch.
4. Checkout and verify the branch.
5. Create or modify workflow artifacts only after branch verification.
```

Default workflow branch:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
```

Allowed exceptions when the scope clearly fits:

```text
fix/
docs/
architecture/
```

Do not use these prefixes for `workflow create` unless repository governance is
explicitly changed to allow them:

```text
feat/
refactor/
test/
build/
ci/
quality/
agent/
chore/
```

## Microservice Invariants

Future microservice work must preserve these hard rules:

- Each microservice is independently runnable.
- Each microservice has its own Docker container when containerization is in
  scope.
- Each microservice can run in Docker Swarm or Kubernetes only when repository
  tooling or manifests are verified.
- No microservice shares domain code with another service.
- No shared `forensic-common` module.
- No shared entity, DTO, event or test-fixture implementation classes.
- Communication is only through REST/OpenAPI, gRPC/protobuf or defined event
  contracts.
- Contracts are versioned and protected by contract tests.

Allowed coupling mechanisms:

- OpenAPI specifications.
- gRPC or protobuf contracts.
- Event schema documentation.
- Contract-test definitions.
- Documented API versions.

## Verified Path Conventions

The user draft names some paths that do not match the verified repository
layout. Execution must not silently create alternate paths when a verified
project convention exists.

- Skills use `.agents/skills/<skill-name>/SKILL.md`, not flat
  `.agents/skills/<skill-name>.md` files.
- Project prompts currently use `.agents/prompts/*.md`.
- `.codex/prompts/**` is not present, and `.codex/AGENTS.md` says `.codex`
  should remain portable.
- Root `README.md` is not present; `docs/README.md` is present.

Execution slices may create missing files only after verifying that the target
does not conflict with these conventions.

## Quality Gate Expectations

`QUALITY.md` is authoritative.

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Documentation-only governance slices must at least run:

```bash
git diff --check
```

and inspect the slice-specific diff. Do not claim Markdown linting, link
checking, Sonar, Docker, Kubernetes or plugin validation unless the exact tool
or command was verified and executed.

## Traceability Chain

Every execution slice must preserve this chain:

```text
Requirement
-> Three Amigos decision
-> Architecture decision or ADR reference
-> Workflow slice
-> responsible skill, role or subagent
-> verification evidence
-> quality-gate evidence
-> commit or push evidence when authorized
```

## Callable Subagent Policy

The repository supports callable subagents, but workflow-authoring and
orchestrator rules say to use callable subagents only when the user explicitly
asks for delegated or parallel agent work. This workflow creation used local
role-review checklists instead of spawning callable subagents.

During `workflow execute`, use callable subagents only when the active user
request or workflow execution context explicitly authorizes delegated execution.
Otherwise, apply the matching role and skill files as review checklists and
report that limitation.

## Slice Dependency Order

```text
00 -> 01 -> 02 -> 03 -> 04 -> 05 -> 06 -> 07 -> 08 -> 09 -> 10 -> 11 -> 12 -> 13 -> 14
```

Write-capable work is sequential by default because the slices share
governance, role, skill, prompt and documentation files. Read-only specialist
reviews may run in parallel. Write-capable parallel work requires disjoint write
scopes, stable terminology, explicit handoff records and branch verification.

## Slice Template

Each execution slice must record:

- purpose
- prerequisites
- affected files
- owner role
- review roles
- allowed write scope
- dependencies
- done criteria
- verification commands
- stop conditions
- handoff rule

## Workflow Slices

### Slice 00 - Repository Preparation And Branch Verification

Purpose: reverify repository state, active workflow branch, local branch ref,
root rules, workflow rules, skill layout and quality authority.

Owner role: Senior Workflow Architect.

Review roles: Senior DevOps Engineer, Senior Git Workspace Specialist when
branch state is unclear.

Allowed write scope: `docs/workflow/execution-summary.md`.

Dependencies: none.

Done criteria:

- Repository root, active branch, local branch ref and working tree state are
  documented.
- No implementation work starts on `main`, `master`, `develop` or another
  shared branch.

Verification commands:

```bash
git rev-parse --show-toplevel
git branch --show-current
git show-ref --verify --quiet refs/heads/feature/workflow-skill-agent-integrity-correction-20260516
git status --short --branch
```

Stop conditions: branch is missing, inactive or not locally verifiable;
unrelated or unclear uncommitted changes exist.

Handoff rule: hand off to Slice 01 only after branch verification is recorded.

### Slice 01 - Inventory Existing Skill And Agent Files

Purpose: inventory existing agent, skill, prompt, workflow and governance files;
mark missing, duplicate or conflicting responsibilities.

Owner role: Senior Documentation Engineer.

Review roles: Senior Swarm Orchestrator, Skill Registry and Conflict Auditor.

Allowed write scope: `docs/governance/skill-agent-inventory.md` and
`docs/workflow/execution-summary.md`.

Dependencies: Slice 00.

Done criteria:

- Relevant governance files and directories are listed.
- Skills and agents are assigned to categories.
- Unclear roles are marked rather than guessed.

Verification commands:

```bash
rg --files AGENTS.md QUALITY.md .agents .codex docs/governance docs/architecture docs/adr docs/workplan docs/workflow
git diff --check
```

Stop conditions: a required governance path cannot be verified and no safe
repository convention exists.

Handoff rule: inventory findings feed Slices 02, 08 and 13.

### Slice 02 - Define Decision And Escalation Chain

Purpose: prevent multiple roles from claiming the same final decision authority.

Owner role: Senior System Architect.

Review roles: Workflow / Workplan Executor, Three Amigos Gatekeeper, Senior
Documentation Engineer.

Allowed write scope: `AGENTS.md` and
`docs/governance/agent-decision-chain.md`.

Dependencies: Slice 01.

Done criteria:

- Each role defines what it may decide, review, implement, stop, escalate and
  must not override.
- Senior System Architect is clearly above backend, frontend, DevOps, testing,
  documentation and microservice governance for architecture decisions.
- No two roles claim contradictory final authority.

Verification commands:

```bash
git diff -- AGENTS.md docs/governance/agent-decision-chain.md
git diff --check
```

Stop conditions: role hierarchy conflicts cannot be resolved from existing
root rules, ADRs or role files.

Handoff rule: decision chain becomes input for Slices 05, 06, 12 and 13.

### Slice 03 - Formalize Three Amigos Gate

Purpose: make Three Amigos review mandatory before workflow authoring continues.

Owner role: Three Amigos Requirement Gatekeeper.

Review roles: Senior Tester, Senior System Architect, Senior Requirement
Engineer.

Allowed write scope:

- `.agents/skills/three-amigos-requirement-gatekeeper/**`
- `docs/governance/three-amigos-gate.md`
- `.agents/prompts/workflow-create.md`

Dependencies: Slice 02.

Done criteria:

- Checklist includes business goal, non-goals, architecture impact, affected
  services, affected contracts, test strategy, risks, definition of done and
  stop conditions.
- Gate states that Three Amigos does not implement production code and does not
  replace architecture decisions.

Verification commands:

```bash
git diff -- .agents/skills/three-amigos-requirement-gatekeeper docs/governance/three-amigos-gate.md .agents/prompts/workflow-create.md
git diff --check
```

Stop conditions: gate language conflicts with ADR-0011 or root `AGENTS.md`.

Handoff rule: gate output becomes mandatory input for future workflow creation
and Slice 10 traceability.

### Slice 04 - Codify Workflow Create Branching

Purpose: prevent workflow artifacts from being created on shared or wrong
branches.

Owner role: Senior DevOps Engineer.

Review roles: Senior Workflow Architect, Senior Git Workspace Specialist.

Allowed write scope:

- `.agents/skills/git-branch-strategy/SKILL.md`
- `.agents/prompts/workflow-create.md`
- `docs/governance/workflow-branching.md`
- ADR index or architecture-decision references only when verified

Dependencies: Slice 03.

Done criteria:

- `workflow create` starts with branch type selection.
- Branch is created and verified before workflow artifacts are created.
- Allowed and forbidden prefixes are documented.
- Local and remote branch collision checks are documented.

Verification commands:

```bash
git diff -- .agents/skills/git-branch-strategy/SKILL.md .agents/prompts/workflow-create.md docs/governance/workflow-branching.md
git diff --check
```

Stop conditions: branch rules conflict with ADR-0016, root `AGENTS.md` or
`.agents/prompts/workflow-create.md`.

Handoff rule: branching rules feed Slices 05, 06, 11 and 14.

### Slice 05 - Correct Workflow / Workplan Executor Boundaries

Purpose: ensure the executor orchestrates and enforces slices but does not own
architecture decisions.

Owner role: Senior Workflow Architect.

Review roles: Senior System Architect, Senior Tester.

Allowed write scope:

- `.agents/skills/workflow-executor/SKILL.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/prompts/workflow-execute.md`
- `.agents/prompts/slice-execute.md`
- `docs/governance/workplan-slice-template.md`

Dependencies: Slices 02 and 04.

Done criteria:

- Executor may plan, prioritize, assign subagents, enforce stop rules and
  verify progress.
- Executor may not override architecture, quality gates, branch rules or
  specialist stop decisions.
- Slice template includes goal, lead agent, support agents, input, output,
  acceptance criteria, quality gate, stop rule and handoff rule.

Verification commands:

```bash
git diff -- .agents/skills/workflow-executor/SKILL.md .agents/skills/workflow-authoring/SKILL.md .agents/prompts/workflow-execute.md .agents/prompts/slice-execute.md docs/governance/workplan-slice-template.md
git diff --check
```

Stop conditions: executor role would become the final architecture authority.

Handoff rule: corrected executor boundaries feed all remaining slices.

### Slice 06 - Bound Agent Swarm Orchestrator Authority

Purpose: define the orchestrator as coordinator, not a final technical decision
role.

Owner role: Senior Swarm Orchestrator.

Review roles: Senior System Architect, Agent Handoff Protocol.

Allowed write scope:

- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/roles/senior-swarm-orchestrator.md`
- `.agents/skills/agent-swarm-coordination-specialist/SKILL.md`
- `docs/governance/subagent-orchestration.md`

Dependencies: Slice 05.

Done criteria:

- Orchestrator may coordinate, parallelize, monitor handoffs, detect conflicts
  and aggregate status.
- Orchestrator may not replace architecture decisions, skip Three Amigos,
  bypass quality gates or weaken microservice rules.
- Escalation paths are documented.

Verification commands:

```bash
git diff -- .agents/orchestrator/swarm-orchestrator.md .agents/roles/senior-swarm-orchestrator.md .agents/skills/agent-swarm-coordination-specialist/SKILL.md docs/governance/subagent-orchestration.md
git diff --check
```

Stop conditions: orchestrator authority conflicts with Senior System Architect
or Three Amigos authority.

Handoff rule: orchestrator limits feed Slice 13 consistency audit.

### Slice 07 - Harden Microservice Expert And Invariants

Purpose: anchor microservice autonomy and no-shared-code rules as hard
governance.

Owner role: Microservice Senior Expert.

Review roles: Senior System Architect, Senior DevOps Engineer, Senior
gRPC/Proto Specialist, Contract Governance Expert.

Allowed write scope:

- `.agents/skills/microservice-senior-expert/SKILL.md`
- `.agents/roles/microservice-senior-expert.md`
- `docs/governance/microservice-invariants.md`
- Relevant ADR or arc42 references when verified

Dependencies: Slices 02 and 06.

Done criteria:

- No shared code, domain, DTO, event, entity, fixture, repository, service,
  utility or internal error-model implementation modules between services.
- Each service must be independently buildable, startable, testable,
  configurable, observable, health-checkable, containerized and deployable
  before it is called a microservice.
- REST/OpenAPI, gRPC/protobuf and event contracts are the only allowed service
  integration mechanisms.
- Violation handling is documented.

Verification commands:

```bash
git diff -- .agents/skills/microservice-senior-expert/SKILL.md .agents/roles/microservice-senior-expert.md docs/governance/microservice-invariants.md
git diff --check
```

Stop conditions: a rule would describe the current modular monolith as already
implementing microservices; Docker Swarm or Kubernetes commands would be
invented without repository tooling.

Handoff rule: microservice invariants feed Slices 08, 09, 12 and 13.

### Slice 08 - Audit Missing Governance Skills

Purpose: add or sharpen missing governance skills without duplicating existing
responsibilities.

Owner role: Senior System Architect.

Review roles: Skill Registry and Conflict Auditor, Senior Swarm Orchestrator,
Senior Documentation Engineer.

Allowed write scope:

- Existing verified skill directories under `.agents/skills/**`
- New skill directories only when no verified equivalent exists
- `docs/governance/skill-agent-inventory.md`

Dependencies: Slices 01 and 07.

Done criteria:

- Requested responsibilities are mapped to existing skills or newly created
  skill directories.
- Each skill has responsibility, boundaries, inputs, outputs and stop rules.
- No skill silently overlaps Senior System Architect authority.

Verification commands:

```bash
rg --files .agents/skills
git diff -- .agents/skills docs/governance/skill-agent-inventory.md
git diff --check
```

Stop conditions: existing skill responsibilities overlap and cannot be resolved
without a governance decision; a flat `.agents/skills/*.md` target would be
created despite the verified `.agents/skills/<name>/SKILL.md` convention.

Handoff rule: skill audit results feed Slice 13 consistency audit.

### Slice 09 - Document Development Model Alignment

Purpose: validate the agent workflow against established delivery and
architecture models.

Owner role: Senior Documentation Engineer.

Review roles: Senior System Architect, Senior Tester, Senior DevOps Engineer.

Allowed write scope: `docs/governance/development-model-alignment.md` and
minimal verified references in `AGENTS.md`.

Dependencies: Slices 02 through 08.

Done criteria:

- V-Model, Scrum, DevOps/DORA, Team Topologies and DDD/Hexagonal Architecture
  are mapped to the agent structure.
- Deviations are documented.
- Required corrections are fed back to `AGENTS.md` or skills.

Verification commands:

```bash
git diff -- docs/governance/development-model-alignment.md AGENTS.md
git diff --check
```

Stop conditions: a model mapping would falsely describe planned behavior as
implemented.

Handoff rule: alignment results feed Slice 12 consolidation and Slice 13 audit.

### Slice 10 - Introduce Workflow Traceability Matrix

Purpose: make workflow decisions traceable from requirement to verification
evidence.

Owner role: Senior Tester.

Review roles: Senior Workflow Architect, Senior Documentation Engineer.

Allowed write scope:

- `docs/governance/workflow-traceability-matrix.md`
- `.agents/prompts/workflow-create.md`

Dependencies: Slices 03 and 05.

Done criteria:

- Traceability chain covers requirement, Three Amigos, architecture or ADR,
  workflow slice, responsible skill or subagent, tests, quality gate and commit
  or push evidence.
- Traceability is required for new workflows.

Verification commands:

```bash
git diff -- docs/governance/workflow-traceability-matrix.md .agents/prompts/workflow-create.md
git diff --check
```

Stop conditions: traceability would require unverifiable evidence or fabricated
quality results.

Handoff rule: traceability matrix becomes required evidence for Slice 14.

### Slice 11 - Add Governance Quality Gate

Purpose: ensure agent and skill governance changes have explicit verification.

Owner role: Senior Tester.

Review roles: Senior DevOps Engineer, Security / Supply Chain Expert.

Allowed write scope:

- `QUALITY.md`
- `docs/governance/governance-quality-gate.md`

Dependencies: Slices 03, 04 and 10.

Done criteria:

- Governance-specific checks are documented.
- Automated checks and manual checks are separated.
- Manual checks cover role conflict review, branch-rule review,
  Three-Amigos-gate review and microservice-invariant review.
- Unverified tooling such as Markdown lint or link checking is not claimed as
  available.

Verification commands:

```bash
git diff -- QUALITY.md docs/governance/governance-quality-gate.md
git diff --check
```

Stop conditions: quality commands cannot be verified from `QUALITY.md` or build
files; the slice would weaken strict dependency verification or existing quality
gates.

Handoff rule: governance quality gate feeds Slice 14 final verification.

### Slice 12 - Consolidate Root AGENTS.md

Purpose: make root `AGENTS.md` the central truth for corrected agent
governance.

Owner role: Senior System Architect.

Review roles: Senior Documentation Engineer, Senior Workflow Architect,
Microservice Senior Expert.

Allowed write scope: `AGENTS.md`.

Dependencies: Slices 02 through 11.

Done criteria:

- Root governance includes the hierarchy, decision order, branch-first rule,
  Three Amigos obligation, microservice invariant references, stop rules and
  links to detail skills.
- Detail skills do not contradict root `AGENTS.md`.

Verification commands:

```bash
git diff -- AGENTS.md
git diff --check
```

Stop conditions: root rules would conflict with `QUALITY.md`, accepted ADRs,
verified role files or workflow prompts.

Handoff rule: consolidated root governance is the source for Slice 13 audit.

### Slice 13 - Run Cross-Skill Consistency Audit

Purpose: verify that no logical conflicts remain after governance changes.

Owner role: Senior Tester.

Review roles: Senior System Architect, Senior Workflow Architect, Skill
Registry and Conflict Auditor.

Allowed write scope:

- `docs/governance/skill-agent-integrity-audit.md`
- Minimal follow-up fixes in files changed by prior slices only when the owning
  role approves the handoff

Dependencies: Slices 01 through 12.

Done criteria:

- Every role is clearly ordered.
- No duplicate final decision authority remains undocumented.
- `workflow create` cannot proceed without branch verification and Three
  Amigos readiness.
- Subagents cannot override architecture decisions.
- Microservices cannot use shared Java implementation modules.
- Quality gates, stop rules and handoffs are documented.
- Remaining uncertainty is explicitly marked.

Verification commands:

```bash
git diff -- docs/governance/skill-agent-integrity-audit.md AGENTS.md .agents docs/governance QUALITY.md
git diff --check
```

Stop conditions: a conflict remains that would make workflow execution unsafe.

Handoff rule: audit result is required input for Slice 14.

### Slice 14 - Final Verification, Commit And Push

Purpose: finish the workflow with verified evidence and optional branch
publication.

Owner role: Senior Workflow Architect.

Review roles: Senior DevOps Engineer, Senior Tester, git commit preparation
skills.

Allowed write scope: final execution notes and commit metadata only when
authorized.

Dependencies: Slice 13.

Done criteria:

- Git diff and file list reviewed.
- Required quality gates executed or blockers documented.
- Commit message includes what changed, why, how, affected files, quality
  checks, governance decisions and breaking-change status.
- Push occurs only when explicitly authorized and all required checks pass or a
  documented blocker is accepted by the workflow.

Verification commands:

```bash
git status --short --branch
git diff --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Stop conditions: quality gate fails and cannot be safely corrected; broad
line-ending-only changes appear; commit or push is not explicitly authorized;
open governance conflicts remain undocumented.

Handoff rule: final report must list executed commands, skipped commands,
changed files, residual risks and commit or push status.
