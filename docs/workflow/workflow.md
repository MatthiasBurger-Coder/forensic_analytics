# Workflow: GOV-01–GOV-05 Governance Registry and Agent Definition Reconciliation

## Executive Summary

This governance-only workflow covers:

- #115 / GOV-01: refresh Skill Registry hashes and synchronize the governance cache;
- #116 / GOV-02: reconcile role inventory count and entry-point classification;
- #117 / GOV-03: resolve Root Architect and flowchart-integrity mapping gaps;
- #118 / GOV-04: standardize skill-definition schema across `.agents`;
- #119 / GOV-05: classify and document portability of `.codex` agent definitions.

No product code, runtime behavior, contracts, persistence, build logic or
analytics behavior is in scope.

## Requirement Clarification and Profile

Original request: create a workflow for GOV-01 through GOV-05.

Interpreted intent: produce a branch-isolated, executable governance workflow
that verifies repository facts before changing registry, role, skill,
flowchart or portability documentation.

Change type: governance documentation and agent-definition governance.
Process strand: `workflow create` now; `workflow execute` only when explicitly
requested later.
Execution profile: `FULL_PATH`, because governance authority, registry state,
role/skill ownership, workflow documentation and `.codex` portability are
affected.

Decision: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` at 82 percent confidence. No
issue-specific EPIC file or complete acceptance criteria for #115–#119 was
found under `docs/epics`; the five titles are therefore treated as the scope
input. Each execution slice must derive exact acceptance evidence from
repository files and STOP if a behavior or ownership decision is unverifiable.

Assumptions:

- Governance-only files may be changed under `.agents/**`, `.codex/**`,
  `docs/agents/**`, `docs/process/**`, `docs/governance/**`,
  `docs/skill-audit/**`, `docs/workflow/**` and checked `docs/arc42/**` notes.
- A dedicated Root Architect role or new flowchart skill is not created unless
  execution verifies that the issue explicitly authorizes it; bootstrap
  ownership remains with Senior System Architect.
- No compatibility aliases, generated behavior or publication are required.

Non-goals:

- Do not activate the `skills-agents` strand or run `push auto`.
- Do not change product implementation, Java/React/build/runtime/contracts.
- Do not invent a role, skill, agent, schema field or acceptance criterion.

## Verified Baseline

- Repository root: `/home/matthias/projects/forensic_analytics`.
- Workflow branch: `feature/workflow-gov-skill-governance-20260814`.
- The working tree was clean on `main` before branch creation.
- Actual inventory: 77 `.agents/skills/**/SKILL.md`, 19 `.agents/roles`
  role files (including directory-style roles), 6 reusable `.codex` skills,
  and 34 `.codex/agents/*.toml` definitions.
- The existing registry JSON records 18 project roles and is
  `MANUAL_REVIEW_REQUIRED`; the discrepancy is GOV-02 scope.
- No matching EPIC file was found under `docs/epics`.
- Relevant governance sources include ADR-0015, ADR-0021, the Governance
  Flowchart V2 package, and the documented Root Architect bootstrap mapping.

## Target Picture

At handoff to `workflow execute`, the repository has a deterministic,
source-derived registry cache; reconciled counts; explicit entry-point
classification; owner-backed Root Architect and flowchart mapping; one
verified `.agents` skill schema; and a portability classification for every
`.codex/agents/*.toml`. Unresolved gaps remain explicit and traceable.

## Architecture, Evidence and Resilience Constraints

- Repository files are the source of truth; registries are derived caches and
  must be invalidated when governing files change.
- Keep project-specific `.agents` governance separate from reusable `.codex`.
- Do not mark ownership or portability verified by naming symmetry alone.
- Preserve the three strands and `maxRetries = 3` escalation cap.
- Every inventory, hash and classification must be deterministic for the same
  repository tree.
- Missing exact paths, conflicting authority or ambiguous ownership is STOP;
  route through the documented Root Architect / Senior System Architect path.

## Backend Assessment

No Java backend, application, domain, adapter, persistence, runtime or build
surface is affected. Senior Java Backend Developer performs the required N/A
impact review and must STOP if execution discovers a product-code dependency.

## Frontend Assessment

No React module, UI state, API adapter, reporting view or UX surface is affected.
Senior React Frontend Developer performs the required N/A impact review and must
STOP if execution discovers a frontend dependency.

## Test Strategy

The slices use deterministic repository checks: exact path existence, SHA-256
recomputation, role/skill/agent counts, frontmatter parsing, TOML/JSON parsing,
reference validation, flowchart integrity and dependency/lock validation. The
repository minimum Gradle test command remains required by the workflow; no
synthetic forensic evidence is created.

## Resilience Requirements

Inventory generation must be repeatable and bounded to the checked repository
tree. Hash/cache mismatches cause manual refresh rather than stale reuse. A
missing owner, malformed definition or unresolved flowchart path is represented
as an explicit gap. Automatic clarification/correction is capped at three
attempts and then escalates; no silent fallback or cross-strand retry is allowed.

## Required Reviews and Ownership

The workflow-create gate includes all five mandatory perspectives:

| Role | Responsibility |
|---|---|
| Senior Requirement Engineer | scope, traceability, assumptions and issue alignment |
| Senior System Architect | governance authority, Root Architect and arc42 alignment |
| Senior Java Backend Developer | N/A impact check; no Java/product boundary crossed |
| Senior React Frontend Developer | N/A impact check; no frontend surface crossed |
| Senior Tester | deterministic checks and quality-gate selection |

Additional governance reviews: Senior Workflow Architect, Senior Documentation
Engineer, Skill Registry Conflict Auditor and `flowchart-integrity-auditor`.
Callable subagents are not used because the request authorizes workflow
creation, not delegated execution; role files and skills are review checklists.

## Ordered Slices

### Slice 01 — GOV-01: Registry Hash and Cache Refresh

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior System Architect / Skill Registry Conflict Auditor
secondary_reviewers: [Senior Requirement Engineer, Senior Tester]
affected_files: [docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json]
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G1
file_locks: [docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json]
contract_locks: []
architecture_locks: [governance-registry-source-of-truth]
quality_gates:
  targeted: [path existence, hash recomputation, JSON syntax, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; update only for verified authority change
  adr: [ADR-0015, ADR-0021]
stop_conditions: [unreproducible hash, cache treated as source of truth, missing governing file]
```

Recompute and reconcile hashes and cache metadata from the verified tree. Do
not hand-edit a hash without reproducible evidence.

### Slice 02 — GOV-02: Role Inventory and Entry-Point Classification

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior Requirement Engineer
secondary_reviewers: [Senior System Architect, Senior Tester, Skill Registry Conflict Auditor]
affected_files: [docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json, docs/agents/skill-registry.md, docs/agents/organigramm.md]
affected_modules: []
affected_contracts: []
dependencies: [S01]
parallel_group: G2
file_locks: [docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json, docs/agents/skill-registry.md, docs/agents/organigramm.md]
contract_locks: []
architecture_locks: [role-entry-point-ownership]
quality_gates:
  targeted: [role count reconciliation, entry-point path validation, markdown/json syntax, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; synchronize only verified governance references
  adr: [ADR-0015, ADR-0021]
stop_conditions: [unverified role, unexplained count, guessed owner]
```

Reconcile flat and directory-style role artifacts and make the 19-versus-18
discrepancy explicit or correct it from source-derived evidence.

### Slice 03 — GOV-03: Root Architect and Flowchart Integrity Mapping

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers: [Senior Documentation Engineer, flowchart-integrity-auditor, Senior Requirement Engineer]
affected_files: [docs/skill-audit/governance-flowchart-v2-linkage.md, docs/skill-audit/manual-review-required.md, docs/governance/workflow/level-1-overview.md, docs/governance/workflow/level-2-subgraphs.md, docs/agents/skill-registry.md, docs/arc42/11-risks-and-technical-debt.md]
affected_modules: []
affected_contracts: []
dependencies: [S01, S02]
parallel_group: G3
file_locks: [docs/skill-audit/governance-flowchart-v2-linkage.md, docs/skill-audit/manual-review-required.md, docs/governance/workflow/level-1-overview.md, docs/governance/workflow/level-2-subgraphs.md]
contract_locks: []
architecture_locks: [root-architect-escalation, flowchart-level-consistency]
quality_gates:
  targeted: [flowchart integrity audit, source-to-diagram linkage, markdown syntax, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: update risks only if mapping status changes
  adr: [ADR-0021]
stop_conditions: [unauthorized dedicated owner, ambiguous diagram path, unresolved STOP/terminal/backward path]
```

Resolve or explicitly retain the two documented non-blocking gaps. Do not
invent a role or skill just to change a status to `VERIFIED`.

### Slice 04 — GOV-04: Skill Definition Schema Standardization

```yaml
slice_id: S04
profile: FULL_PATH
owner: Senior Documentation Engineer / Skill Registry Conflict Auditor
secondary_reviewers: [Senior System Architect, Senior Requirement Engineer, Senior Tester]
affected_files: [.agents/skills/**/SKILL.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json]
affected_modules: []
affected_contracts: []
dependencies: [S01, S02, S03]
parallel_group: G4
file_locks: [.agents/skills/**/SKILL.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json]
contract_locks: [skill-definition-frontmatter]
architecture_locks: [skill-schema-ownership]
quality_gates:
  targeted: [frontmatter parse for every SKILL.md, required-field report, duplicate-name report, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; no update unless governance architecture changes
  adr: [ADR-0015]
stop_conditions: [schema field cannot be derived, normalization erases meaning, duplicate owner unresolved]
```

Define and apply one verified schema across `.agents` while preserving skill
meaning. Missing fields become explicit findings when they cannot be safely
populated.

### Slice 05 — GOV-05: `.codex` Agent Portability Classification

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior System Architect
secondary_reviewers: [Senior Documentation Engineer, Skill Registry Conflict Auditor, Senior Tester]
affected_files: [.codex/agents/*.toml, .codex/AGENTS.md, docs/agents/README.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md]
affected_modules: []
affected_contracts: []
dependencies: [S02, S04]
parallel_group: G5
file_locks: [.codex/agents/*.toml, docs/agents/README.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md]
contract_locks: [codex-agent-portability-classification]
architecture_locks: [portable-codex-boundary]
quality_gates:
  targeted: [TOML parse for every agent, classification coverage, project-reference scan, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; update only for verified portability-boundary decisions
  adr: [ADR-0021]
stop_conditions: [unverified coupling, conflated portable/project scope, invalid TOML]
```

Classify all 34 definitions as reusable, project-specific or manual-review
required from verified references. Do not silently rewrite portable files with
project-specific governance.

### Slice 06 — Final Governance Synchronization and Execute Handoff

```yaml
slice_id: S06
profile: FULL_PATH
owner: Senior Workflow Architect
secondary_reviewers: [Senior Requirement Engineer, Senior System Architect, Senior Tester, Senior Documentation Engineer]
affected_files: [docs/workflow/**, docs/arc42/README.md, docs/arc42/08-crosscutting-concepts.md, docs/arc42/11-risks-and-technical-debt.md]
affected_modules: []
affected_contracts: []
dependencies: [S01, S02, S03, S04, S05]
parallel_group: G6
file_locks: [docs/workflow/**, docs/arc42/** governance-only]
contract_locks: []
architecture_locks: [workflow-handoff, arc42-governance-synchronization]
quality_gates:
  targeted: [metadata validation, dependency acyclicity, context-pack hash check, markdown/json syntax, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked or updated with exact evidence
  adr: [ADR-0015, ADR-0021]
stop_conditions: [missing evidence, lock conflict, stale context pack, ambiguous execute handoff]
```

Aggregate evidence, synchronize checked arc42 status, validate all slice
contracts, and release the workflow for a later explicit `workflow execute`.

## Dependency Graph and Parallelization

```text
S01 -> S02 -> S03 -> S04 -> S05 -> S06
```

The slices are intentionally sequential because later slices consume registry,
ownership or schema decisions. No write-capable parallel execution is allowed.

## Quality and Documentation Plan

The required minimum is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full gate is the exact `QUALITY.md` command including clean test, JaCoCo,
`checkPackageCoverage`, strict dependency verification and `git diff --check`.
`validatePlugins` is not applicable unless plugin metadata or implementation
changes. Each slice also runs its targeted deterministic governance checks.

Arc42 status: `CHECKED`. ADR-0015, ADR-0021, the Governance Flowchart V2
package, `docs/arc42/08-crosscutting-concepts.md` and
`docs/arc42/11-risks-and-technical-debt.md` were inspected. Slice 06 owns any
verified synchronization; no arc42 file is changed merely to close a gap.

## Commit and Push Plan

No commit or push is requested by the user. This workflow-creation turn does
not run checkpoint push, `push`, `push auto`, PR creation, merge or cleanup.
If a later `workflow execute` request authorizes checkpoint pushes, each slice
may commit only its own verified files after its quality gate and may push only
the active workflow branch; publication and branch cleanup remain separate
authorizations.

## Stop Conditions

STOP on missing exact paths, unverifiable counts, unresolved owner conflicts,
invalid frontmatter/TOML/JSON, stale hashes, flowchart integrity failures,
cross-strand scope or any need to guess. Automatic clarification or local
correction is capped at `maxRetries = 3`, then escalates through the documented
Root Architect path without switching strands.

## Definition of Done

Done means every issue has one traceable slice; metadata, owners, locks,
dependencies and checks are complete; registry/count/ownership/schema/
portability claims are source-derived; `.agents` and `.codex` boundaries stay
explicit; arc42 status and context hashes are checked; and handoff is ready
for a later `workflow execute` command.

## Handoff to Workflow Execute

Execute only after explicit user request on
`feature/workflow-gov-skill-governance-20260814`. Read this file completely,
process S01–S06 in order, verify the branch before each write, run the gates
after every slice, inspect the diff, and stop on any unverifiable fact.

## Arc42 Check Status

Checked against the existing arc42 governance baseline and ADR-0015 / ADR-0021.
No arc42 artifact was modified during workflow creation; Slice 06 owns any
verified synchronization required after execution.
