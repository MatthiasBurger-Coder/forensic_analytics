# AGENTS.md — Forensic Analytics

## Purpose

This document defines the mandatory engineering rules for automated agents working on the **Forensic Analytics** repository.

The repository contains the Forensic Analytics platform: a forensic analysis system for ingesting static and runtime evidence, building analyzable execution and dependency models, supporting exception-centered replay, and preparing structured findings for human and LLM-assisted diagnosis.

Agents must follow this document before modifying code, tests, build logic, examples, documentation, analysis workflows, runtime evidence importers, graph persistence, replay logic, or LLM integration code.

## Project Baseline

The project baseline is:

- Java 25 unless the repository build explicitly defines a stricter compatible baseline
- Gradle via the project wrapper
- JUnit 6
- ArchUnit where architecture rules are available
- Java 25-compatible JaCoCo where coverage verification is configured
- SonarQube / SonarCloud related checks where configured
- Hexagonal architecture
- Evidence-first forensic analysis
- Deterministic analysis output
- IF-Less development preference
- Declarative programming preference
- Regression-first workflow

All source code, comments, JavaDoc, test names, and repository documentation must be written in English.

## Command Execution Environment

Agents must execute repository commands in the environment that matches the host operating system:

- On Windows hosts, use WSL from the repository's WSL-mounted worktree path and run Linux-style commands, including `./gradlew`.
- On Linux hosts, use native shell access and run Linux-style commands directly, including `./gradlew`.
- Before staging or committing from a Windows-hosted WSL worktree, verify that Git status is not polluted by line-ending-only changes. If broad unexpected line-ending changes appear, correct the local Git EOL configuration or stop and report before proceeding.
- If WSL is unavailable on a Windows host, or if the repository cannot be accessed from WSL, stop and report the blocker before substituting Windows-native command execution.

If a baseline, command, or quality rule in this document differs from `AGENTS.md`, `QUALITY.md`, `settings.gradle(.kts)`, `build.gradle(.kts)`, or CI workflow files in the repository, the repository files must be inspected and the conflict must be reported before continuing.

## Mandatory Subagent Workflow

All non-trivial repository work must be routed through the configured subagent workflow.

The Agent Workflow Orchestrator is responsible for:

- reading workflows,
- detecting slices,
- assigning subagents or role reviews,
- enforcing architecture rules,
- enforcing quality gates,
- coordinating execution,
- preserving stop-rule behavior,
- collecting and reporting results.

Direct implementation of non-trivial work without subagent or role review is forbidden.

Use these sources for workflow routing:

1. `.codex/AGENTS.md`
2. `.codex/workflow/workflow-execution-rules.md`
3. `.agents/orchestrator/routing-rules.md`
4. `.agents/orchestrator/swarm-orchestrator.md`
5. relevant `.agents/roles` and `.agents/skills`

Callable subagents should be used when the active request or workflow command authorizes delegated execution. If callable subagents are unavailable in the current runtime, the corresponding role file must be used as an explicit review checklist and the limitation must be reported.

## Workflow Governance Hierarchy

The Agent Workflow Orchestrator owns workflow execution governance.

```text
Agent Workflow Orchestrator
|
+-- Workflow Executor
|
+-- Senior System Architect
|   +-- Skill Registry & Conflict Auditor
|   +-- Three Amigos Requirement Gatekeeper
|   +-- Contract-First API Steward
|   +-- Data Ownership & Persistence Steward
|   +-- Security & Threat Modeling
|   +-- Observability & Runtime Diagnostics
|   +-- ADR Steward
|
+-- Senior Swarm Orchestrator
+-- Senior Java Backend Engineer
+-- Senior React Frontend Engineer
+-- Senior UX Designer
+-- Senior DevOps Engineer
+-- Senior Tester
```

The Workflow Executor executes approved slices. It must not be the sole authority for requirement, architecture, security, data ownership, quality, ADR or release decisions.

The Senior System Architect owns architecture governance and may block architecture-sensitive workflows. The Three Amigos Requirement Gatekeeper is the intake gate for new or changed requirements. The Skill Registry & Conflict Auditor is the governance control for skill overlap, conflicting ownership and incompatible workflow rules.

## Mandatory Process Strands

Repository agent work is organized into exactly three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

The strands must not be mixed. Shared governance roles such as Senior System Architect, Documentation Governance, Skill Registry Maintainer, Organigramm Maintainer, Process Governance Maintainer and Push Auto Guard execute inside the active strand and must apply that strand's file scope, quality gate and documentation duty.

The `skills-agents` strand is the only strand that may use `push auto`. `push auto` must not publish backend, frontend, Docker/runtime, gRPC, REST, persistence, analysis-engine, Joern, JavaParser, BTM generator or product implementation changes.

The process model, organigramm and registry are documented in:

1. `docs/process/README.md`
2. `docs/process/skill-agent-creation.md`
3. `docs/process/workflow-create.md`
4. `docs/process/workflow-execute.md`
5. `docs/process/push-auto.md`
6. `docs/agents/organigramm.md`
7. `docs/agents/skill-registry.md`

## Mandatory Workflow Creation Command

When the user writes exactly:

```text
workflow create
```

