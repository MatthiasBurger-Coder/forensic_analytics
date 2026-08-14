# Workflow: GOV-02 and GOV-04 Role Inventory Validation and Skill Schema Standardization

## Executive Summary

This workflow completes the remaining governance work for:

- #116 / GOV-02: reconcile role inventory count and entry-point classification
  with a repeatable validator;
- #118 / GOV-04: define and apply a canonical .agents skill-definition
  schema with machine-checkable validation.

The workflow is documentation-, governance- and validation-tooling-only. It
must not change product behavior, service contracts, persistence, runtime,
Docker, frontend behavior or analytics evidence processing.

## Workflow Identity

- workflowId: `gov-02-04-role-inventory-skill-schema`
- workflowVersion: `gov-02-04-role-inventory-skill-schema-v1`
- processStrand: `workflow execute` after workflow-create approval
- branch: `feature/workflow-gov-02-04-20260814`
- workflow history: [`workflow.history.md`](workflow.history.md)
- context pack: [`context-pack.md`](context-pack.md)

The workflow version is stable for S01–S05. A scope, dependency or governance
rule change requires a new workflow-create version before execution continues.

## Requirement Clarification and Three Amigos Decision

Original request: create a workflow for GOV-02 and GOV-04 so the remaining
issues can be completed.

Interpreted intent: author a branch-isolated, executable workflow that closes
the remaining acceptance gaps without treating the current registry cache as
the source of truth.

Change type: governance tooling, documentation and agent-definition
standardization.

Affected process strand: workflow create; later execution uses the separate
workflow execute strand.

Requirement classifications:

- functional: deterministic role and skill validation reports;
- quality-gate: actionable validation failures and reproducible checks;
- documentation: canonical schemas, exceptions, counts and portability
  boundaries;
- architecture governance: preserve role ownership, process-strand separation
  and ADR-0015 / ADR-0021 authority;
- non-functional: deterministic output for the same repository tree.

Explicit requirements:

- GOV-02 must define one role-counting rule, distinguish flat and
  directory-style entry points, resolve every registered role path and report
  duplicate logical role names or missing entry points.
- GOV-04 must define required and optional skill metadata/sections, provide
  actionable machine-checkable validation, preserve existing STOP and
  forbidden rules, document exceptions and keep routing references resolvable.
- Both validators must be runnable without product build or runtime behavior.
- Registry files remain derived caches and must be refreshed only from verified
  repository sources.

Accepted assumptions:

- No matching EPIC file exists under docs/epics; issue #116 and #118 text is
  the accepted requirement source for this workflow.
- The validators may be repository-local governance scripts under
  docs/skill-audit/; no Gradle or product module integration is required
  unless execution verifies a concrete existing integration point.
- Existing skill semantics, STOP rules and forbidden scopes are authoritative;
  migration must not mechanically rewrite meaning.
- The current inventory baseline is 77 project skills, 20 project roles,
  6 reusable Codex skills and 34 Codex agent definitions.

Non-goals:

- no product Java, React, service, API, persistence, runtime or deployment
  changes;
- no new microservice or shared implementation module;
- no automatic issue closure before acceptance evidence is complete;
- no weakening of AGENTS.md, QUALITY.md, ADRs or process-strand rules;
- no use of registry JSON as the source of truth.

Decision: PROCEED_WITH_ACCEPTED_ASSUMPTIONS at 86 percent confidence.
Remaining assumptions are non-blocking and are recorded above. The workflow
must STOP if execution discovers a missing exact source, contradictory schema
authority or a validator design that would require guessing.

## Verified Baseline

- Repository: /home/matthias/projects/forensic_analytics.
- Workflow branch: feature/workflow-gov-02-04-20260814.
- Current branch is dedicated and active before workflow artifacts are written.
- GOV-02 and GOV-04 are open; GOV-01, GOV-03 and GOV-05 are closed.
- .agents/roles contains 18 flat role documents and 2 directory-style
  SKILL.md role entry points, for 20 roles.
- .agents/skills contains 77 SKILL.md entry points with verified
  name/description frontmatter.
- The current registry and audit documents are derived evidence; source files
  remain authoritative.
- ADR-0015 and ADR-0021 are accepted and were checked.
- docs/arc42 was checked; this workflow introduces no product architecture or
  runtime change. Slice 04 owns any verified governance-risk note update.

## Target Picture

