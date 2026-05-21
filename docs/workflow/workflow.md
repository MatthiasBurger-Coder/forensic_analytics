# Workflow: Governance Performance Optimization

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `governance-performance-20260521-v1` |
| Workflow branch | `architecture/workflow-governance-performance-20260521` |
| Creation status | Created by `workflow create`; execution requires a clean committed workflow package. |
| Process strand | `workflow create` now; later `workflow execute` for slices. |

## Executive Summary

This workflow optimizes the repository's agent and workflow governance without
weakening the forensic safety rules. The target is to reduce repeated full
governance review for low-risk work by introducing profile classification,
quality-impact classification, workflow context packs, machine-readable slice
metadata, dedicated S3D ownership, persistent skill registry evidence,
unified branch rules, flowchart integrity auditing, workflow-executor
resolution rules and process-performance metrics.

The workflow is governance-only. It must not implement product backend,
frontend, Docker/runtime, gRPC/protobuf, persistence, analysis-engine, Joern,
JavaParser, BTM generator or analytics behavior.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Create a workflow for the listed process-performance optimizations: execution profile routing, quality-impact classification, context packs, machine-readable slice metadata, dedicated S3D orchestration, persistent skill registry, branch strategy unification, flowchart integrity audit, workflow-executor cleanup and process-performance profiling. |
| Interpreted intent | Create an executable governance workflow that later implements those optimizations as small process slices while preserving `AGENTS.md`, `QUALITY.md`, ADRs, arc42, process-strand separation and quality-gate authority. |
| Change type | Process governance, skills, roles, routing, workflow documentation and architecture-governance documentation. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Agent governance, workflow governance, quality-gate routing, branch governance, documentation governance and skill registry ownership. |
| Product runtime impact | None planned. Product code changes are forbidden by this workflow. |
| EPIC source | Product EPIC v0.2 remains unchanged; this workflow is process-governance architecture and does not change product requirements. |
| Confidence | 94 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

No blocking requirement question remains for workflow creation. Ambiguities that
could change repository governance are assigned to early decision slices before
any process rule is edited.

## Verified Baseline

Read-only verification before workflow authoring found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- WSL repository access: available from the Windows-hosted worktree.
- Workflow branch created and verified: `architecture/workflow-governance-performance-20260521`.
- Working tree before workflow regeneration: clean.
- Existing active workflow before regeneration: `docs/workflow/workflow.md` for the Microservices BTM Pipeline.
- Quality authority: `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- Root `checkPackageCoverage` task exists in `build.gradle.kts`.
- Process strands are accepted by ADR-0020.
- Branch-first workflow creation is accepted by ADR-0016.
- Skill registry conflict auditing is accepted by ADR-0015.
- Governance Flowchart V2 is accepted by ADR-0021.

Verified existing gaps that this workflow may address:

- Flowchart Integrity Audit is mapped with a documented gap and no dedicated skill.
- S3D execution orchestration currently routes through Senior Swarm Orchestrator.
- Project-specific and reusable workflow-executor skill files share the same front-matter name.
- Quality-gate orchestration exists, but a dedicated quality-impact classifier is not present.
- Skill registry auditing exists, but the persistent JSON registry proposed by the request is not present.
- Branch rules are spread across `AGENTS.md`, process docs and git governance skills.

## Target Picture

The target governance model after workflow execution is:

```text
User request
  -> execution-profile-router
  -> requirement and architecture gate with profile-aware depth
  -> workflow context pack
  -> machine-readable slice metadata
  -> dedicated S3D execution orchestrator
  -> quality-impact-classifier
  -> slice quality gates
  -> skill registry cache when safe
  -> checkpoint and branch governance
  -> process-performance metrics
