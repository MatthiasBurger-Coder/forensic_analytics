# Codex Commit Workflow — `forensics_tracing`

## Purpose

This file defines the commit workflow for the repository located at:

```text
D:/Projects/forensics_tracing
```

Use this workflow when Codex is asked to inspect, fix, commit, and push changes in the **Forensics Tracing** project.

The goal is not to create a vague commit message. The goal is to:

1. inspect the repository state,
2. respect the repository-level instructions,
3. run the correct project quality gates,
4. fix realistic quality-gate issues within the current task scope,
5. review the final diff carefully,
6. create a precise commit message based only on the actual diff,
7. commit deliberately,
8. push only when the previous steps succeeded.

---

## Project Context

This repository belongs to the **Forensics Tracing / Forensics Platform** workstream.

The current codebase is primarily a Java-based forensic tracing and rule-generation project. Its core responsibilities include:

- Java source-code scanning
- AST-based event extraction
- dependency and symbol-context analysis
- Byteman rule generation
- runtime tracing helpers
- Gradle plugin integration
- optional Maven adapter work if present in the repository
- quality, architecture, coverage, and CI hardening

The wider product vision includes exception-centered runtime replay, graph-backed forensic analysis, and LLM-supported error diagnosis. Do **not** introduce or change those platform-level features unless the current task and the actual repository diff explicitly require it.

---

## Repository-Level Instruction Precedence

Before making or committing any change, inspect the repository root for project-level instruction files.

If present, apply this precedence order:

1. `AGENTS.md`
2. `QUALITY.md`
3. `.github/workflows/*`
4. `settings.gradle`, `settings.gradle.kts`
5. `build.gradle`, `build.gradle.kts`
6. `gradle/libs.versions.toml`
7. this `Commit.md`
8. general assumptions or heuristics

Rules:

- `AGENTS.md` has priority for agent behavior, architecture rules, stop conditions, branch rules, and commit conventions.
- `QUALITY.md` is the binding quality contract.
- CI workflow files are the source of truth for what the remote pipeline verifies.
- If this file conflicts with `AGENTS.md` or `QUALITY.md`, the repository-level document wins.
- Do not guess missing rules. Read the actual files.
- If a class, method, package, task, or architectural contract cannot be found, stop and report instead of inventing a replacement.

---

## Non-Negotiable Project Constraints

### Java and Gradle

- Use **Java 25** as the project baseline unless the repository-level documents explicitly state otherwise.
- Use the project Gradle wrapper if available.
- Verify the wrapper version with `./gradlew --version`.
- Do not upgrade Gradle, Java, plugins, or dependencies unless the current task explicitly requires it.
- Do not switch to Maven as the build or quality gate for this repository.
- If a Maven adapter exists, treat it as project code, not as the primary build system.

### Framework and Runtime Boundaries

- Preserve the existing Gradle plugin nature of the project.
- Do not introduce Spring Boot.
- Do not introduce unrelated frameworks.
- Do not introduce infrastructure dependencies into the domain layer.
- Do not move domain behavior into plugin, adapter, or infrastructure packages.
- Do not introduce Gradle API dependencies into domain or application code.
- Do not introduce Byteman rendering or file-system concerns into domain classes unless already intentionally modeled.

### Source-Code Language Rules

- Source code must be written in English.
- Source-code comments must be written in English.
- Script comments must be written in English.
- Do not add decorative icons or emojis to scripts or source files.
- Documentation may remain in English unless the task explicitly requires German documentation.

---

## Architecture Context

The project follows a hexagonal architecture style. Respect actual repository structure first. If the package layout differs from the examples below, inspect and map the real package responsibilities before changing code.

### Domain Layer

Typical package:

```text
de.burger.forensics.domain
```

Responsibilities:

- domain model
- domain strategies
- domain ports
- pure domain behavior
- rule-generation concepts when they are intentionally modeled as domain behavior

Forbidden unless already intentionally present:

- Gradle API
- JavaParser API
- filesystem concerns
- concrete logging frameworks
- concrete database technology
- Byteman file writing
- CI or build tooling

### Application Layer

Typical package:

```text
de.burger.forensics.application
```

Responsibilities:

- use cases
- orchestration
- request/result models
- coordination of domain services and ports

Important components may include:

- `GenerateRulesUseCase`
- `GenerationRequest`
- `RuleGenerationResult`

### Adapter Layer

Typical packages:

```text
de.burger.forensics.adapters
de.burger.forensics.adaptersupport
```

Responsibilities:

- JavaParser integration
- source scanning
- scanner support code
- conversion from external APIs to application/domain models
- parsing and normalization helpers

Important components may include:

- `JavaParserScanner`
- `MethodEventExtractor`
- condition rendering helpers
- source-root discovery helpers
- dependency graph or symbol-context adapters if present

### Plugin Adapter Layer

Typical package:

```text
de.burger.forensics.plugin
```

Responsibilities:

- Gradle plugin adapter
- Gradle task wiring
- plugin extension
- plugin runtime location
- integration from Gradle configuration into application use cases
- Byteman rule file output orchestration if currently located there

Important components may include:

- `BtmGenPlugin`
- `BtmGenExtension`
- `GenerateBtmTask`
- `PluginRuntimeLocator`
- `BtmFileWriter`

If the repository contains a Maven plugin adapter, keep it separated from the Gradle adapter. Do not blend Maven Mojo code into Gradle task code.

### Byteman Rule Rendering

Important components may include:

- `BytemanRuleRenderAdapter`
- `BytemanRuleRenderer`
- `IfTrueRuleStrategy`
- `IfFalseRuleStrategy`
- `SwitchRuleStrategy`
- `SwitchCaseRuleStrategy`
- `MethodEnterRuleStrategy`
- `MethodExitRuleStrategy`
- `ReturnRuleStrategy`
- `ThrowRuleStrategy`
- `JdbcExecuteRuleStrategy`
- `ThreadLifecycleRuleStrategy`

When rule-generation behavior changes, explicitly verify and mention affected rule types.

### Runtime and Infrastructure

Typical package:

```text
de.burger.forensics.infrastructure
```

Responsibilities:

- runtime tracing helper code
- tracing event models if currently located there
- AspectJ logging support
- runtime instrumentation support
- infrastructure concerns

Important components may include:

- `RtTrace`
- `RtTraceHelper`
- `RtTracer`
- `RtEvent`
- `RtSpanToken`
- `MethodLoggingAspect`
- `SuppressLogging`

### Testing and Quality

Expected test concerns may include:

- JUnit 6 tests
- ArchUnit tests
- SOLID heuristic tests
- Gradle TestKit tests
- JaCoCo coverage verification
- plugin functional tests
- source-scanning regression tests
- Byteman rendering regression tests
- runtime tracing tests

Do not weaken tests or architecture rules to make a commit pass.

---

## Project-Specific Change Classification

When inspecting the diff, classify changes into one or more of these project areas:

- Gradle plugin wiring
- Maven plugin adapter wiring, if present
- source-root detection
- JavaParser scanning
- AST context propagation
- type resolution / symbol resolution
- dependency graph persistence or cache logic
- condition rendering / normalization
- Byteman rule rendering
- generated BTM file format
- runtime tracing
- correlation/span/event behavior
- logging aspect behavior
- file output behavior
- monorepo or multi-module handling
- configuration cache compatibility
- dependency verification / build security
- CI workflow behavior
- SonarCloud integration
- JaCoCo coverage verification
- ArchUnit or SOLID quality rules
- README / QUALITY / AGENTS documentation
- PUML / PlantUML / visualization behavior, if still present in the repository

If a change affects generated rule behavior, verify the relevant emitted rules rather than only checking compilation.

---

## Strict Execution Order

## Phase 1 — Repository Inspection

Start in the repository root.

Run and inspect:

```bash
pwd
git rev-parse --show-toplevel
git status --short
git diff --name-status
git diff --cached --name-status
```

Then inspect the repository structure and build files:

```bash
ls -la
find . -maxdepth 2 -type f | sort
```

Inspect these files if present:

```text
AGENTS.md
QUALITY.md
settings.gradle
settings.gradle.kts
build.gradle
build.gradle.kts
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
gradle/verification-metadata.xml
.github/workflows/*.yml
.github/workflows/*.yaml
```

Determine and record:

1. whether this is a single-module or multi-module Gradle build,
2. which modules are included,
3. whether Gradle wrapper exists,
4. which Java version is used,
5. which Gradle version the wrapper declares,
6. which quality commands `QUALITY.md` documents,
7. which CI workflows exist,
8. which Gradle tasks the CI workflows run,
9. whether staged changes already existed before your work,
10. whether unrelated user changes are present.

Do not modify files before this inspection is complete unless a repository-level document explicitly instructs otherwise.

---

## Phase 2 — Read Repository Rules

### `AGENTS.md`

If `AGENTS.md` exists:

- read it completely,
- extract architecture rules,
- extract stop-and-report rules,
- extract commit message rules,
- extract branch or push restrictions,
- extract test and quality expectations,
- apply those rules exactly.

If `AGENTS.md` is missing, report that it was not found.

### `QUALITY.md`

If `QUALITY.md` exists:

- read it completely,
- treat it as the binding quality contract,
- extract the documented minimum quality command,
- extract the full local quality gate if documented,
- extract coverage expectations,
- extract SonarCloud expectations,
- extract architecture/test expectations.

If `QUALITY.md` is missing, stop and report unless the current task explicitly says to create or repair it.

### CI Workflows

Read all workflow YAML files in `.github/workflows`.

Record:

- workflow names,
- trigger conditions,
- Java setup version,
- Gradle setup behavior,
- dependency verification behavior,
- Gradle tasks executed,
- SonarCloud steps,
- publishing steps,
- whether the local quality gate covers the CI checks.

Do not guess CI behavior.

---

## Phase 3 — Verify Toolchain

Before running quality checks, verify:

```bash
java -version
./gradlew --version
./gradlew tasks --all --console=plain
```

Rules:

- Java must be Java 25 unless the repository-level rules explicitly define a different baseline.
- Use the Gradle wrapper.
- Do not replace the wrapper.
- Do not upgrade dependencies as part of commit preparation.
---

## Phase 4 — Run Quality Gates Before Commit

Run the documented `QUALITY.md` command first.

The expected minimum command for this project usually is:

```bash
./gradlew test --console=plain --stacktrace
```

If `QUALITY.md` documents a different or stronger command, run the documented command instead and report the difference.

After the documented command succeeds or after its failures have been handled, run the full local Gradle quality gate:

```bash
./gradlew clean check jacocoTestReport --console=plain --stacktrace
```

If the repository documents that `check` already depends on `jacocoTestCoverageVerification`, do not bypass that verification.

If the repository defines an explicit additional coverage task, run it unless the repository-level documents say otherwise.

Do not skip:

- unit tests,
- integration tests that are part of `check`,
- ArchUnit tests,
- SOLID heuristic tests,
- Gradle TestKit tests,
- JaCoCo coverage verification,
- plugin validation tasks that are part of `check`,
- dependency verification that is part of the build.

---

## Phase 5 — Handle Quality Failures

When a quality gate fails:

1. capture the relevant failure output,
2. identify the concrete failing task,
3. identify the failing class, test, package, rule, or Gradle configuration,
4. classify the failure as one of:
    - introduced by the current change set,
    - pre-existing repository issue,
    - environment/tooling issue,
    - external service issue,
    - dependency verification issue,
    - CI-only issue,
5. fix all realistically fixable issues within the current task scope,
6. re-run the failing command,
7. re-run the full local quality gate.

Rules:

- Do not disable failing tests to make the build pass.
- Do not remove meaningful assertions.
- Do not lower JaCoCo thresholds unless the current task explicitly changes the policy.
- Do not weaken ArchUnit or SOLID rules unless an explicit architecture decision requires it.
- Do not hide compiler warnings without fixing the cause.
- Do not add broad dependency-verification trust rules when narrow, exact metadata entries are sufficient.
- Do not delete `verification-metadata.xml` to bypass dependency verification.
- Do not commit generated build outputs.
- Do not silently accept flaky failures. Report them with evidence.

If the failure is caused by missing external credentials, such as a SonarCloud token, report it as an external/local environment limitation and do not fake success.

---

## Phase 6 — Optional External Quality Gate

If the environment provides the required SonarCloud token and the repository documents local Sonar execution, run:

```bash
./gradlew sonar --console=plain --stacktrace
```

If the token is missing or unavailable:

- report that SonarCloud was skipped locally,
- do not treat the missing token as a code failure,
- do not claim Sonar success,
- do not remove or weaken Sonar configuration.

---

## Phase 7 — Final Repository Inspection

Only after quality-gate handling, inspect the final state again:

```bash
git status --short
git diff
git diff --cached
git diff --name-status
git diff --cached --name-status
```

Rules:

- Review the real final diff, not only filenames.
- Separate staged and unstaged changes deliberately.
- Do not accidentally include unrelated user changes.
- Do not commit generated build directories.
- Do not commit `.gradle`, `build`, IDE workspace files, local logs, temporary files, or downloaded artifacts unless explicitly intended.
- If pre-existing staged changes were present, do not rewrite or unstage them without a clear reason.
- If unrelated changes exist, report them and keep them out of the commit when possible.

---

## Phase 8 — Commit Message Creation