At handoff to workflow execute:

- GOV-02 has a repeatable role-inventory validator with explicit logical-role
  and physical-entry-point rules;
- GOV-04 has a documented canonical skill schema, accepted aliases/exceptions
  and a validator that reports actionable file/section failures;
- all affected registry, inventory, routing and manual-review documents are
  source-derived and hash-synchronized;
- existing skill responsibilities and STOP rules remain intact;
- acceptance evidence is complete enough for issue closure.

## Architecture, Evidence and Resilience Constraints

- Repository source files are the authority; JSON/Markdown registries are
  invalidatable caches.
- Role and skill identities must resolve to exact files or explicitly declared
  schema exceptions.
- Validation output must preserve unresolved or unsupported cases as findings;
  it must not silently normalize them away.
- Validators must be deterministic, bounded to the checked repository tree and
  safe to run from a clean checkout.
- No validator may execute product code, invoke external services or mutate
  source definitions implicitly.
- Automatic correction loops remain capped at maxRetries = 3; unresolved
  governance conflicts escalate to the Root Architect.

## Backend Assessment

N/A. No Java backend, domain, application, adapter, persistence, contract or
runtime surface is in scope. The Senior Java Backend Developer performs the
required N/A impact review and must STOP if execution discovers a product-code
dependency.

## Frontend Assessment

N/A. No React module, UI state, API adapter, reporting view or UX surface is in
scope. The Senior React Frontend Developer performs the required N/A impact
review and must STOP if execution discovers a frontend dependency.

## Test Strategy

Each slice must validate the smallest meaningful artifact first:

- shell syntax and executable validator behavior;
- deterministic counts, duplicate detection and missing-path diagnostics;
- frontmatter/section/schema validation for all 77 skills;
- JSON syntax, registry path resolution and hash recomputation;
- routing/reference scans and git diff --check.

The required repository minimum remains:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The final workflow gate uses the exact full local gate from QUALITY.md:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

validatePlugins is not applicable unless execution changes Gradle plugin
metadata, task inputs/outputs or plugin implementation classes.

## Resilience Requirements

- A validator failure must identify the exact file, entry point, field or
  section and return a non-zero result.
- Missing optional metadata remains an explicit finding when it cannot be
  derived safely.
- No validator may auto-edit skill or role files.
- Repeated correction attempts stop after maxRetries = 3.
- A stale registry or context pack blocks handoff until refreshed from source.

## Required Roles and Ownership

| Role | Responsibility |
|---|---|
| Senior Workflow Architect | workflow structure, dependencies and handoff |
| Senior Requirement Engineer | issue traceability, acceptance criteria and scope |
| Senior System Architect | authority, architecture and ADR alignment |
| Senior Java Backend Developer | N/A product-boundary review |
| Senior React Frontend Developer | N/A frontend-boundary review |
| Senior Tester | deterministic validator and quality-gate review |
| Senior Documentation Engineer | registry, audit, process and arc42 synchronization |
| Skill Registry Conflict Auditor | owner, conflict, cache and STOP-rule review |

Callable subagents are not required by the user request. If delegated review
is requested later but unavailable, the matching role files are used as local
review checklists and the limitation is reported.

## Ordered Slices

