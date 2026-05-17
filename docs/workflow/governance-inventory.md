# Governance Inventory

## Repository Baseline

| Item | Verified path |
|---|---|
| Repository root | `/mnt/d/Projects/forensic_analytics` |
| Active workflow branch | `architecture/workflow-governance-flowchart-v2-20260517` |
| Root agent rules | `AGENTS.md` |
| Quality contract | `QUALITY.md` |
| Active workflow folder | `docs/workflow/` |
| Process docs | `docs/process/` |
| Agent docs | `docs/agents/` |
| Governance docs | `docs/governance/` |
| arc42 docs | `docs/arc42/` |
| ADR docs | `docs/adr/` |
| Project roles | `.agents/roles/` |
| Project skills | `.agents/skills/` |
| Orchestrator docs | `.agents/orchestrator/` |
| Codex reusable workflow docs | `.codex/` |

## Verified Target Artifact Inventory

Later slices must use these verified paths as candidate targets. If a slice
needs a different target artifact, it must verify the path before editing and
stop if the target cannot be found.

| Area | Verified artifacts |
|---|---|
| Root rules | `AGENTS.md`, `QUALITY.md` |
| Active workflow | `docs/workflow/workflow.md`, `docs/workflow/execution-summary.md`, `docs/workflow/slice-dependency-map.md`, `docs/workflow/governance-conflict-review.md`, `docs/workflow/governance-inventory.md` |
| Process governance | `docs/process/README.md`, `docs/process/workflow-create.md`, `docs/process/workflow-execute.md`, `docs/process/branch-governance.md`, `docs/process/push-auto.md`, `docs/process/skills-update.md`, `docs/process/skill-agent-creation.md`, `docs/process/three-amigos-requirement-gate.md` |
| Agent governance | `docs/agents/README.md`, `docs/agents/agent-governance.md`, `docs/agents/organigramm.md`, `docs/agents/skill-registry.md` |
| Governance docs | `docs/governance/README.md`, `docs/governance/contract-governance.md` |
| Architecture docs | `docs/architecture/`, `docs/arc42/` |
| ADR docs | `docs/adr/README.md`, `docs/adr/ADR-*.md` |
| EPIC docs | `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md` |
| Skill audit docs | `docs/skill-audit/` |
| Contract reference docs | `docs/contracts/` |
| Project orchestrator | `.agents/orchestrator/routing-rules.md`, `.agents/orchestrator/swarm-orchestrator.md` |
| Project prompts | `.agents/prompts/` |
| Project roles | `.agents/roles/` |
| Project skills | `.agents/skills/` |
| Codex agents | `.codex/agents/` |
| Codex skills | `.codex/skills/` |
| Codex subagents | `.codex/subagents/` |
| Codex workflow rules | `.codex/workflow/` |

## Quality Commands From QUALITY.md

`QUALITY.md` is the authoritative quality source.

Minimum command for implementation, build, plugin, adapter, runtime, contract
or test changes:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate when required and practical:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Documentation-only governance slices use the narrow workflow gate unless a
slice changes product code, build logic, plugin metadata, tests, contracts,
runtime behavior or implementation files:

```bash
git status --short
git diff --check
git diff --cached --check
```

`validatePlugins` is required only when Gradle plugin metadata, task inputs,
task outputs or plugin implementation classes change.

## Existing Governance Capabilities

- Three process strands are documented: `skills-agents`, `workflow create`, `workflow execute`.
- `workflow create` branch-first behavior is documented.
- `workflow execute` slice checkpoint push is documented.
- `push auto` is restricted to `skills-agents`.
- Documentation Governance is already inside active strands, not a fourth strand.
- arc42 and ADR documentation already contain agent governance sections.

## Introduced V2 Labels

These labels are introduced by the Governance Flowchart V2 workflow and must be added or mapped by execution slices:

| Label | Meaning | Current status |
|---|---|---|
| S1 | Existing `skills-agents` strand | Introduced by workflow, mapped to existing strand |
| S2 | Existing `workflow create` strand | Introduced by workflow, mapped to existing strand |
| S3 | Existing `workflow execute` strand | Introduced by workflow, mapped to existing strand |
| S3D | Execution-orchestration node inside S3 | Mapped by Slice 06 to workflow-execute, Senior Swarm Orchestrator, dependency graph and conflict-lock governance |
| `S3_STATUS` | Working-tree preflight node | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S3_BRANCH` | Execution-branch preflight node | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S3_SCOPE` | Workflow-scope preflight node | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S3_CLASSIFY` | Slice classification node | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S3_UNCLASSIFIED` | Unclassifiable-slice stop and escalation node | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `ARCH_VIOLATION` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| `BUILD_FAILURE` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| `TEST_FAILURE` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| `DOC_GOVERNANCE_FAILURE` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| `LOCK_CONFLICT` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| `UNKNOWN_FAILURE` | Typed Error Router category | Mapped by Slice 05 in workflow-execute, routing rules and quality-gate governance |
| CP | Commit, checkpoint and rollback subgraph | Mapped by Slice 08 in workflow-execute, branch and agent governance |
| `CP_ROLLBACK` | Rollback and revert decision node | Mapped by Slice 08 as explicit rollback decision with safe options |
| `CP_FINAL` | Post-checkpoint continuation node | Mapped by Slice 08 with outgoing `CMD_PUSH`, `RELEASE` and `Q11` paths |
| PUB | Publication-mode subgraph | Mapped by Slice 07 in process and agent publication governance |
| `PUB_PR_RESULT` | PR-open terminal for normal push path | Mapped by Slice 07 as normal `push` outcome without automatic merge |
| `PUB_DONE` | Publication completed terminal | Mapped by Slice 07 as verified checkpoint or merge completion |
| `PUB_PUSH_FAILED` | Push failure terminal | Mapped by Slice 07 to `CP_ROLLBACK` handoff or Root Architect escalation |
| `PUB_REJECTED` | Publication rejected terminal | Mapped by Slice 07 as governance, scope, branch or guard rejection |
| R10 | No automatic backward jump from S3 to S2 | Introduced by workflow, not yet implemented outside `docs/workflow` |
| R11 | One slice, one commit | Mapped by Slice 09 in process, prompt and commit-governance rules |
| `CP_RECORD` | Slice traceability record | Mapped by Slice 09 with workflow version, changed files, quality evidence, commit hash, rollback reference and documentation update status fields |
| workflow history | Workflow-version and slice-history artifact | Mapped by Slice 09 in `docs/workflow/workflow.history.md` |
| D8 | Blocking quality and release-readiness gate | Mapped by Slice 10 as the synchronous gate before commit, checkpoint push and release readiness |
| Q11 | Async execution report path | Mapped by Slice 10 as non-blocking by default after `CP_FINAL`, except explicitly declared regulatory reporting gates |
| `S1_PUSH_ELIGIBILITY_GUARD` | Skills-agents push eligibility guard | Mapped by Slice 11 as the active guard for skills-agents push eligibility |
| `PUB_PR_MERGE_GUARD` | Publication PR merge guard | Mapped by Slice 11 as the active guard for PR merge, open, blocked or rejected decisions |
| DOCROOT | Global documentation governance | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S1_DOC` | Local skills-agents documentation step | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S2_DOC` | Local workflow-create documentation step | Introduced by workflow, not yet implemented outside `docs/workflow` |
| `S3_DOC` | Local workflow-execute documentation step | Introduced by workflow, not yet implemented outside `docs/workflow` |
| Level 1 diagram | Governance overview diagram | Introduced by workflow, not yet implemented outside `docs/workflow` |
| Level 2 diagrams | Detailed subgraph diagrams | Introduced by workflow, not yet implemented outside `docs/workflow` |

## Risks

| Risk | Mitigation in workflow |
|---|---|
| V2 labels are mistaken for new process strands | Explicit mapping to existing strands. |
| Automatic retry loops become unbounded | `maxRetries = 3` with Root Architect escalation. |
| S3 preflight checks silently continue on failure | Explicit STOP paths for status, branch and scope checks. |
| Quality failures route to generic retry | Typed Error Router with owner roles. |
| Parallel agents edit the same artifact | S3D conflict locks for files, contracts and architecture boundaries. |
| Rollback is interpreted as destructive reset | `CP_ROLLBACK` is a decision node with safe options and escalation. |
| Documentation nodes overlap | `DOCROOT` is separated from `S1_DOC`, `S2_DOC` and `S3_DOC`. |
| Workflow-create artifacts collide with `push auto` | Branches containing `docs/workflow/**` use normal `push` or workflow-execute slice checkpoint push, not `push auto`. |
| Later slices infer target files from descriptive names | Slice 00 records concrete candidate target paths; later slices must stop if a target artifact cannot be verified. |

## Open Governance Gaps For Slice 14

- Dedicated Root Architect role file is not present.
- Dedicated Flowchart Integrity Audit skill is not present.
- Typed Error Router is mapped into workflow-execute and quality-gate governance; a dedicated standalone skill remains a Slice 14 linkage decision.
- Conflict Locking is mapped into S3D orchestration; a dedicated standalone skill remains a Slice 14 linkage decision.

These gaps are documented and are not blockers for workflow creation because the user request defines the target semantics and Slice 14 owns the linkage decision.