Create the commit message from the actual final diff only.

Do not invent facts that are not visible in the code changes.

Do not use vague messages such as:

```text
update code
fix issues
small improvements
quality fixes
cleanup
```

Allowed commit types:

```text
feat
fix
refactor
chore
test
docs
perf
```

Preferred structure:

```text
<type>: <short precise summary>

What:
- ...
- ...

Why:
- ...
- ...

Changes:
- ...
- ...

Impact:
- ...
- ...

Testing:
- ...
- ...
```

Commit-message rules:

- Mention affected layers when relevant: domain, application, adapter, plugin adapter, infrastructure, test support, CI/build tooling.
- Mention affected packages, classes, Gradle tasks, rule strategies, use cases, ports, adapters, tests, or workflow files where relevant.
- If bugs were fixed, state the faulty behavior corrected.
- If a feature was added, state what it does and why it was added.
- If refactoring was done, explain the structural improvement.
- If rule rendering changed, name the affected rule strategy or emitted rule format.
- If JavaParser scanning changed, name the constructs or context handling that changed.
- If runtime tracing changed, the name affected event fields, correlation/span behavior, helper calls, or enablement behavior.
- If dependency verification changed, mention exactly why.
- If `QUALITY.md`, `AGENTS.md`, or CI workflows changed, mention that explicitly.
- If tests were added or adjusted, mention what was covered or repaired.
- If quality-gate fixes were necessary, mention them in the body when they affected code or tests.
- If no quality fixes were necessary, say that in the final execution summary, not necessarily in the commit body.

---

## Phase 9 — Stage, Commit, and Push

Only after the final diff has been reviewed:

1. stage only relevant files,
2. create the commit using the prepared detailed message,
3. capture the commit hash,
4. determine the current branch name,
5. push to the current branch.

Commands should follow the actual repository state. Typical sequence:

```bash
git add <relevant-files>
git commit -m "<type>: <short precise summary>" -m "<body>"
git rev-parse --short HEAD
git branch --show-current
git push
```

Rules:

- Do not commit before reviewing the final diff.
- Do not push before the commit was created successfully.
- Do not force-push unless explicitly instructed.
- If push is rejected because the remote branch changed, report the exact reason and do not force-push.
- If no relevant changes exist, do not create an empty commit unless explicitly instructed.

---

## Final Execution Report

After execution, print a final report containing:

1. repository root used,
2. whether `AGENTS.md` was found and which rules it defined,
3. whether `QUALITY.md` was found,
4. the quality command documented in `QUALITY.md`,
5. CI pipeline tasks identified from `.github/workflows`,
6. exact local quality-gate command used,
7. Java version used,
8. Gradle wrapper version used,
9. whether the documented quality command passed,
10. whether the full local Gradle quality gate passed,
11. whether SonarCloud was executed or skipped,
12. if a quality command failed, the exact reason,
13. which fixes were applied because of quality checks,
14. whether any failures were pre-existing, environment-related, or external-service-related,
15. final changed files,
16. final commit message,
17. branch name,
18. new commit hash,
19. whether the push succeeded.

If any blocker remains, state it explicitly.

Do not claim success for failed steps.

---

## Compact Project-Specific Quality Summary

Expected baseline logic for this project:

```text
AGENTS.md                         = agent rules / highest local instruction precedence
QUALITY.md                        = binding quality contract
.github/workflows                 = CI alignment source
Java 25                           = baseline unless repository documents otherwise
Gradle wrapper                    = required build entry point
./gradlew test                    = expected documented minimum quality command
./gradlew clean check jacocoTestReport --console=plain --stacktrace
                                  = expected full local commit gate
./gradlew sonar --console=plain --stacktrace
                                  = optional external gate only with token
```

---

## Stop Conditions

Stop and report instead of guessing when:

- `AGENTS.md` or `QUALITY.md` contradicts this file,
- Java or Gradle version expectations cannot be satisfied,
- the repository root cannot be identified,
- the relevant class, method, package, Gradle task, or module cannot be found,
- an architectural boundary would need to be violated,
- a public API would need a breaking change not requested by the task,
- generated files appear in the diff unexpectedly,
- staged changes existed before your work and their ownership is unclear,
- the quality gate fails for a reason that cannot be fixed safely within scope,
- CI behavior cannot be determined because workflows are missing or unreadable,
- if the remote rejects the push.

When stopping, include:

- what was attempted,
- what was found,
- why continuing would be unsafe,
- the exact command or file that exposed the blocker,
- the smallest safe next step.
