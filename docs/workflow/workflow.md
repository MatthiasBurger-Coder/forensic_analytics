# Skill Landscape Expansion Workflow

## Status

Planned workflow. This document converts the requested skill-landscape workflow draft into an active repository workflow under `docs/workflow`.

This workflow is documentation and execution planning only. It does not create the listed skills, ADRs, prompts or matrices until `workflow execute` is requested and the configured subagent or role-review process approves each slice.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Authoritative agent rules: `AGENTS.md`
- Authoritative quality rules: `QUALITY.md`
- Workflow execution rules: `.codex/workflow/workflow-execution-rules.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Existing planning material: `docs/workplan/**`
- New active workflow location: `docs/workflow/**`

## Target Outcome

Build a governed skill and agent landscape that can intake new requirements, validate them with a Three Amigos gate, audit skill conflicts, generate workflows, execute slices through subagents or role reviews, run quality gates, document ADRs and enforce commit or push governance.

The target processing model is:

```text
New requirement
  -> Requirement Gate
  -> Skill Audit
  -> Conflict Check
  -> Workflow
  -> Slices
  -> Subagents or role reviews
  -> Handoffs
  -> Quality Gates
  -> ADRs
  -> Commit / Push when allowed
```

## Non-Goals

- Do not implement runtime business functionality.
- Do not create service code or change production Java modules.
- Do not migrate existing `docs/workplan/**` files unless a later slice explicitly owns that archival or migration.
- Do not bypass `AGENTS.md`, `QUALITY.md`, existing ADRs, hexagonal boundaries or evidence-integrity rules.
- Do not introduce shared Java implementation modules between future microservices.
- Do not commit or push unless a later workflow execution slice explicitly allows it and all required quality gates are clean. Only optional, unavailable or not-applicable checks may be documented as non-blocking.

## Target Skill Landscape

```text
Agent Workflow Orchestrator
|
+-- Senior System Architect
|   +-- Skill Registry & Conflict Auditor
|   +-- Three Amigos Requirement Gatekeeper
|   +-- Requirement Analyst
|   +-- Architecture Validator
|   +-- Quality Validator
|   +-- Dependency / Deadlock Validator
|   +-- Contract-First API Steward
|   +-- REST Contract Validator
|   +-- gRPC / Protobuf Validator
|   +-- Compatibility Validator
|   +-- Data Ownership & Persistence Steward
|   +-- Relational Store Validator
|   +-- Graph Store Validator
|   +-- Event Store Validator
|   +-- Vector Store Validator
|   +-- Security & Threat Modeling Skill
|   +-- Secure Coding Reviewer
|   +-- API Security Reviewer
|   +-- Container Security Reviewer
|   +-- Supply Chain Security Reviewer
|   +-- Observability & Runtime Diagnostics Skill
|   +-- Logging Validator
|   +-- Metrics Validator
|   +-- Trace Context Validator
|   +-- Runtime Diagnostics Validator
+-- ADR Steward
+-- Workflow Executor
|   +-- Agent Handoff Protocol Skill
|   +-- Quality Gate Orchestrator
|   +-- Release & Branch Governance Skill
+-- Senior Java Backend Engineer
+-- Senior React Frontend Engineer
+-- Senior UX Designer
+-- Senior DevOps Engineer
+-- Senior Tester
+-- Senior Swarm Orchestrator
```

## Governance Principles

- No skill expansion without defined responsibility, forbidden scope, inputs, outputs, collaboration rules and STOP rules.
- No new requirement may be converted into execution slices before the Three Amigos Requirement Gatekeeper returns `READY_FOR_WORKFLOW`.
- No parallel agent work is allowed without explicit input artifacts, output artifacts, owner, dependencies and handoff status.
- No microservice rule may permit shared Java implementation modules, shared technical utility modules as coupling points, cross-service database access or implicit DTO sharing.
- All planned outputs must preserve evidence-first forensic semantics and keep unknown, incomplete or unresolved facts explicit.

## Repository Target Structure

The workflow plans these additions or updates:

```text
.agents/
+-- skills/
    +-- skill-registry-conflict-auditor/
    +-- three-amigos-requirement-gatekeeper/
    +-- agent-handoff-protocol/
    +-- contract-first-api-steward/
    +-- data-ownership-persistence-steward/
    +-- quality-gate-orchestrator/
    +-- adr-steward/
    +-- security-threat-modeling/
    +-- observability-runtime-diagnostics/
    +-- release-branch-governance/

.agents/
+-- prompts/
|   +-- workflow-execute.md
|   +-- skill-audit.md
|   +-- requirement-intake.md
|   +-- slice-execute.md

docs/
+-- workflow/
|   +-- workflow.md
|   +-- prompts/
|   +-- skill-landscape-inventory.md
|   +-- skill-conflict-matrix.md
|   +-- agent-handoff-matrix.md
|   +-- deadlock-prevention-rules.md
+-- adr/
```

## Quality Gates

Minimum verification from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate from `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Every slice must run the narrowest relevant local check first. Documentation-only slices add at least:

```bash
git diff --check
```

This does not replace `QUALITY.md`. The documented Gradle minimum gate remains required before claiming commit-ready or fully clean workflow status. Run the full local quality gate before commit readiness, or document why the gate could not be executed. Failed required gates are always blocking.

## Slice Dependency Order

```text
00 -> 01 -> 02 -> 03 -> 04
04 -> 05
04 -> 06
04 -> 07
04 -> 08
04 -> 09
04 -> 10
04 -> 11
05,06,07,08,09,10,11 -> 12 -> 13 -> 14 -> 15 -> 16
```

Slices 00 through 04 must run sequentially. Slices 05 through 11 may run in parallel only after the Agent Handoff Protocol is active and write scopes are disjoint. Slices 12 through 16 run sequentially.

## Role Ownership Map

| Slice | Primary owner | Review roles |
| --- | --- | --- |
| 00 | Senior Workflow Architect | Senior System Architect, Senior Tester, Repository Explorer |
| 01 | Senior System Architect | Senior Swarm Orchestrator, Senior Documentation Engineer |
| 02 | Senior Workflow Architect | Senior System Architect, Senior Tester |
| 03 | Three Amigos Requirement Gatekeeper | Senior System Architect, Senior Tester, Skill Registry & Conflict Auditor |
| 04 | Senior Swarm Orchestrator | Workflow Executor, Senior Tester |
| 05 | Senior gRPC/Proto Specialist | Senior Java Backend Engineer, Senior System Architect, Senior Tester |
| 06 | Senior Analysis Storage Architect | Senior System Architect, Senior Java Backend Engineer, Senior Security/Sandbox Engineer |
| 07 | Senior Tester | Senior DevOps Engineer, Senior System Architect |
| 08 | Senior System Architect | Senior Documentation Engineer, Skill Registry & Conflict Auditor |
| 09 | Senior Security/Sandbox Engineer | Senior DevOps Engineer, Senior Java Backend Engineer, Contract-First API Steward |
| 10 | Senior DevOps Engineer | Senior Java Backend Engineer, Data Ownership & Persistence Steward |
| 11 | Senior Workflow Architect | Workflow Executor, Quality Gate Orchestrator, Senior DevOps Engineer |
| 12 | Senior Swarm Orchestrator | Workflow Executor, Skill Registry & Conflict Auditor, Senior System Architect |
| 13 | Skill Registry & Conflict Auditor | Agent Handoff Protocol, Three Amigos Requirement Gatekeeper, Senior System Architect |
| 14 | ADR Steward | Senior System Architect, Skill Registry & Conflict Auditor |
| 15 | Three Amigos Requirement Gatekeeper | Skill Registry & Conflict Auditor, Workflow Executor, Contract-First API Steward, Data Ownership & Persistence Steward, Quality Gate Orchestrator |
| 16 | Quality Gate Orchestrator | Release & Branch Governance, Senior System Architect, Senior Tester |

Callable subagents may be used only when the active `workflow execute` command authorizes delegated execution. Otherwise, use the corresponding role files as local review checklists and report that limitation. Until a target governance skill exists, route its bootstrap review to the closest verified existing role shown in this table.

## Slice 00 - Repository And Skill Inventory

### Purpose

Inventory the existing agent, skill, prompt, workflow, ADR and governance structure before changing it.

### Affected Files

- `AGENTS.md`
- `.agents/**`
- `.codex/**`
- `docs/workplan/**`
- `docs/workflow/**`
- `docs/adr/**`
- `QUALITY.md`

### Output

- `docs/workflow/skill-landscape-inventory.md`

### Verification

- No implementation before inventory is complete.
- Existing rules are documented, not overwritten.
- Potential conflicts and duplications are visible.

### Stop Conditions

- Multiple active workflows conflict and no owner can be identified.
- Existing governance rules contradict each other in a blocking way.
- The active skill or role inventory cannot be verified from repository files.

## Slice 01 - Target Organization And Agent Hierarchy

### Purpose

Place the target skill and agent hierarchy into the authoritative agent documentation or a verified agent overview.

### Affected Files

- `AGENTS.md`
- Optional: `docs/agents/organigram.md`

### Required Rules

- Agent Workflow Orchestrator stays above execution governance, and Senior System Architect owns architecture governance above specialist architecture skills.
- Workflow Executor executes slices and does not make sole domain, architecture, security or quality decisions.
- Three Amigos Requirement Gatekeeper is the requirement intake gate.
- Skill Registry & Conflict Auditor is the governance control for skill overlaps.

### Stop Conditions

- A role is both sole reviewer and sole implementer of the same decision.
- The hierarchy conflicts with existing root `AGENTS.md` rules.

## Slice 02 - Skill Registry And Conflict Auditor

### Purpose

Create a meta-skill that inventories skills, detects conflicts and evaluates compatibility.

### Affected Files

- `.agents/skills/skill-registry-conflict-auditor/SKILL.md`
- `.agents/skills/skill-registry-conflict-auditor/workflow.md`
- `.agents/skills/skill-registry-conflict-auditor/decision-rules.md`
- `.agents/skills/skill-registry-conflict-auditor/conflict-rules.md`
- `.agents/skills/skill-registry-conflict-auditor/templates/skill-registry-entry-template.md`

### Required Content

- Mission
- Responsibilities
- Authority
- Forbidden
- Inputs
- Outputs
- Collaboration Rules
- STOP rules

### Conflict Types

- Architecture Conflict
- Ownership Conflict
- Quality Conflict
- Security Conflict
- Workflow Conflict
- Tooling Conflict
- Microservice Boundary Conflict
- Data Ownership Conflict

### Stop Conditions

- A skill permits shared implementation modules for microservices.
- A skill permits direct cross-service database access.
- A skill permits implementation without requirement gate approval.
- A skill permits commit without quality gate approval.
- Multiple skills own the same output but apply incompatible rules.

## Slice 03 - Three Amigos Requirement Gatekeeper

### Purpose

Finalize the Three Amigos gate as the mandatory requirement intake checkpoint before workflow authoring.

### Affected Files

- `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/workflow.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/decision-rules.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/anti-patterns.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/templates/requirement-template.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/templates/slice-template.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/templates/acceptance-template.md`

### Required Phases

1. Requirement Intake
2. Business Goal Validation
3. Architecture Fit Validation
4. Quality/Testability Validation
5. Dependency/Deadlock Validation
6. Skill Requirement Validation
7. `READY_FOR_WORKFLOW` or `REQUIRES_REFINEMENT`

### Stop Conditions

- Business goal is missing.
- Non-goals are missing.
- Affected services are unclear.
- Acceptance criteria are missing.
- API contracts are unclear.
- Testability cannot be demonstrated.
- Data ownership is unclear.
- Rollback strategy is missing.

## Slice 04 - Agent Handoff Protocol

### Purpose

Define the mandatory handoff protocol for parallel or sequential agent work.

### Affected Files

- `.agents/skills/agent-handoff-protocol/SKILL.md`
- `.agents/skills/agent-handoff-protocol/workflow.md`
- `.agents/skills/agent-handoff-protocol/handoff-contract.md`
- `.agents/skills/agent-handoff-protocol/status-model.md`
- `.agents/skills/agent-handoff-protocol/templates/handoff-report-template.md`

### Status Model

```text
NOT_STARTED
READY
IN_PROGRESS
BLOCKED
READY_FOR_REVIEW
CHANGES_REQUESTED
APPROVED
DONE
```

### Required Handoff Fields

- `source_agent`
- `target_agent`
- `slice_id`
- `input_artifacts`
- `output_artifacts`
- `assumptions`
- `known_risks`
- `blockers`
- `validation_status`
- `next_action`

### Stop Conditions

- Target agent is undefined.
- Input artifacts are missing.
- Output expectations are unclear.
- Blockers are not classified.
- Parallel work would modify the same files without ownership rules.

## Slice 05 - Contract-First API Steward

### Purpose

Create a skill for REST and gRPC/protobuf contracts that prevents microservice coupling through shared Java DTO modules.

### Affected Files

- `.agents/skills/contract-first-api-steward/SKILL.md`
- `.agents/skills/contract-first-api-steward/workflow.md`
- `.agents/skills/contract-first-api-steward/rest-rules.md`
- `.agents/skills/contract-first-api-steward/grpc-protobuf-rules.md`
- `.agents/skills/contract-first-api-steward/compatibility-rules.md`
- `.agents/skills/contract-first-api-steward/templates/api-contract-template.md`
- `.agents/skills/contract-first-api-steward/templates/compatibility-report-template.md`

### Required Rules

- API contract before implementation.
- No implicit DTO shared modules.
- Version protobuf contracts.
- Standardize REST error models.
- Document breaking changes explicitly.
- Identify API consumers.

### Stop Conditions

- Service communication is introduced without a contract.
- DTOs are shared through common implementation modules.
- A breaking change has no ADR.
- gRPC message semantics are unclear.
- Error model is missing.

## Slice 06 - Data Ownership And Persistence Steward

### Purpose

Document data ownership, persistence decisions and service-to-service data flow rules.

### Affected Files

- `.agents/skills/data-ownership-persistence-steward/SKILL.md`
- `.agents/skills/data-ownership-persistence-steward/workflow.md`
- `.agents/skills/data-ownership-persistence-steward/ownership-rules.md`
- `.agents/skills/data-ownership-persistence-steward/persistence-decision-matrix.md`
- `.agents/skills/data-ownership-persistence-steward/templates/data-ownership-report-template.md`

### Persistence Areas

- Relational Store
- Graph Store
- Event Store
- Vector Store
- File/Object Store
- Runtime Trace Store

### Stop Conditions

- Data ownership is unclear.
- Multiple services write the same owned data.
- A service directly accesses another service's database.
- Graph, event or vector storage is used without a reasoned decision.
- Data protection or security impact is unresolved.

## Slice 07 - Quality Gate Orchestrator

### Purpose

Create a skill that binds slice execution, commit readiness and failure handling to `QUALITY.md`.

### Affected Files

- `.agents/skills/quality-gate-orchestrator/SKILL.md`
- `.agents/skills/quality-gate-orchestrator/workflow.md`
- `.agents/skills/quality-gate-orchestrator/quality-gates.md`
- `.agents/skills/quality-gate-orchestrator/failure-handling.md`
- `.agents/skills/quality-gate-orchestrator/templates/quality-result-template.md`

### Quality Dimensions

- Build
- Unit Tests
- Integration Tests
- Contract Tests
- ArchUnit
- Coverage
- Sonar
- Dependency Verification
- Docker Build
- Security Checks
- Documentation Completeness

### Stop Conditions

- Build fails.
- Tests fail.
- Architecture rules fail.
- Commit is planned despite a required red gate.
- Quality gate cannot be executed and no exception report exists.

## Slice 08 - ADR Steward

### Purpose

Create a skill and template for documenting architecture decisions and maintaining an ADR backlog.

### Affected Files

- `.agents/skills/adr-steward/SKILL.md`
- `.agents/skills/adr-steward/workflow.md`
- `.agents/skills/adr-steward/adr-rules.md`
- `.agents/skills/adr-steward/templates/adr-template.md`
- `docs/adr/**`

### Initial ADR Backlog

- `docs/adr/ADR-0009-no-shared-common-modules.md`
- `docs/adr/ADR-0010-contract-first-rest-and-grpc.md`
- `docs/adr/ADR-0011-three-amigos-before-workflow.md`
- `docs/adr/ADR-0012-quality-gates-before-commit.md`
- `docs/adr/ADR-0013-data-ownership-per-service.md`
- `docs/adr/ADR-0014-agent-handoff-protocol.md`
- `docs/adr/ADR-0015-skill-registry-conflict-auditing.md`

### Stop Conditions

- Architecture decision is introduced without ADR coverage.
- Existing ADR is contradicted.
- ADR is overwritten silently.
- Rationale is missing.

## Slice 09 - Security And Threat Modeling

### Purpose

Create a security skill for API, gRPC, container, supply-chain and repository-processing risks.

### Affected Files

- `.agents/skills/security-threat-modeling/SKILL.md`
- `.agents/skills/security-threat-modeling/workflow.md`
- `.agents/skills/security-threat-modeling/threat-model-rules.md`
- `.agents/skills/security-threat-modeling/secure-coding-rules.md`
- `.agents/skills/security-threat-modeling/supply-chain-rules.md`
- `.agents/skills/security-threat-modeling/templates/threat-model-template.md`

### Review Fields

- API Security
- gRPC Security
- Authentication / Authorization
- Secrets Handling
- Logging Safety
- Container Security
- Dependency / Supply Chain Risk
- Repository Processing Risk
- Runtime Trace Data Risk

### Stop Conditions

- Secrets could be logged.
- REST or gRPC is introduced without a security review.
- Containers would run with unnecessary privileges.
- External repositories are processed without isolation.
- Supply-chain risks are ignored.

## Slice 10 - Observability And Runtime Diagnostics

### Purpose

Create a skill for trace context, structured logging, metrics and runtime diagnostics.

### Affected Files

- `.agents/skills/observability-runtime-diagnostics/SKILL.md`
- `.agents/skills/observability-runtime-diagnostics/workflow.md`
- `.agents/skills/observability-runtime-diagnostics/trace-context-rules.md`
- `.agents/skills/observability-runtime-diagnostics/logging-rules.md`
- `.agents/skills/observability-runtime-diagnostics/metrics-rules.md`
- `.agents/skills/observability-runtime-diagnostics/templates/observability-check-template.md`

### Required Trace Terms

- `correlationId`
- `traceId`
- `spanId`
- `parentSpanId`
- `analysisRunId`
- `runtimeSessionId`
- `incidentId`
- `serviceName`
- `workerName`
- `stepName`
- `phase`

### Stop Conditions

- Service communication has no correlation concept.
- Errors cannot be assigned to an analysis or runtime context.
- Logs could contain sensitive data.
- Runtime events have no stable identity.

## Slice 11 - Release And Branch Governance

### Purpose

Create a skill for branch, commit, push and rollback readiness.

### Affected Files

- `.agents/skills/release-branch-governance/SKILL.md`
- `.agents/skills/release-branch-governance/workflow.md`
- `.agents/skills/release-branch-governance/branch-rules.md`
- `.agents/skills/release-branch-governance/commit-rules.md`
- `.agents/skills/release-branch-governance/push-rules.md`
- `.agents/skills/release-branch-governance/templates/release-readiness-template.md`

### Stop Conditions

- Commit is planned without quality gate approval.
- Commit message is incomplete.
- Branch context is unclear.
- Push is planned despite unresolved failures.

## Slice 12 - Prompt And Workflow Integration

### Purpose

Link the new skills into project prompts and workflow execution rules so the landscape is usable, not only documented.

### Affected Files

- `.agents/prompts/workflow-execute.md`
- `.agents/prompts/skill-audit.md`
- `.agents/prompts/requirement-intake.md`
- `.agents/prompts/slice-execute.md`
- `docs/workflow/prompts/skill-landscape-expansion.md`

### Required Logic

1. Load `AGENTS.md`.
2. Load Skill Registry.
3. Run Requirement Gate if the requirement is new.
4. Run Skill Conflict Audit.
5. Build Slice Plan.
6. Assign subagents or role reviews.
7. Use Handoff Protocol.
8. Run Quality Gates.
9. Produce Summary.
10. Commit or push only if allowed.

### Stop Conditions

- Skill Registry is not read.
- Gatekeeper is skipped.
- Workflow is created without role assignment.
- Quality Gate is not included.
- Handoff rules are missing.
- Prompt integration would put project-specific rules into portable `.codex` files without a reusable-template justification.

## Slice 13 - Conflict Matrix And Deadlock Prevention

### Purpose

Create central matrices showing which skills review, block, support or hand off to each other.

### Affected Files

- `docs/workflow/skill-conflict-matrix.md`
- `docs/workflow/agent-handoff-matrix.md`
- `docs/workflow/deadlock-prevention-rules.md`

### Deadlock Rules

- Each slice has exactly one owner agent.
- Reviewer agents may block, but must provide concrete resolution steps.
- No agent may wait for an artifact without knowing its owner.
- No cyclic handoff chain is allowed without orchestrator decision.
- Blockers must be classified as `REQUIRES_INPUT`, `REQUIRES_DECISION`, `REQUIRES_FIX` or `REQUIRES_ARCHITECTURE_DECISION`.

## Slice 14 - Initial ADRs

### Purpose

Create initial ADRs for the main architecture and process decisions behind the skill landscape.

### Affected Files

- `docs/adr/ADR-0009-no-shared-common-modules.md`
- `docs/adr/ADR-0010-contract-first-rest-and-grpc.md`
- `docs/adr/ADR-0011-three-amigos-before-workflow.md`
- `docs/adr/ADR-0012-quality-gates-before-commit.md`
- `docs/adr/ADR-0013-data-ownership-per-service.md`
- `docs/adr/ADR-0014-agent-handoff-protocol.md`
- `docs/adr/ADR-0015-skill-registry-conflict-auditing.md`

### Done Criteria

- Decisions are documented.
- Rationale is present.
- Consequences are explicit.
- ADRs do not contradict existing project rules.

## Slice 15 - Example Requirement Validation

### Purpose

Validate the skill landscape with a simulated new requirement.

### Example Requirement

```text
Implement a new gRPC ingestion service that receives analysis events from the plugin and forwards them to an Analysis Store.
```

### Expected Flow

1. Requirement Gatekeeper normalizes the requirement.
2. Skill Registry identifies required skills.
3. Contract-First API Steward reviews the gRPC contract.
4. Data Ownership Steward reviews ownership.
5. Observability Skill reviews correlation and trace context.
6. Security Skill reviews gRPC and upload risks.
7. Quality Gate Orchestrator defines tests.
8. Workflow Executor creates a slice plan.

### Expected Result

`READY_FOR_WORKFLOW` or a documented blocker report.

## Slice 16 - Final Review, Quality Gate And Commit Preparation

### Purpose

Review all changed workflow, skill, prompt, matrix and ADR artifacts before versioning.

### Tasks

1. Validate Markdown structure.
2. Validate links and references.
3. Check skill directories for required files.
4. Re-run conflict checks.
5. Run `QUALITY.md` gates when applicable.
6. Produce a summary report.
7. Prepare a commit message.
8. Commit only when allowed by workflow governance.
9. Push only when allowed by workflow governance.

### Commit Message Requirements

- Summary
- Why
- What changed
- Added skills
- Updated governance
- Updated Codex prompts
- Validation performed
- Risks / follow-ups

## Blocking Errors

- Skill without Mission.
- Skill without STOP rules.
- Skill without clear responsibility.
- Contradictory microservice rule.
- Workflow Executor without Gatekeeper requirement.
- Commit rules without Quality Gate.
- API Skill allows shared DTO module.
- Data Skill allows cross-service database access.

## Non-Blocking Notes

- Missing examples.
- Incomplete future extension ideas.
- Optional tooling notes.
- Open detail questions for concrete implementation projects.

## Whole Workflow Definition Of Done

- All new skills are created.
- All skills are placed in the hierarchy.
- `AGENTS.md` is updated.
- Codex prompts use the governance chain.
- Skill Registry & Conflict Auditor exists.
- Three Amigos Requirement Gatekeeper is mandatory.
- Agent Handoff Protocol exists.
- Contract-First API Steward exists.
- Data Ownership Steward exists.
- Quality Gate Orchestrator exists.
- ADR Steward exists.
- Security & Threat Modeling Skill exists.
- Observability & Runtime Diagnostics Skill exists.
- Release & Branch Governance Skill exists.
- Conflict matrix exists.
- Deadlock-prevention rules exist.
- Initial ADRs exist.
- Example requirement validates through the process.
- Quality gate is executed or a documented exception exists.
- Commit and push follow governance.