Codex must ensure a dedicated Git branch for the new workflow exists and is active before creating or modifying workflow planning artifacts.

`workflow create` is a requirements, architecture, planning and documentation strand. It must not implement product backend, frontend, Docker/runtime or analytics code.

The required checked outputs are:

1. a complete checked `docs/workflow/workflow.md`,
2. checked or updated arc42 documentation under `docs/arc42/**`.

Supporting sidecar files under `docs/workflow/**` are not completion criteria for new workflow creation. They may be archived or migrated only through an explicit workflow-governance task. The checked `docs/workflow/workflow.md` and checked arc42 review are mandatory.

Read-only verification, requirement intake, requirement clarification, routing-rule inspection, and role selection may occur before branch creation. Mutating workflow creation must not.

`workflow create` must run an explicit Requirement Clarification Loop before final workflow authoring. The loop records the original request, interpreted intent, change type, affected process strand, affected architecture area, explicit requirements, implicit requirements, assumptions, non-goals, risks, open questions, blocking questions, confidence level, and one decision:

```text
READY_FOR_WORKFLOW
PROCEED_WITH_ACCEPTED_ASSUMPTIONS
REQUIRES_REFINEMENT
```

If blocking questions remain open, Codex must not create a final checked `docs/workflow/workflow.md`, must not release the workflow for `workflow execute`, must ask focused clarification questions, and must return `REQUIRES_REFINEMENT`.

Non-blocking uncertainty may be documented as an accepted assumption only when it does not affect architecture boundaries, testability, data ownership, service boundaries, APIs, contracts, runtime behavior or scope.

Confidence decisions are governed as follows:

- Confidence >= 90%: `READY_FOR_WORKFLOW` when no blocking questions remain.
- Confidence 70-89%: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when every assumption is non-blocking and documented.
- Confidence < 70%: `REQUIRES_REFINEMENT`.

The required order is:

1. Verify the Git repository context with `git rev-parse --show-toplevel`.
2. Check the current working tree with `git status --short`.
3. Stop if the current branch is detached, unclear, or if unrelated or unclear uncommitted changes exist.
4. Run Requirement Intake.
5. Run the Requirement Clarification Loop until no blocking questions remain, or stop with `REQUIRES_REFINEMENT`.
6. Run the Three Amigos Requirement Gate with these mandatory roles:
   - Senior Requirement Engineer: goal, scope, non-goals, acceptance criteria, assumptions and open questions.
   - Senior System Architect: architecture boundaries, arc42, service boundaries, plugin-vs-analytics boundary and risks.
   - Senior Java Backend Developer: backend impact, ports, adapters, domain, JUnit 6 testability, Spring and microservice consequences.
   - Senior React Frontend Developer: frontend impact, UX flows, React components, state, API adapters and build/test consequences.
   - Senior Tester: testability, regression, quality gates, acceptance criteria and slice acceptance.
7. Generate a dedicated workflow branch name unless the current branch is already a matching branch for this workflow.
8. Check local and remote branch-name collisions, choosing the next clear unique suffix when needed.
9. Create and checkout the workflow branch, or verify the existing matching workflow branch before mutating workflow files.
10. Verify the active branch with `git branch --show-current`.
11. Create or sharpen `docs/workflow/workflow.md` only after the branch exists and is active.
12. Validate `docs/workflow/workflow.md`.
13. Check and update arc42 documentation when affected.
14. Validate arc42 documentation.
15. Run Documentation Governance.
16. Release the workflow for `workflow execute` only after both required outputs are checked and no blocking questions remain.