### Slice 01 — GOV-02: Role Inventory Contract and Validator

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Requirement Engineer
secondary_reviewers: [Senior System Architect, Senior Tester, Skill Registry Conflict Auditor]
affected_files: [docs/skill-audit/validate-role-inventory.sh, docs/skill-audit/skill-inventory.md, docs/agents/skill-registry.md]
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G1
file_locks: [docs/skill-audit/validate-role-inventory.sh, docs/skill-audit/skill-inventory.md, docs/agents/skill-registry.md]
contract_locks: [role-inventory-counting-rule]
architecture_locks: [governance-source-of-truth]
quality_gates:
  targeted: [bash -n validator, role-count check, duplicate-name check, entry-point resolution, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; update only if a verified governance risk changes
  adr: [ADR-0015]
stop_conditions: [missing role entry point, ambiguous logical-role rule, validator mutates source files]
```

Define the logical-role versus physical-entry-point rule, implement a
repository-local deterministic validator and reconcile the human-readable
inventory and registry ownership documentation. The validator must report
flat documents, directory-style entry points, duplicate logical names and
missing paths separately.

Done criteria:

- validator exists, is syntax-checked and exits non-zero for a controlled
  invalid fixture or verified failure condition without modifying sources;
- current repository reports 18 flat roles, 2 directory entry points and 20
  total roles;
- every registered role path resolves;
- duplicate logical names and missing entry points are explicitly reported as
  zero findings;
- GOV-02 acceptance evidence is recorded.

### Slice 02 — GOV-04: Canonical Skill Definition Schema

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior Documentation Engineer
secondary_reviewers: [Senior System Architect, Senior Requirement Engineer, Senior Tester, Skill Registry Conflict Auditor]
affected_files: [docs/skill-audit/skill-definition-schema.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md]
affected_modules: []
affected_contracts: [skill-definition-schema]
dependencies: [S01]
parallel_group: G2
file_locks: [docs/skill-audit/skill-definition-schema.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.md]
contract_locks: [skill-definition-schema]
architecture_locks: [skill-schema-ownership]
quality_gates:
  targeted: [schema document completeness, required/optional field review, alias review, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; update only for verified governance consequence
  adr: [ADR-0015]
stop_conditions: [schema erases skill meaning, required field cannot be derived, duplicate schema owner]
```

Document one canonical schema for .agents/skills/**/SKILL.md. Define
required frontmatter and required/optional body sections, accepted legacy
aliases, exception format, ownership, forbidden scope, inputs, outputs,
collaboration rules and STOP rules. The document must distinguish a
machine-checkable requirement from a semantic section that requires human
review.

Done criteria:

- schema authority is one exact file;
- required versus optional fields and accepted aliases are explicit;
- existing name/description frontmatter rules remain mandatory;
- schema migration cannot weaken STOP or forbidden behavior;
- exceptions are typed, justified and traceable.

### Slice 03 — GOV-04: Skill Schema Migration and Validator

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior Documentation Engineer / Skill Registry Conflict Auditor
secondary_reviewers: [Senior System Architect, Senior Requirement Engineer, Senior Tester]
affected_files: [docs/skill-audit/validate-skill-schema.sh, .agents/skills/**/SKILL.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.json]
affected_modules: []
affected_contracts: [skill-definition-schema]
dependencies: [S02]
parallel_group: G3
file_locks: [docs/skill-audit/validate-skill-schema.sh, .agents/skills/**/SKILL.md, docs/skill-audit/skill-inventory.md, docs/skill-audit/skill-registry.json]
contract_locks: [skill-definition-schema]
architecture_locks: [skill-schema-ownership, stop-rule-preservation]
quality_gates:
  targeted: [bash -n validator, all-skill schema validation, actionable failure report, duplicate-name check, registry JSON validation, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked; update only if a verified governance risk changes
  adr: [ADR-0015]
stop_conditions: [semantic rewrite without evidence, weakened STOP rule, unresolved duplicate owner, non-deterministic report]
```

Implement the schema validator and migrate only verified non-semantic metadata
or sections. Every one of the 77 skills must either conform or have an
explicit documented exception. The validator must identify file, missing
field/section, alias or exception and must not rewrite files automatically.

Done criteria:

- all 77 skill entry points are parsed;
- required metadata is validated;
- required/optional sections and accepted aliases are reported;
- exceptions are actionable and countable;
- routing and collaboration references remain resolvable;
- STOP and forbidden sections remain present and semantically intact;
- GOV-04 acceptance evidence is recorded.

### Slice 04 — Registry, Audit and Workflow Handoff Synchronization

```yaml
slice_id: S04
profile: FULL_PATH
owner: Senior System Architect / Senior Workflow Architect
secondary_reviewers: [Senior Requirement Engineer, Senior Documentation Engineer, Senior Tester, Skill Registry Conflict Auditor]
affected_files: [docs/skill-audit/skill-registry.md, docs/skill-audit/skill-registry.json, docs/skill-audit/skill-inventory.md, docs/skill-audit/manual-review-required.md, docs/agents/skill-registry.md, docs/arc42/11-risks-and-technical-debt.md]
affected_modules: []
affected_contracts: [role-inventory-counting-rule, skill-definition-schema]
dependencies: [S01, S02, S03]
parallel_group: G4
file_locks: [docs/skill-audit/**, docs/agents/skill-registry.md, docs/arc42/11-risks-and-technical-debt.md]
contract_locks: [role-inventory-counting-rule, skill-definition-schema]
architecture_locks: [governance-registry-source-of-truth, documentation-governance]
quality_gates:
  targeted: [both validators, source-hash recomputation, JSON syntax, registry path resolution, routing scan, git diff --check]
  required: [./gradlew test --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: update only for verified current-risk status
  adr: [ADR-0015, ADR-0021]
stop_conditions: [stale cache, unexplained count, unresolved exception, registry treated as source of truth]
```

Refresh all derived registry/audit records from the verified validator output,
remove only findings proven resolved, retain unresolved exceptions explicitly
and synchronize the agent registry. Do not close #116 or #118 until their
acceptance evidence is complete.

Done criteria:

- both validators pass against the current tree;
- registry hashes and counts match source-derived output;
- all referenced paths resolve;
- manual-review status reflects only actual unresolved findings;
- no stale issue or workflow claim remains.

### Slice 05 — Final Governance Quality Gate and Issue Handoff

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior Workflow Architect
secondary_reviewers: [Senior Requirement Engineer, Senior System Architect, Senior Documentation Engineer, Senior Tester]
affected_files: [docs/workflow/**, docs/arc42/README.md, docs/arc42/11-risks-and-technical-debt.md]
affected_modules: []
affected_contracts: []
dependencies: [S01, S02, S03, S04]
parallel_group: G5
file_locks: [docs/workflow/**, docs/arc42/** governance-only]
contract_locks: []
architecture_locks: [workflow-handoff, arc42-governance-synchronization]
quality_gates:
  targeted: [workflow metadata validation, dependency acyclicity, context-pack hash check, both validators, JSON syntax, git diff --check]
  required: [./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace]
documentation:
  arc42: checked or updated with exact evidence
  adr: [ADR-0015, ADR-0021]
stop_conditions: [missing evidence, stale context pack, ambiguous issue handoff, quality-gate failure]
```

Aggregate the acceptance evidence, refresh the workflow context pack, record
the final quality result and prepare issue comments for #116 and #118. Issue
closure is a separate publication/coordination action and must occur only
after the workflow is merged and the acceptance evidence is verified.

## Dependency Graph and Parallelization

```text
S01 -> S02 -> S03 -> S04 -> S05
```

No write-capable parallelization is allowed. S02 and S03 share schema
ownership; S04 consumes both validator outputs; S05 owns the final handoff.

## Quality and Documentation Plan

Every slice starts with targeted deterministic checks and git diff --check,
then runs the required minimum Gradle test command. Slice 05 runs the exact
full local gate from QUALITY.md. No optional SonarCloud check is treated as a
substitute for the local gate.

Arc42 status: CHECKED. ADR-0015 and ADR-0021 were reviewed. No product
architecture, runtime, deployment or service-boundary change is planned; the
arc42 risk document is updated only if execution verifies a governance-risk
transition.

## Commit and Push Plan

No commit, push, PR creation or issue closure is requested by this workflow
creation action. A later explicit workflow execute may create one
slice-scoped checkpoint commit after each successful quality gate. Publication
remains separate from issue closure.

## Stop Conditions

STOP on missing exact paths, unverifiable acceptance criteria, ambiguous
schema ownership, registry/cache drift, missing validator output, semantic
skill rewrites without evidence, unresolved duplicate owners, dependency cycles,
quality-gate failures, stale context-pack hashes or any product-scope change.
Automatic correction attempts are capped at maxRetries = 3.

## Definition of Done

- all slice metadata is complete and dependency-acyclic;
- GOV-02 and GOV-04 acceptance criteria are evidenced by repeatable checks;
- role and skill registries are source-derived and hash-synchronized;
- all exceptions and unresolved findings remain explicit;
- required quality gates pass;
- checked workflow/context-pack and arc42 status are synchronized;
- issue comments contain exact evidence before #116/#118 are closed.

## Handoff to Workflow Execute

Execute only after an explicit workflow execute request on
feature/workflow-gov-02-04-20260814. Read this complete workflow, process
S01-S05 in order, verify the branch before each write, run the required gates
after every slice and stop on unverifiable assumptions.

## Arc42 Check Status

Checked against docs/arc42/README.md,
docs/arc42/11-risks-and-technical-debt.md, ADR-0015 and ADR-0021. No arc42
change is required during workflow creation because this workflow changes
governance validation and documentation only.

## Execution Status

READY_FOR_EXECUTION