```

Governance remains strict where risk is real. Low-risk documentation,
metadata and typo changes may use reduced reviews only when the classifier
proves no product build, runtime behavior, contracts, tests, architecture or
quality-rule impact.

## Scope

In scope:

- Add or update project-specific skills under `.agents/skills/**`.
- Add or update project-specific roles under `.agents/roles/**`.
- Update routing rules under `.agents/orchestrator/**`.
- Update process-governance docs under `docs/process/**`.
- Update agent-governance docs under `docs/agents/**`.
- Update governance flowchart docs under `docs/governance/**`.
- Update skill-audit docs under `docs/skill-audit/**`.
- Update workflow-authoring and workflow-execution governance docs.
- Check or update arc42 governance documentation.
- Update root `AGENTS.md` only when branch or process-strand authority cannot
  be made consistent through narrower governance docs.

## Non-Goals

Out of scope:

- Product backend, frontend, runtime, Docker, persistence or analytics behavior.
- gRPC/protobuf, REST/OpenAPI or event contract implementation changes.
- Java source changes, Gradle plugin implementation changes or dependency upgrades.
- Microservice extraction, deployment topology changes or runtime service wiring.
- Automatic PR creation, merge, branch cleanup or `push auto`.
- Rewriting historical ADR decisions without a new explicit ADR slice.

## Architecture Boundaries

- `.codex/**` remains reusable unless a change is proven portable.
- Project-specific rules belong in root `AGENTS.md`, `QUALITY.md`, `.agents/**`
  or project documentation.
- `workflow create` does not execute implementation slices.
- `workflow execute` must not jump backward to `workflow create` or rewrite
  the active workflow to resolve execution blockers.
- Governance optimizations may reduce review depth only by routing some roles
  to N/A impact checks; they must not bypass mandatory authority, STOP rules or
  required quality gates.
- `QUALITY.md` remains authoritative for quality commands.
- Failed required quality gates must never be marked optional.
- Missing or ambiguous skill, role, route, branch, quality or workflow authority
  stops the slice and escalates through the documented owner.

## Backend Assessment

Backend product modules are not in scope. Senior Java Backend review is still
required by workflow-create governance, but this workflow classifies backend
impact as N/A unless a future slice unexpectedly proposes Java source, build,
contract or product test changes. Such a proposal must stop and require a
separate workflow.

## Frontend Assessment

Frontend product modules are not in scope. Senior React Frontend review is an
N/A impact check unless a future slice unexpectedly proposes React source,
frontend API adapter or frontend test changes. Such a proposal must stop and
require a separate workflow.

## Test Strategy

The workflow is process-governance documentation and skill/role metadata work.
Default slice verification is:

- `git status --short --branch`
- `git diff --check`
- targeted Markdown, JSON, YAML or registry consistency checks when a slice
  changes those formats
- skill-registry conflict review when `.agents/**`, `.codex/**`,
  `docs/agents/**` or `docs/skill-audit/**` changes
- flowchart integrity review when `docs/governance/workflow/**` changes

The minimum Gradle test command is required only if a slice changes product
source, tests, build logic, contracts, `QUALITY.md` or another file that can
influence the product build. This workflow forbids product code changes, so
any such change is a scope blocker unless the workflow is explicitly refined.

## Subagent Assignment

Callable subagents were not required during this `workflow create` turn because
the user did not request delegated or parallel agent execution. The mandatory
five-role review was performed as local role-review checklists.

During `workflow execute`, role or callable-subagent routing is:

- Senior Workflow Architect: overall workflow execution order and workflow
  metadata consistency.
- Senior Requirement Engineer: process requirement integrity and EPIC drift
  check.
- Senior System Architect: governance architecture, ADR and arc42 impact.
- Senior Documentation Engineer: process, agent, workflow, skill-audit and
  arc42 documentation synchronization.
- Senior Tester: quality-impact classification and verification plan.
- Senior Java Backend Developer: N/A impact check unless product backend files
  are unexpectedly touched.
- Senior React Frontend Developer: N/A impact check unless frontend files are
  unexpectedly touched.
- Skill Registry Conflict Auditor: required for new or changed skills, roles
  and routing rules.

## Ordered Slices

### Slice 00 - Execution Preflight And Workflow Context Freeze

```yaml
slice_id: S00
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-requirement-engineer
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/execution-report.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G00
file_locks:
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - workflow-execute-preflight
quality_gates:
  targeted:
    - git status --short --branch
    - git diff --check
  required:
    - git status --short --branch
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - active branch does not match workflow branch
  - working tree contains unrelated changes
  - workflow version cannot be verified
```

Purpose: verify the execution branch, clean worktree, workflow version, process
strand and context-pack state before any write-capable slice starts.

Done criteria:

- Execution branch and local ref are verified.
- `docs/workflow/context-pack.json` is either current or explicitly marked
  stale with a stop decision.
- No product implementation files are in the planned write set.

### Slice 01 - Add Execution Profile Router

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-requirement-engineer
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/skills/execution-profile-router/SKILL.md
  - .agents/orchestrator/routing-rules.md
  - .agents/orchestrator/swarm-orchestrator.md
  - docs/agents/skill-registry.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/governance/README.md
affected_modules: []
affected_contracts: []
dependencies:
  - S00
parallel_group: G01
file_locks:
  - .agents/skills/execution-profile-router/**
  - .agents/orchestrator/**
  - docs/agents/skill-registry.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/governance/README.md
contract_locks: []
architecture_locks:
  - agent-governance-routing
quality_gates:
  targeted:
    - git diff --check
    - rg -n "execution-profile-router|FAST_PATH|NORMAL_PATH|FULL_PATH" .agents docs
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - profile classification would bypass a mandatory root AGENTS.md rule
  - profile output lacks STOP rules
  - routing update changes process strand semantics
```

Purpose: introduce `FAST_PATH`, `NORMAL_PATH` and `FULL_PATH` classification
before specialist role routing.

Done criteria:

- New skill defines mission, responsibilities, forbidden scope, inputs,
  outputs, collaboration rules and STOP rules.
- Routing rules require classification before specialist assignment.
- Reduced reviews are limited to N/A impact checks and cannot waive required
  gates.

### Slice 02 - Add Quality Impact Classifier

```yaml
slice_id: S02
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - senior-system-architect
  - senior-documentation-engineer
affected_files:
  - .agents/skills/quality-impact-classifier/SKILL.md
  - .agents/skills/quality-gate-orchestrator/SKILL.md
  - .agents/skills/quality-gate-orchestrator/quality-gates.md
  - docs/process/workflow-execute.md
  - docs/process/branch-governance.md
  - docs/agents/skill-registry.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: G02
file_locks:
  - .agents/skills/quality-impact-classifier/**
  - .agents/skills/quality-gate-orchestrator/**
  - docs/process/workflow-execute.md
  - docs/process/branch-governance.md
  - docs/agents/skill-registry.md
contract_locks: []
architecture_locks:
  - quality-gate-governance
quality_gates:
  targeted:
    - git diff --check
    - rg -n "quality-impact-classifier|FAST_PATH|NORMAL_PATH|FULL_PATH|failed required" .agents docs
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - classifier can downgrade a failed required gate
  - classifier conflicts with QUALITY.md
  - Gradle commands are invented or renamed without build evidence
```

Purpose: formalize when documentation-only, governance-only and product
implementation slices require documentation checks, targeted tests, minimum
Gradle tests or the full local gate.

Done criteria:

- Quality matrix exists and preserves `QUALITY.md` authority.
- Documentation-only governance slices do not require Gradle by default.
- Product, build, source, test, contract and `QUALITY.md` changes still require
  the applicable Gradle gate.

### Slice 03 - Define Workflow Context Pack

```yaml
slice_id: S03
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-documentation-engineer
  - senior-system-architect
  - senior-tester
affected_files:
  - .agents/skills/workflow-authoring/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/workflow/context-pack.md
  - docs/workflow/context-pack.json
affected_modules: []
affected_contracts: []
dependencies:
  - S01
  - S02
parallel_group: G03
file_locks:
  - .agents/skills/workflow-authoring/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/workflow/context-pack.*
contract_locks: []
architecture_locks:
  - workflow-context-provenance
quality_gates:
  targeted:
    - git diff --check
    - python3 -m json.tool docs/workflow/context-pack.json
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - context pack claims stale hashes are current
  - context pack replaces source-of-truth files
  - subagents are told to skip rereading changed governing files
```

Purpose: define `docs/workflow/context-pack.md` and
`docs/workflow/context-pack.json` as a workflow-local summary and hash record.

Done criteria:

- Context pack is explicitly secondary evidence.
- Subagents reopen `AGENTS.md`, `QUALITY.md`, routing or workflow files when
  hashes change, governance files are touched or a conflict is detected.
- The pack records active strand, branch, profile, affected areas, required
  roles and quality commands.

### Slice 04 - Formalize Machine-Readable Slice Metadata

```yaml
slice_id: S04
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-swarm-orchestrator
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/skills/workflow-authoring/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - .agents/skills/three-amigos-requirement-gatekeeper/templates/slice-template.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/agents/skill-registry.md
affected_modules: []
affected_contracts: []
dependencies:
  - S03
parallel_group: G04
file_locks:
  - .agents/skills/workflow-authoring/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - .agents/skills/three-amigos-requirement-gatekeeper/templates/slice-template.md
  - docs/process/workflow-create.md
  - docs/process/workflow-execute.md
  - docs/agents/skill-registry.md
contract_locks: []
architecture_locks:
  - workflow-slice-metadata
quality_gates:
  targeted:
    - git diff --check
    - rg -n "slice_id|affected_files|dependencies|file_locks|quality_gates" .agents docs
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - metadata allows dependency ranges instead of concrete slice IDs
  - metadata omits locks or quality gates
  - metadata changes conflict with S3D STOP rules
```

Purpose: make future workflow slices parseable without guessing free-form
Markdown.

Done criteria:

- Required metadata fields are documented for every slice.
- S3D stop conditions name missing metadata, unknown dependencies, cycles and
  overlapping locks.
- Templates and process docs use the same field names.

### Slice 05 - Split Dedicated S3D Execution Orchestrator

```yaml
slice_id: S05
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-swarm-orchestrator
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/roles/senior-execution-orchestrator.md
  - .agents/skills/s3d-execution-orchestrator/SKILL.md
  - .agents/orchestrator/routing-rules.md
  - .agents/orchestrator/swarm-orchestrator.md
  - docs/agents/skill-registry.md
  - docs/process/workflow-execute.md
affected_modules: []
affected_contracts: []
dependencies:
  - S04
parallel_group: G05
file_locks:
  - .agents/roles/senior-execution-orchestrator.md
  - .agents/skills/s3d-execution-orchestrator/**
  - .agents/orchestrator/**
  - docs/agents/skill-registry.md
  - docs/process/workflow-execute.md
contract_locks: []
architecture_locks:
  - s3d-orchestration
quality_gates:
  targeted:
    - git diff --check
    - rg -n "senior-execution-orchestrator|s3d-execution-orchestrator|LOCK_CONFLICT" .agents docs
  required:
    - git diff --check
documentation:
  arc42: update_if_governance_model_changes
  adr: check
stop_conditions:
  - S3D becomes a fourth process strand
  - S3D is allowed to rewrite workflow-create artifacts during execution
  - lock conflict routing no longer reaches Typed Error Router
```

Purpose: move technical dependency graph planning and lock validation from
Senior Swarm Orchestrator to a dedicated S3D role and skill.

Done criteria:

- Swarm Orchestrator remains coordinator.
- S3D owns DAG, topological groups and lock validation.
- `LOCK_CONFLICT` routing remains explicit.

### Slice 06 - Persist Skill Registry Matrix

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/skills/skill-registry-conflict-auditor/SKILL.md
  - docs/agents/skill-registry.md
  - docs/skill-audit/skill-registry.md
  - docs/skill-audit/skill-registry.json
  - docs/process/skills-update.md
  - docs/process/workflow-execute.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: G06
file_locks:
  - .agents/skills/skill-registry-conflict-auditor/**
  - docs/agents/skill-registry.md
  - docs/skill-audit/**
  - docs/process/skills-update.md
  - docs/process/workflow-execute.md
contract_locks: []
architecture_locks:
  - skill-registry-governance
quality_gates:
  targeted:
    - git diff --check
    - python3 -m json.tool docs/skill-audit/skill-registry.json
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - cached registry is reused after governing files changed
  - registry marks unresolved conflicts as ready
  - persistent registry becomes the source of truth over repository files
```

Purpose: add a persistent, hash-invalidated skill registry matrix for routing
and conflict-audit reuse.

Done criteria:

- Registry reuse is allowed only when relevant hashes are unchanged.
- Missing owners and incompatible STOP rules remain blocking.
- Manual review is required when `.agents/**`, `.codex/**`, `AGENTS.md`,
  `QUALITY.md`, `docs/workflow/**` or `docs/skill-audit/**` changed.

### Slice 07 - Unify Branch Strategy Rules

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/skills/git-branch-strategy/SKILL.md
  - .agents/skills/release-branch-governance/branch-rules.md
  - .agents/skills/git-commit-preparation/SKILL.md
  - docs/process/branch-governance.md
  - docs/agents/skill-registry.md
  - AGENTS.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: G07
file_locks:
  - .agents/skills/git-branch-strategy/SKILL.md
  - .agents/skills/release-branch-governance/**
  - .agents/skills/git-commit-preparation/**
  - docs/process/branch-governance.md
  - docs/agents/skill-registry.md
  - AGENTS.md
contract_locks: []
architecture_locks:
  - branch-governance
quality_gates:
  targeted:
    - git diff --check
    - rg -n "workflow create|workflow execute|skills update|work/" AGENTS.md .agents docs
  required:
    - git diff --check
documentation:
  arc42: update_if_branch_governance_changes
  adr: check
stop_conditions:
  - commit preparation can create a generic branch that conflicts with the active strand
  - branch matrix contradicts ADR-0016
  - branch rules weaken shared-branch protection
```

Purpose: centralize branch naming for `skills update`, `workflow create`,
`workflow execute`, ad-hoc implementation and commit preparation on shared
branches.

Done criteria:

- Branch matrix has one source of truth or one authoritative cross-reference.
- `workflow create` keeps `feature/`, `fix/`, `docs/` and `architecture/`
  workflow branch forms.
- Generic `work/<task-slug>` creation is removed or guarded when it conflicts
  with active strand branch rules.

### Slice 08 - Add Flowchart Integrity Auditor

```yaml
slice_id: S08
profile: FULL_PATH
owner: senior-documentation-engineer
secondary_reviewers:
  - senior-system-architect
  - senior-tester
affected_files:
  - .agents/skills/flowchart-integrity-auditor/SKILL.md
  - .agents/orchestrator/routing-rules.md
  - docs/governance/workflow/README.md
  - docs/governance/workflow/level-1-overview.md
  - docs/governance/workflow/level-2-subgraphs.md
  - docs/agents/skill-registry.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
parallel_group: G08
file_locks:
  - .agents/skills/flowchart-integrity-auditor/**
  - .agents/orchestrator/routing-rules.md
  - docs/governance/workflow/**
  - docs/agents/skill-registry.md
contract_locks: []
architecture_locks:
  - governance-flowchart-integrity
quality_gates:
  targeted:
    - git diff --check
    - rg -n "flowchart-integrity-auditor|STOP|CP_FINAL|PUB_PUSH" .agents docs/governance docs/agents
  required:
    - git diff --check
documentation:
  arc42: update_if_gap_closed
  adr: check
stop_conditions:
  - auditor allows push auto self-reference
  - auditor allows workflow execute to call workflow create automatically
  - STOP, fallback or terminal nodes become ambiguous
```

Purpose: close the documented Flowchart Integrity Audit gap with a dedicated
skill and route.

Done criteria:

- Auditor checks yes/no/default paths, STOP paths, terminals, self-loops,
  backward jumps and Level 1/Level 2 consistency.
- Routing no longer requires Senior Documentation Engineer plus Senior System
  Architect as the primary flowchart-audit bootstrap owner.
- arc42 risk notes are updated if the gap is closed.

### Slice 09 - Clarify Workflow Executor Resolution

```yaml
slice_id: S09
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-system-architect
  - skill-registry-conflict-auditor
  - senior-documentation-engineer
affected_files:
  - .codex/skills/workflow-executor/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - .codex/AGENTS.md
  - docs/agents/skill-registry.md
  - docs/process/workflow-execute.md
affected_modules: []
affected_contracts: []
dependencies:
  - S01
  - S06
parallel_group: G09
file_locks:
  - .codex/skills/workflow-executor/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - .codex/AGENTS.md
  - docs/agents/skill-registry.md
  - docs/process/workflow-execute.md
contract_locks: []
architecture_locks:
  - workflow-executor-resolution
quality_gates:
  targeted:
    - git diff --check
    - rg -n "workflow-executor|project-specific|reusable base|override" .codex .agents docs
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - renaming front matter would break verified runtime skill discovery
  - portable .codex files receive project-specific Forensic Analytics rules
  - project-specific override precedence is ambiguous
```

Purpose: remove ambiguity between the reusable `.codex` workflow executor and
the project-specific `.agents` workflow executor.

Done criteria:

- The active Forensic Analytics executor is the `.agents` skill.
- The `.codex` skill remains reusable base protocol.
- If front-matter renaming is not proven safe, use explicit resolution text
  instead of renaming.

### Slice 10 - Add Process Performance Profiler

```yaml
slice_id: S10
profile: NORMAL_PATH
owner: senior-performance-engineer
secondary_reviewers:
  - senior-workflow-architect
  - senior-documentation-engineer
  - senior-tester
affected_files:
  - .agents/skills/process-performance-profiler/SKILL.md
  - .agents/skills/workflow-executor/SKILL.md
  - docs/process/workflow-execute.md
  - docs/workflow/metrics/README.md
  - docs/agents/skill-registry.md
affected_modules: []
affected_contracts: []
dependencies:
  - S03
  - S04
parallel_group: G10
file_locks:
  - .agents/skills/process-performance-profiler/**
  - .agents/skills/workflow-executor/SKILL.md
  - docs/process/workflow-execute.md
  - docs/workflow/metrics/**
  - docs/agents/skill-registry.md
contract_locks: []
architecture_locks:
  - process-performance-observability
quality_gates:
  targeted:
    - git diff --check
    - rg -n "process-performance-profiler|metrics|critical path|repeated reads" .agents docs
  required:
    - git diff --check
documentation:
  arc42: check
  adr: check
stop_conditions:
  - profiler records secrets, prompt content or raw evidence payloads
  - profiler blocks required quality gates
  - metrics become proof of correctness instead of operational diagnostics
```

Purpose: measure workflow process time and repeated governance work so future
optimizations are evidence-based.

Done criteria:

- Profiler records timing, role count, file-read count, quality commands,
  repeated governing-file reads, retries, blockers and critical path.
- Metrics are written under `docs/workflow/metrics/**` only during
  workflow-execute documentation updates.
- Metrics are diagnostics and never replace required reviews or quality gates.

### Slice 11 - Final Governance Synchronization And Release Gate

```yaml
slice_id: S11
profile: FULL_PATH
owner: senior-documentation-engineer
secondary_reviewers:
  - senior-system-architect
  - senior-requirement-engineer
  - senior-tester
affected_files:
  - AGENTS.md
  - QUALITY.md
  - .agents/**
  - .codex/**
  - docs/agents/**
  - docs/process/**
  - docs/governance/**
  - docs/skill-audit/**
  - docs/arc42/**
  - docs/adr/**
  - docs/workflow/**
affected_modules: []
affected_contracts: []
dependencies:
  - S05
  - S06
  - S07
  - S08
  - S09
  - S10
parallel_group: G11
file_locks:
  - AGENTS.md
  - QUALITY.md
  - .agents/**
  - .codex/**
  - docs/**
contract_locks: []
architecture_locks:
  - documentation-governance
  - arc42-governance
  - adr-governance
quality_gates:
  targeted:
    - git status --short --branch
    - git diff --check
    - python3 -m json.tool docs/workflow/context-pack.json
  required:
    - git diff --check
documentation:
  arc42: required_check_or_update
  adr: required_check_or_update
stop_conditions:
  - docs and routing disagree about owner or trigger
  - arc42 still reports a closed gap as open
  - ADR update is required but missing
  - root AGENTS.md and process docs conflict
```

Purpose: perform the final Documentation Governance, arc42/ADR impact check,
quality-impact summary and release decision for this workflow package.

Done criteria:

- All new skills and roles are in the registry.
- Routing, process docs, arc42 and ADR references agree.
- Context pack and persistent registry are either current or explicitly stale.
- No product implementation files changed.
- The workflow is released for final commit/push only after required checks pass.

## Dependency Graph

The detailed dependency diagram is maintained in
[`slice-dependency-map.md`](slice-dependency-map.md).

Execution summary:

- S00 is mandatory first.
- S01 and S02 are early controls and must finish before downstream optimization.
- S03 and S04 define context and parseable slice metadata before S3D is split.
- S05 waits for S04 because the dedicated S3D role depends on metadata rules.
- S06, S07, S08 and S09 can start after their direct dependencies when locks
  are disjoint.
- S10 waits for context-pack and metadata rules.
- S11 is final and waits for all governance slices.

## Parallelization Opportunities

Parallel execution is allowed only when S3D proves disjoint file locks.
Potential groups:

- G01 and G02 are serial because quality classification depends on profile
  classification semantics.
- G06 and G08 may be parallel after S01 when file locks are disjoint.
- G07 and G09 must not run in parallel if both touch `.agents/skills/git-*`,
  `.codex/AGENTS.md`, `docs/process/workflow-execute.md` or root `AGENTS.md`.
- S11 is serial.

## Quality Gates

Quality authority is `QUALITY.md`.

For this governance-only workflow:

- Required for every slice: `git diff --check`.
- Required before checkpoint commit: staged diff ownership and
  `git diff --cached --check`.
- Required when JSON artifacts are changed:
  `python3 -m json.tool <json-file>`.
- Required when product source, tests, build logic, contracts or `QUALITY.md`
  are changed: stop as out-of-scope unless the workflow is refined; if refined,
  run the minimum Gradle command from `QUALITY.md`.
- Required before final release if scope expands into product build influence:
  the full local quality gate from `QUALITY.md`.

Do not claim any Gradle gate passed unless it was actually executed.

## Documentation Synchronization Points

- Every skill or role change updates `docs/agents/skill-registry.md`.
- Every routing change updates `.agents/orchestrator/routing-rules.md`.
- Every process-strand behavior change updates `docs/process/**`.
- Every governance-flowchart change updates `docs/governance/workflow/**`.
- Closing a documented governance gap updates arc42 risks and quality scenarios.
- ADRs are updated only when an accepted decision changes, not for routine
  implementation of an existing decision.

## Stop Conditions

Stop workflow execution if:

- active branch does not match `architecture/workflow-governance-performance-20260521`;
- product implementation files are modified;
- a slice touches source, contracts, build logic, tests or `QUALITY.md` without
  explicit workflow refinement;
- execution profile or quality-impact classification can bypass mandatory
  `AGENTS.md`, `QUALITY.md`, ADR or STOP rules;
- cached context or registry artifacts are reused after relevant hashes changed;
- workflow-executor resolution would require guessing runtime skill discovery;
- branch rules conflict with ADR-0016;
- flowchart integrity rules allow missing STOP paths, self-references or
  automatic `workflow execute` to `workflow create` jumps;
- any required file, route, role, skill or quality command cannot be verified.

## Uncertainty Escalation

Unclear governance authority escalates to Senior System Architect / Root
Architect path. Automatic clarification or correction loops are capped at
`maxRetries = 3`. After the third unresolved attempt, workflow execution stops
and reports the attempted loop, blocker, files or decisions involved and why
continuing automatically would be unsafe.

## Commit And Push Plan

This `workflow create` turn does not commit or push unless the user explicitly
requests it.

During later `workflow execute`:

- each successful slice creates exactly one slice-scoped checkpoint commit;
- the checkpoint push targets only `origin/architecture/workflow-governance-performance-20260521`;
- checkpoint push does not create or merge a PR, run branch cleanup, force-push
  or run `push auto`;
- `push auto` remains restricted to `skills-agents`.

## Definition Of Done

- `docs/workflow/workflow.md` is complete and checked.
- `docs/workflow/three-amigos-decision-record.md` records `READY_FOR_WORKFLOW`.
- `docs/workflow/slice-dependency-map.md` matches slice metadata.
- `docs/workflow/quality-and-leakage-gates.md` matches `QUALITY.md`.
- `docs/workflow/context-pack.json` validates as JSON.
- arc42 governance documentation is checked or updated.
- No product implementation files are changed by workflow creation.
- Final `git diff --check` passes.

## Handoff To Workflow Execute

`workflow execute` may start only after this workflow package is committed or
otherwise accepted by the user on the workflow branch. Execution must begin with
Slice S00 and must use the checked branch, context pack and slice metadata from
this workflow.

## arc42 Check Status

arc42 was checked for workflow creation. The workflow changes process
governance architecture, not product runtime architecture. The immediate arc42
update is limited to the repository governance check status. Later slices must
update arc42 quality scenarios, risks or architecture-decision references when
they close documented gaps or change accepted governance behavior.