Default workflow branch names use:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
fix/workflow-<short-topic>-<yyyyMMdd>
docs/workflow-<short-topic>-<yyyyMMdd>
architecture/workflow-<short-topic>-<yyyyMMdd>
```

Use `feature/` unless the workflow is clearly a bugfix, documentation-only change, or architecture/agent-structure change.

Never create or modify workflow planning artifacts on `main`, `master`, `develop`, or any shared branch. If the branch cannot be created, checked out, or verified as active, stop and report:

```text
STOP: workflow create cannot continue safely.
Reason: <concrete reason>
No workflow files were created before resolving the branch isolation issue.
```

Subagents must never perform implementation work on `main`, `master`, `develop`, or any shared branch. Before modifying files, every subagent or role execution must verify that the active branch belongs to the current workflow. Subagents must not switch branches unless the workflow explicitly authorizes that branch operation.

If architecture boundaries or testability are unclear, return to the Three Amigos Requirement Gate instead of producing an executable workflow.

## Mandatory Workflow Execution Command

When the user writes exactly:

```text
workflow execute
```

Codex must not start ad-hoc implementation. The command is explicit authorization to execute the active workflow through the configured subagent workflow.

Execution order:

1. Load the checked workflow from `docs/workflow/workflow.md`.
2. Verify that `docs/workflow/workflow.md` records a checked arc42 review or update.
3. Load the checked or updated arc42 documentation from `docs/arc42/**`.
4. Verify the current workflow branch and local worktree state.
5. Read the complete workflow before implementation.
6. Identify all slices and their dependencies.
7. Use `.agents/skills/workflow-executor/SKILL.md` for the execution protocol.
8. Assign suitable subagents or roles for each slice through `.agents/orchestrator/routing-rules.md` and `.agents/orchestrator/swarm-orchestrator.md`.
9. Classify backend, frontend, Docker/runtime and documentation work into separate execution strands.
10. Execute one slice at a time.
11. After each slice:
    - run required tests
    - run required quality checks
    - inspect git diff
    - document the result
    - stage only files changed by the slice
    - run `git diff --cached --check`
    - create a slice-scoped checkpoint commit
    - push the current workflow branch to `origin`
    - record the commit SHA and push result in the execution report
    - continue with the next slice only after the checkpoint push succeeded
12. If a slice requires specialist review, spawn or route to the matching subagent or role.
13. Never bypass configured architecture, testing, DevOps, security, or microservice review for decisions in those areas.
14. Stop if assumptions about classes, modules, APIs, quality commands, or architecture are uncertain.
15. Do not use `push auto`, PR merge, branch cleanup or force-push for slice checkpoint pushes.

Slice checkpoint commit messages use:

```text
<type>(slice-<nn>): <short description>
```

Use `docs(slice-<nn>): ...` or `agent(slice-<nn>): ...` for governance and
documentation slices. Use `feat(slice-<nn>): ...`, `fix(slice-<nn>): ...` or
`test(slice-<nn>): ...` for implementation slices in later workflows.

If the machine crashes or the local worktree is lost, restore the last
successful state from `origin/<workflow-branch>`. The execution report must
show the latest completed slice, commit SHA and pushed branch state.

For `workflow execute`, no direct implementation of a slice is allowed before the relevant subagent or role has reviewed the slice, except read-only verification needed to route the slice.

For backend slices, require JUnit 6 test coverage or a checked workflow exception, hexagonal architecture review, and Microservice Senior Expert review when service autonomy or service boundaries are affected. For frontend slices, require Senior React Frontend Developer and Senior UX Designer review when a frontend module is affected. Documentation synchronization and arc42 consistency are part of the final definition of done.

## Core Principle

Prefer the smallest correct change.

Do not perform broad rewrites, speculative refactorings, unrelated cleanups, or architecture migrations unless the task explicitly requires them.

Every change must be traceable to:

1. the requested task,
2. an observed defect,
3. a verified architectural rule,
4. a quality-gate failure,
5. or a documented project decision.

## Forensic Engineering Principle

Forensic Analytics code must preserve trust in evidence.

Analysis results, replay outputs, graph relationships, findings, diagnostics, reports, and LLM prompts must be reproducible from explicit inputs. Agents must not hide uncertainty, invent evidence, infer missing runtime facts, or silently normalize away behavior-relevant data.

When the system cannot prove a fact from source code, runtime traces, stored events, repository metadata, or explicit user input, it must represent that fact as unknown, incomplete, unresolved, or not available.

## Mandatory Agent Safety Rules

These rules are mandatory for every change performed by an automated agent.

### STOP and Report Rule

If a required method, class, interface, task, package, file, configuration key, data model, graph label, storage table, API endpoint, analysis contract, replay contract, or documented behavior cannot be found exactly as expected, the agent must stop and report the mismatch.

The agent must not infer, guess, invent, or silently substitute missing names.

Examples:

- If an analysis use case references a graph edge type that is not defined, do not invent a replacement edge type.
- If an importer references a runtime event field that does not exist, do not map it to a similarly named field without verification.
- If a replay step expects a method-call event but only a static dependency relation exists, do not treat the dependency relation as proof of execution.
- If a Gradle task referenced by documentation does not exist, do not replace it with another task without verification.
- If README, tests, source code, and stored schema definitions disagree, do not silently choose one interpretation.

Required behavior:

1. Inspect the relevant source files, tests, build files, schemas, and documentation.
2. Verify the exact symbol, method, class, task, field, contract, or data shape.
3. If verification fails, stop the implementation.
4. Report:
    - what was expected,
    - what was found instead,
    - which files were inspected,
    - why continuing would be unsafe.

Do not continue with a speculative implementation.

### No Guessing Rule

Agents must not guess implementation details.

This applies especially to:

- method names
- static helper methods
- Gradle task names
- plugin IDs
- package names
- test class names
- generated file locations
- runtime event field names
- trace correlation fields
- graph node labels
- graph edge labels
- database table names
- JSON schema properties
- LLM prompt template names
- report section names
- SonarQube or JaCoCo task names

If a value cannot be verified from source, build files, tests, documentation, schemas, fixtures, or sample data, the agent must stop and report.

### No Fabricated Evidence Rule

Forensic Analytics must never fabricate analysis evidence.

Agents must not:

- create fake runtime traces to make a replay look complete,
- invent stack frames,
- invent method parameters,
- invent return values,
- infer branch execution from source structure alone,
- treat static dependencies as executed runtime flow,
- treat LLM output as a verified fact,
- silently fill missing graph relationships,
- hide unresolved symbols,
- convert uncertain findings into confirmed findings.

Synthetic test fixtures are allowed only inside tests and must be clearly named as fixtures.

### No Hidden Compatibility Code

Do not introduce compatibility wrappers, aliases, overloads, deprecated bridge methods, fallback paths, or silent schema adapters unless explicitly requested.

Compatibility code may only be added when:

1. the task explicitly asks for backward compatibility,
2. the affected API or schema contract has been verified,
3. the compatibility behavior is tested,
4. and the documentation states why it exists.

### No Side-Effect Renames

Renames must be limited to the explicitly requested symbol unless all affected symbols are verified from source.

Do not rename a method, class, field, package, event field, graph label, Gradle task, API endpoint, or static call in one layer based only on naming symmetry in another layer.

Safe behavior:

1. Verify the actual target symbol.
2. Rename only the confirmed symbol.
3. Update all verified callers, tests, examples, and documentation.
4. If any caller cannot be verified, stop and report.

### No Unrequested Architecture Migration

Do not move packages, rename modules, introduce new layers, replace storage technology, replace graph tooling, introduce a web framework, or restructure the project unless the task explicitly requires it.

Architecture cleanup must be handled as a dedicated task.

## Verify Before Touch Workflow

Every implementation task must start with a read-only verification phase.

### Phase 1: Read-Only Verification

Before modifying files, the agent must inspect:

1. the files explicitly mentioned in the task,
2. directly referenced interfaces,
3. directly referenced implementation classes,
4. directly referenced tests,
5. relevant schemas, fixtures, or generated sample outputs,
6. relevant Gradle tasks or quality documentation when build behavior is affected,
7. README or usage documentation when public behavior is affected,
8. `QUALITY.md` when quality-gate behavior is affected,
9. CI workflow files when commit or pipeline behavior is affected.

During this phase, the agent must not modify files.

The goal is to confirm that the requested change matches the actual repository state.

### Phase 2: Implementation

Only after successful verification may the agent modify files.

The implementation must follow the smallest correct change principle.

A valid implementation must:

- change only files required by the task,
- preserve existing behavior unless the task explicitly changes it,
- avoid speculative improvements,
- avoid unrelated formatting-only changes,
- keep code comments in English,
- keep repository documentation in English,
- preserve forensic evidence semantics,
- keep uncertainty explicit.

### Phase 3: Local Verification

After implementation, the agent must run the narrowest meaningful verification first.

Preferred order:

1. targeted unit test,
2. affected package or module test,
3. importer/parser/replay/graph-specific tests where relevant,
4. ArchUnit rule when architecture is affected,
5. full quality gate as defined by `QUALITY.md`.

The current quality gate source is `QUALITY.md`.

Do not invent or reference a `quality_gate.py` file unless the repository actually contains and documents it.

If the quality gate cannot be executed, the agent must report the reason and provide the commands that were attempted.

## Quality Gate

`QUALITY.md` is the authoritative quality contract for the repository.

The default minimum quality command is expected to be:

```bash
./gradlew test
```

The full local quality gate must be read from `QUALITY.md` and verified against the Gradle build.

If the repository uses the same quality gate as the tracing toolkit baseline, the full local gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

When plugin metadata, Gradle task inputs, task outputs, or plugin implementation classes are changed, also run:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

When general build health is relevant, this diagnostic run may be useful:

```bash
./gradlew clean check --console=plain --stacktrace
```

Do not claim that a command passed unless it was actually executed.

If a command fails, report:

- command executed,
- failure summary,
- relevant failing test or task,
- whether the failure was caused by the current change,
- remaining blocker.

## Architecture Rules

The project follows hexagonal architecture.

Domain and application code must stay independent from technical frameworks, storage implementations, runtime infrastructure, LLM providers, and external tool adapters.

### Architecture Intent

The project is structured around:

- domain model,
- application services,
- ports,
- inbound adapters,
- outbound adapters,
- static analysis importers,
- runtime evidence importers,
- graph persistence,
- replay orchestration,
- LLM-assisted diagnosis,
- reporting and export,
- quality tests.

The intended dependency direction is:

```text
adapters / infrastructure / plugins / UI / CLI
        -> application
        -> domain
```

The reverse direction is forbidden.

### Microservice Boundary Rule

The project follows strict microservice autonomy for service-split work.

Services must not share Java implementation modules, domain modules, DTO modules, repository modules, service modules, utility modules, test-fixture modules, or internal error-model modules.

Allowed integration mechanisms are:

- REST/OpenAPI
- gRPC/protobuf
- RabbitMQ/message contracts

Contracts may be centrally documented, but they must not be used as shared Java implementation modules or as a substitute for service-owned domain models.

Each service must be independently buildable, runnable, testable, configurable, observable, health-checkable, containerized and deployable. Each microservice must own its Spring Boot application, configuration, ports, tests, Dockerfile, health checks, README, adapters and internal domain model.

This rule does not authorize speculative migration of existing modules. The current modular-monolith modules must not be described as implemented microservices. Service extraction must be performed through dedicated, verified slices with explicit service boundary, contract impact, data ownership, test impact, rollback or strangler strategy, quality-gate commands and forbidden changes.

### Package Responsibility Map

The following package responsibilities apply to the Forensic Analytics repository. If the repository uses a different concrete package layout, inspect the current source tree first and map the responsibilities accordingly.

#### Domain

```text
de.burger.forensics.analytics.domain
```

or the repository's verified domain package.

Contains:

- forensic domain models,
- value objects,
- evidence identifiers,
- trace identifiers,
- correlation identifiers,
- analysis findings,
- replay models,
- graph-neutral relationship models,
- domain strategies,
- domain ports,
- domain-level rules.

Allowed dependencies:

- Java standard library,
- domain-internal packages.

Forbidden dependencies:

- Gradle API,
- Maven API,
- JavaParser,
- Joern or CPG implementation APIs,
- Graph database client APIs,
- SQL implementation APIs,
- SLF4J concrete logging providers,
- AspectJ,
- Byteman,
- filesystem adapters,
- runtime tracing implementation,
- LLM SDKs,
- HTTP clients,
- plugin classes,
- infrastructure classes.

#### Application

```text
de.burger.forensics.analytics.application
```

or the repository's verified application package.

Contains:

- use cases,
- orchestration services,
- application-level request and result objects,
- transaction-neutral workflows,
- analysis pipeline coordination,
- replay coordination,
- diagnosis preparation,
- reporting orchestration.

Allowed dependencies:

- domain,
- Java standard library.

Forbidden dependencies:

- Gradle API,
- Maven API,
- JavaParser,
- concrete graph database clients,
- concrete SQL implementation details,
- concrete LLM SDKs,
- concrete runtime helper implementation,
- filesystem-specific implementation details,
- plugin classes,
- UI framework classes.

Application services may depend on ports, not concrete adapters.

#### Inbound Adapters

Typical inbound adapter packages may include:

```text
de.burger.forensics.analytics.adapter.in.cli
de.burger.forensics.analytics.adapter.in.rest
de.burger.forensics.analytics.adapter.in.gradle
de.burger.forensics.analytics.adapter.in.maven
de.burger.forensics.analytics.adapter.in.ui
```

Contains:

- CLI entry points,
- REST controllers if present,
- Gradle task adapters,
- Maven Mojo adapters,
- UI action handlers,
- external command mapping,
- request translation into application use cases.

Inbound adapters must:

- delegate business logic to application services,
- map external input into explicit request objects,
- keep framework-specific annotations out of domain and application,
- avoid performing analysis logic directly.

#### Outbound Adapters

Typical outbound adapter packages may include:

```text
de.burger.forensics.analytics.adapter.out.scan.javaparser
de.burger.forensics.analytics.adapter.out.scan.cpg
de.burger.forensics.analytics.adapter.out.graph
de.burger.forensics.analytics.adapter.out.persistence
de.burger.forensics.analytics.adapter.out.llm
de.burger.forensics.analytics.adapter.out.report
de.burger.forensics.analytics.adapter.out.render
de.burger.forensics.analytics.adapter.out.write.file
```

Contains:

- JavaParser scanning integration,
- Code Property Graph / Joern integration if present,
- graph database adapters,
- SQL or file persistence adapters,
- LLM client adapters,
- report exporters,
- visualization renderers,
- file-writing adapters,
- external service integration.

Outbound adapters may depend inward on application and domain ports.

Outbound adapters must not become orchestration use cases.

#### Infrastructure

```text
de.burger.forensics.analytics.infrastructure
```

or the repository's verified infrastructure package.

Contains:

- persistence configuration,
- technical logging,
- runtime connector implementation,
- filesystem utilities,
- external tool execution,
- environment configuration,
- technical serialization infrastructure,
- security and secret handling infrastructure.

Infrastructure must not contain domain decisions.

#### Runtime Evidence Integration

Runtime tracing helpers and runtime evidence import code must remain lightweight and explicit.

Runtime-related packages may contain:

- trace event ingestion,
- correlation/span handling,
- exception event ingestion,
- runtime payload parsing,
- runtime-to-domain mapping,
- replay input preparation.

Runtime evidence code must not:

- depend on Gradle API,
- depend on JavaParser,
- depend on plugin implementation classes,
- introduce domain decisions,
- perform blocking I/O unless explicitly required,
- throw exceptions from tracing paths without documented behavior,
- fabricate missing runtime values.

## Forensic Analytics Rules

### Evidence Model Rules

Evidence models must distinguish between:

- static source-code facts,
- build-time facts,
- dependency graph facts,
- runtime trace facts,
- exception facts,
- replay-derived facts,
- LLM-generated hypotheses,
- human-confirmed findings.

Do not collapse these categories into a single undifferentiated string or map if type-safe structures are practical.

Every behavior-relevant model should preserve:

- source of evidence,
- timestamp or ordering information where available,
- correlation ID where available,
- process or execution context where available,
- origin system or input file where available,
- confidence or completeness when facts are partial.

### Static Analysis Rules

Static analysis code should:

- detect source relationships,
- extract method, class, branch, dependency, and data-flow candidates,
- preserve source locations,
- keep unresolved symbols explicit,
- avoid deciding runtime execution truth,
- avoid deciding how findings are finally reported.

Static analysis code must not:

- treat source reachability as proof of runtime execution,
- silently drop unresolved references,
- mix graph persistence details into parser logic,
- mix report rendering into scanner logic.

### Runtime Trace Rules

Runtime trace ingestion should:

- preserve correlation IDs,
- preserve span/process IDs,
- preserve event order,
- preserve exception class, message, stack trace, and source location if available,
- preserve method parameters and return values only when actually observed,
- keep missing values explicit.

Runtime trace ingestion must not:

- infer parameter values from source code,
- infer branch decisions without observed runtime data,
- convert logging text into structured facts without a verified parser,
- mutate original trace payloads destructively.

### Graph Rules

Graph construction must be deterministic and testable.

Graph code should:

- use explicit node and edge types,
- preserve relationship provenance,
- distinguish static edges from runtime edges,
- avoid nondeterministic ordering,
- keep graph labels and properties centralized where practical,
- include tests for schema-relevant changes.

Graph code must not:

- invent relationships to make visualizations complete,
- collapse different evidence types into ambiguous edges,
- depend on UI rendering decisions,
- leak concrete graph database APIs into domain or application code.

### Replay Rules

Exception-centered replay must be evidence-based.

Replay code should:

- start from an explicit exception or correlation context,
- reconstruct only observed or derivable steps,
- identify missing evidence clearly,
- preserve ordering and causality constraints,
- separate static context from runtime execution,
- produce stable replay output for the same inputs.

Replay code must not:

- present speculative paths as executed paths,
- hide missing trace events,
- fabricate method parameters or return values,
- merge unrelated correlation IDs,
- silently continue after inconsistent evidence unless the inconsistency is represented in the result.

### LLM Integration Rules

LLM integration is an analysis assistant, not an evidence source.

LLM-related code must:

- pass structured evidence into prompts,
- label LLM output as hypothesis, explanation, recommendation, or generated text,
- keep raw evidence separate from LLM interpretation,
- avoid sending secrets, credentials, tokens, personal data, or unnecessary source content,
- support deterministic prompt construction where practical,
- be testable without requiring a live LLM provider.

LLM-related code must not:

- treat generated output as confirmed fact,
- overwrite evidence with generated summaries,
- hide prompt inputs from tests or audits,
- depend on a concrete LLM provider in domain or application code,
- require network access for unit tests.

### Reporting Rules

Reports must distinguish clearly between:

- confirmed evidence,
- derived analysis,
- unresolved gaps,
- hypotheses,
- suggested fixes,
- verification status.

Reports must not present uncertain findings as verified defects.

## Build Tool Adapter Rules

If the repository contains Gradle or Maven adapters, they must remain build-tool adapters only.

### Gradle Adapter Rules

Gradle task classes must:

- declare inputs and outputs explicitly,
- use Gradle `Property<T>`, `RegularFileProperty`, and `DirectoryProperty` where appropriate,
- avoid resolving files eagerly during configuration,
- avoid doing execution work during configuration,
- keep task actions small,
- delegate business logic to application services.

Gradle task classes must not:

- perform broad source parsing logic directly,
- contain domain rules,
- contain replay logic,
- contain LLM diagnosis logic,
- introduce hidden filesystem assumptions,
- use static mutable state for task behavior.

### Maven Adapter Rules

Maven Mojo classes must:

- map Maven parameters to application request objects,
- delegate execution to application services,
- avoid duplicating Gradle task behavior,
- keep Maven API usage inside Maven adapter packages.

Maven Mojo classes must not depend on Gradle task classes.

### Build-Tool Boundary Rules

Common build-tool code must not depend on Gradle or Maven APIs.

Gradle-specific packages may depend on Gradle APIs and must not depend on Maven APIs.

Maven-specific packages may depend on Maven APIs and must not depend on Gradle APIs.

## IF-Less Development Rules

The project prefers IF-Less development.

This does not mean that every `if` is forbidden.

It means that repeated, branching-heavy, behavior-selecting logic should be replaced by explicit structures.

Prefer:

- strategy pattern,
- polymorphism,
- enum-to-strategy maps,
- lookup tables,
- immutable configuration objects,
- declarative rule registration,
- command objects,
- value-object behavior,
- pipeline stages with explicit contracts.

Avoid:

- long `if / else if / else` chains,
- duplicated branch conditions,
- type-code branching,
- mode flags controlling large behavior blocks,
- switch statements that duplicate strategy dispatch,
- boolean parameters that change method meaning.

Acceptable uses of `if`:

- guard clauses,
- null validation,
- boundary checks,
- error handling,
- simple fail-fast preconditions,
- direct translation of a domain rule when clearer than abstraction.

When replacing conditionals, do not over-engineer.

The smallest understandable design wins.

## Declarative Programming Preference

Prefer declarative configuration and explicit wiring over implicit control flow.

Good examples:

- analysis pipeline definitions,
- strategy registries,
- explicit event mappings,
- graph schema constants,
- immutable request objects,
- explicit replay stage wiring,
- prompt template registries,
- ArchUnit rules that document boundaries
- test fixtures that describe scenarios.

Avoid:

- hidden conventions,
- implicit stringly-typed behavior,
- magic method names,
- reflection-based wiring unless explicitly justified,
- undocumented fallback behavior,
- side effects hidden in constructors.

Declarative code must remain readable.

Do not replace simple code with abstract DSL-like structures unless it improves maintainability.

## Testing Rules

The project uses JUnit 6 and may use ArchUnit, Java 25-compatible JaCoCo, TestKit, integration tests, and fixture-based replay tests.

### Regression-First Workflow

When fixing a bug:

1. Write or update a failing test that reproduces the bug.
2. Verify that the test fails for the expected reason when practical.
3. Implement the smallest fix.
4. Run the targeted test.
5. Run the quality gate.

If writing a regression test is not practical, explain why in the final report.

### Test Placement Guide

Place tests according to the affected production code.

Examples:

```text
src/main/java/**/domain/**
-> src/test/java/**/domain/**

src/main/java/**/application/**
-> src/test/java/**/application/**

src/main/java/**/adapter/**
-> src/test/java/**/adapter/**

src/main/java/**/infrastructure/**
-> src/test/java/**/infrastructure/**

src/main/java/**/runtime/**
-> src/test/java/**/runtime/**
```

Architecture rules belong under a quality-related test package such as:

```text
src/test/java/**/quality
```

SOLID-related quality tests belong under a package such as:

```text
src/test/java/**/quality/solid
```

### Unit Tests

Unit tests must:

- test behavior, not implementation details,
- use descriptive names,
- avoid brittle assertions,
- avoid dependence on test execution order,
- use temporary directories for filesystem output,
- avoid writing to repository files,
- avoid requiring external services,
- keep fixtures small and explicit.

### Integration Tests

Integration tests are appropriate when verifying:

- source ingestion,
- runtime trace ingestion,
- graph persistence,
- replay reconstruction,
- report generation,
- build-tool adapters,
- importer/exporter round trips,
- schema migrations.

Integration tests must not require external services unless explicitly documented and isolated.

### LLM Tests

LLM-related tests must not require live provider access by default.

Prefer:

- fake LLM clients,
- captured prompt assertions,
- structured response fixtures,
- contract tests for prompt input construction,
- tests that verify evidence is not overwritten by generated hypotheses.

### ArchUnit Tests

Architecture-sensitive changes should be protected with ArchUnit tests.

Use ArchUnit to enforce:

- domain independence,
- application independence from infrastructure,
- forbidden dependencies,
- package boundaries,
- adapter dependency direction,
- build-tool adapter isolation,
- LLM provider isolation,
- graph provider isolation.

Do not weaken ArchUnit rules to make a change pass.

If a rule is wrong, explain why and update the rule with a dedicated justification.

### Coverage

JaCoCo is part of the repository quality setup where configured.

Do not lower coverage thresholds to make a task pass.

Do not exclude production code from coverage unless the task explicitly requires it and the exclusion is justified.

## Documentation Rules

Documentation must stay aligned with source code.

When changing public API, analysis workflows, graph schema, replay behavior, generated outputs, runtime event fields, report format, plugin configuration, or examples, inspect and update relevant documentation.

Relevant documentation may include:

- `README.md`,
- `QUALITY.md`,
- `AGENTS.md`,
- ADRs,
- workflow documents,
- example files,
- schema documentation,
- report format documentation,
- CI documentation,
- commit prompt documents.

Do not update documentation based on assumptions.

Verify documented method names, task names, plugin IDs, paths, commands, event fields, graph labels, schema fields, and output locations from source.

## Documentation Ownership

`AGENTS.md` is the primary source of truth for agent behavior, architecture boundaries, coding rules, test-placement rules, and safety rules.

Other documents, including commit prompts and task prompts, may summarize these rules, but they must not redefine them independently.

If a conflict exists between `AGENTS.md` and another prompt document, `AGENTS.md` wins unless the task explicitly states otherwise.

Commit prompts should reference `AGENTS.md` instead of duplicating architecture definitions.

This avoids rule drift between automation instructions and repository-level engineering rules.

## Diagram and Visualization Rules

Diagram generation is an output adapter concern.

Visualization code must:

- consume explicit graph or analysis models,
- preserve evidence distinctions,
- generate deterministic output,
- keep renderer-specific logic outside domain and application,
- be optional unless the project explicitly requires it.

If PUML generation has been removed or is being removed, do not reintroduce PlantUML dependencies, PUML files, PUML tasks, or PUML documentation unless explicitly requested.

If a diagram renderer is changed, verify:

- output file location,
- deterministic ordering,
- graph size handling,
- tests or snapshots where available,
- documentation alignment.

## Security and Data Protection Rules

Forensic Analytics may process sensitive source code, runtime traces, stack traces, logs, and business data.

Agents must:

- avoid logging secrets,
- avoid committing credentials,
- avoid sending secrets to LLM providers,
- avoid storing unnecessary personal data,
- keep raw evidence access explicit,
- respect `.gitignore` and local artifact boundaries,
- avoid committing generated trace data unless explicitly requested.

Do not add dependencies that introduce network calls, telemetry, or external service access without explicit justification.

## Dependency Rules

Dependencies must be treated carefully.

Do not add dependencies unless explicitly required.

Before adding a dependency, verify:

- why it is needed,
- whether the JDK or existing dependencies already provide the required capability,
- whether it affects plugin consumers,
- whether it affects runtime footprint,
- whether it introduces logging providers,
- whether it introduces external service communication,
- whether it conflicts with the existing dependency strategy.

The project should avoid bundling concrete logging providers unless explicitly required.

Do not introduce logging bindings such as:

- `logback-classic`,
- `slf4j-log4j12`,
- `slf4j-reload4j`,

unless the task explicitly requires it and the impact is documented.

## Version Rules

Use the configured project baseline.

Do not upgrade Java, Gradle, plugins, dependencies, JaCoCo, SonarQube plugin, graph database clients, LLM SDKs, or publishing plugins unless the task explicitly asks for it.

Do not change dependency versions as part of unrelated fixes.

## Source Code Style

Java source code must:

- use English comments,
- use English JavaDoc,
- use clear method names,
- prefer immutable data where practical,
- avoid unnecessary setters,
- avoid static mutable state,
- avoid hidden side effects,
- keep classes focused,
- keep public APIs stable unless explicitly changed.

Avoid:

- unrelated formatting,
- large methods,
- mixed abstraction levels,
- broad catch blocks without purpose,
- swallowing exceptions,
- null-heavy APIs where value objects would be clearer,
- stringly-typed dispatch when typed models are available.

## Error Handling Rules

Errors must be explicit and useful.

Prefer:

- fail-fast validation,
- descriptive exception messages,
- preserving original causes,
- narrow exception handling,
- explicit fallback behavior only when documented,
- explicit unresolved states for incomplete evidence.

Avoid:

- silent fallback,
- catching `Exception` without clear purpose,
- returning null to signal failures,
- hiding parser, ingestion, graph, replay, rendering, or LLM failures,
- ignoring file write failures,
- converting inconsistent evidence into a successful replay without a warning.

## Public API and Schema Rules

Public API and schema changes require extra care.

Before changing public API or schema, inspect:

- interface declarations,
- implementations,
- examples,
- README snippets,
- tests,
- JSON schemas,
- database migrations,
- graph labels and properties,
- runtime helper usage,
- report consumers,
- build-tool usage.

If a public API method, event field, graph label, table column, or JSON property is renamed, update all verified callers, fixtures, tests, examples, and documentation.

Do not assume matching names across layers.

## File Writing Rules

File-writing adapters and tasks must:

- create parent directories where needed,
- write deterministic output,
- avoid partially written files where practical,
- use declared Gradle outputs when invoked from Gradle tasks,
- avoid writing outside configured output locations,
- avoid writing raw sensitive evidence into repository paths unless explicitly requested.

Do not write to source directories unless explicitly requested.

## Logging Rules

Logging must be useful but not noisy.

Logging code must:

- avoid introducing concrete logging providers,
- avoid leaking secrets,
- avoid excessive logs in normal test output,
- preserve useful diagnostics for failures,
- keep build logging behind build-tool logging adapters where appropriate,
- keep runtime evidence output separate from application logs.

Runtime trace output must remain separate from build logging concerns.

## Commit Rules

A commit must be clearly documented:

1. what was changed,
2. why it was changed,
3. how it was changed,
4. which files or components were affected,
5. whether bugs were fixed,
6. whether new features were introduced,
7. whether refactoring, cleanup, structural, or architectural changes were made,
8. whether tests were added or adjusted,
9. whether any breaking or behavior-relevant changes exist,
10. which verification commands were executed.

Do not create vague commit messages.

Do not commit generated build output, local trace data, local database files, IDE metadata, `.gradle`, `build`, temporary files, or unrelated local files unless explicitly required.

## Required Git Inspection Before Commit

Before creating a commit, inspect:

```bash
git status
git diff
git diff --cached
```

If staged and unstaged changes both exist, inspect them separately.

Do not commit files that are unrelated to the task.

If unexpected changes exist, stop and report.

## Final Report Requirements

At the end of every task, report:

1. files changed,
2. main changes made,
3. tests or verification commands executed,
4. commands that failed, if any
5. quality gate result,
6. known limitations,
7. remaining blockers, if any.

Do not claim success for unexecuted verification.

If no files were changed, say so explicitly.

## Definition of Done

A task is done only when:

- the requested change was implemented,
- the change follows this `AGENTS.md`,
- no speculative changes were introduced,
- forensic evidence semantics were preserved,
- the uncertainty remains explicit,
- relevant tests were added or updated when needed,
- the narrowest meaningful verification was executed,
- the quality gate from `QUALITY.md` was executed or a clear reason was reported,
- documentation was updated when public behavior changed,
- examples were kept consistent with public API,
- the final report is accurate.

## Forbidden Actions

Agents must not:

- guess missing names,
- invent Gradle tasks,
- reference undocumented quality mechanisms,
- fabricate forensic evidence,
- treat LLM output as verified evidence,
- silently rename side-effect symbols,
- add compatibility wrappers without request,
- weaken tests to make a build pass,
- lower coverage thresholds without request,
- remove architecture rules without justification,
- introduce framework dependencies into domain code,
- introduce Gradle or Maven dependencies into application or domain code,
- leak graph database APIs into domain or application code,
- leak LLM provider APIs into domain or application code,
- perform broad package migrations without request,
- change Java or Gradle baseline without request,
- reintroduce removed PUML generation without request,
- commit unrelated files,
- claim verification passed without executing it.
