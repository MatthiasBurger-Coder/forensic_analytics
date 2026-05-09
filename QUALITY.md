# Code Quality & Evidence Integrity Checks

This repository contains the **Forensic Analytics Platform** quality contract for contributors and automated agents.

The platform analyses software systems by combining static analysis, runtime trace data, exception context, graph relationships, replay information, and LLM-assisted diagnostic output. Because the platform may be used to explain failures, regressions, suspicious runtime behavior, and architectural violations, quality is not limited to test coverage. Quality also includes evidence integrity, reproducibility, traceability, deterministic analysis output, and clear separation between verified facts and inferred hypotheses.

`QUALITY.md` is the project-specific quality contract for this repository.

Use `.\gradlew.bat` on Windows PowerShell and `./gradlew` on Unix-like shells.

> Windows note: the command is written as `./gradlew` in this document for readability. On Windows PowerShell, use `.\gradlew.bat` with the same task arguments.

---

## Project Baseline

The project baseline is:

- Java 17
- Gradle 9.4.0
- JUnit 5
- ArchUnit
- JaCoCo
- SonarQube / SonarCloud related quality checks
- Gradle Dependency Verification in strict mode
- Hexagonal architecture
- Regression-first workflow
- Evidence-first analytics
- Deterministic analysis output
- Clear separation of facts, derived facts, hypotheses, and LLM-generated explanations

All source code, comments, JavaDoc, test names, and repository documentation must be written in English.

Do not upgrade Java, Gradle, plugins, dependencies, JaCoCo, SonarQube plugins, database drivers, graph libraries, LLM SDKs, or publishing plugins unless the current task explicitly requires it.

---

## Quality Scope

The quality gate applies to all project areas, including:

- domain model
- evidence model
- source-code analysis
- static analysis imports
- runtime trace imports
- exception context extraction
- graph model and graph persistence
- replay model and replay reconstruction
- analysis pipeline orchestration
- diagnostics and finding generation
- LLM prompt construction and LLM output handling
- reporting and visualization adapters
- build logic
- documentation
- examples
- tests

The quality gate must protect both technical correctness and forensic correctness.

---

## Minimum Quality Command

Run the documented minimum verification command before broader validation:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

This runs the full test suite, including JUnit 5 tests, ArchUnit checks, and SOLID-oriented quality tests when they are present in the repository.

Gradle Dependency Verification must remain enabled in strict mode.

Do not replace this command with a Python script, shell script, IDE run configuration, or manually selected subset of tests.

---

## Full Local Quality Gate

The authoritative local quality gate for this repository is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Each task is listed explicitly for a reason:

- `clean` removes stale build output before verification.
- `test` runs the full automated test suite.
- `jacocoTestReport` generates the JaCoCo XML report required by downstream coverage checks.
- `jacocoTestCoverageVerification` verifies the configured JaCoCo coverage rules.
- `checkPackageCoverage` verifies per-package line and branch coverage against the repository thresholds.
- `--dependency-verification strict` verifies resolved Gradle artifacts against `gradle/verification-metadata.xml`.

`checkPackageCoverage` must be run explicitly if it is not wired into the default Gradle `check` lifecycle.

Agents and contributors must not claim that the full local gate passed unless the exact command above, or a repository-documented equivalent with the same tasks and strict dependency verification, was actually executed.

---

## Partial Diagnostic Command

This command is useful for general build-health diagnostics:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

This command may be incomplete for this repository.

If `checkPackageCoverage` is not part of the default Gradle `check` lifecycle, `./gradlew clean check` is not the complete local quality gate on its own.

Use it only as a diagnostic run, not as the final commit gate unless the repository explicitly wires all required quality tasks into `check`.

---

## Domain-Specific Quality Gates

Forensic Analytics requires additional correctness guarantees beyond generic unit tests.

When a change affects one of the following areas, the tests must verify the behavior at the appropriate level.

### Evidence Model

Changes to evidence classes, evidence IDs, provenance metadata, timestamps, source locations, trace IDs, correlation IDs, or finding references must verify that:

- every finding can be traced back to concrete evidence
- evidence identity is stable and deterministic
- evidence provenance is preserved during transformations
- original source information is not overwritten by derived analysis
- missing evidence is represented explicitly, not silently invented
- serialized evidence can be read back without semantic loss when serialization is part of the change

A diagnostic result must distinguish between:

- observed facts
- derived facts
- inferred relationships
- hypotheses
- LLM-generated explanations

LLM-generated content must never be stored or presented as primary evidence unless the system explicitly labels it as model output.

### Static Analysis

Changes to static analysis, AST scanning, symbol extraction, call graphs, control-flow extraction, data-flow extraction, dependency extraction, or repository import logic must verify that:

- parsed source locations remain stable
- method, class, package, and module identities are deterministic
- unresolved symbols are reported instead of silently converted into false relationships
- parser-specific behavior remains inside adapter packages
- source analysis does not contain reporting, LLM, replay, or persistence policy
- large repositories and multi-module layouts are not accidentally reduced to a single source root

If unresolved symbols, missing imports, skipped files, or unsupported language constructs are encountered, the result must expose them as diagnostics or limitations.

### Runtime Trace Import

Changes to runtime trace ingestion, event normalization, correlation handling, span handling, exception capture, or runtime event mapping must verify that:

- event order is deterministic when input ordering is deterministic
- correlation IDs and process IDs are preserved
- span nesting is reconstructed only from available trace data
- missing start/end events are represented as incomplete traces, not repaired silently
- exception information is preserved with stack trace, class, method, message, and cause where available
- trace ingestion remains safe when optional fields are missing

Runtime traces must not be rewritten to make the replay look complete when the underlying input is incomplete.

### Graph Model and Graph Persistence

Changes to graph nodes, graph edges, graph storage, query adapters, indexing, import/export, or relationship derivation must verify that:

- every edge has a documented relationship type
- derived relationships carry derivation metadata
- graph queries return stable results for stable inputs
- duplicate nodes and duplicate relationships are avoided or explicitly handled
- graph persistence does not lose provenance metadata
- graph adapters do not leak storage-specific APIs into the domain model

Graph output must distinguish between direct evidence relationships and inferred analysis relationships.

### Replay Reconstruction

Changes to replay reconstruction, runtime-path reconstruction, exception replay, source mapping, timeline generation, or step reconstruction must verify that:

- replay steps are built only from available evidence or documented derivations
- missing events are represented as gaps
- source-code references are stable and traceable
- replay order is deterministic
- exception origin and propagation are not guessed
- replay output distinguishes actual execution from reconstructed interpretation

Replay must not claim that a method, branch, or code path was executed unless the evidence supports that claim.

### Finding Generation

Changes to finding generation, severity classification, rule evaluation, aggregation, deduplication, or diagnostics must verify that:

- findings contain evidence references
- severity is deterministic for the same input
- duplicate findings are either merged by a documented rule or kept separate with reason
- findings preserve enough context for review
- false certainty is avoided when data is incomplete
- limitations are visible in the finding output

### LLM Integration

Changes to LLM prompt construction, response parsing, analysis summarization, remediation proposals, model adapters, or provider integrations must verify that:

- prompts are built from explicit evidence and context objects
- prompt construction is deterministic for the same input
- the model is not given hidden or unrelated repository data
- model output is parsed defensively
- model output is labeled as generated analysis, not evidence
- hallucinated file names, methods, classes, line numbers, or causal chains are rejected or clearly marked as unverified
- tests do not require external LLM service access unless explicitly documented as integration tests

Unit tests for LLM integration must use test doubles, fixtures, or local deterministic responses.

Do not call external LLM APIs in the default unit test suite.

### Reporting and Visualization

Changes to reports, dashboards, textual summaries, exported JSON, PlantUML, Graphviz, Mermaid, HTML, Markdown, or other visualization output must verify that:

- output is deterministic for the same input
- report entries link back to evidence or findings
- missing data is shown as missing rather than fabricated
- generated diagrams do not become the source of truth
- visualization adapters do not contain core analysis decisions

PUML, Graphviz, Mermaid, or similar formats are optional output adapters. They must not be required for the core analytics model unless explicitly defined as part of a task.

---

## Architecture Quality Rules

The project follows hexagonal architecture.

The intended dependency direction is:

```text
inbound adapters / outbound adapters / infrastructure
        -> application
        -> domain
```

The reverse direction is forbidden.

### Domain Layer

The domain layer contains:

- evidence model
- finding model
- graph relationship model
- replay model
- value objects
- domain rules
- domain ports

The domain layer must not depend on:

- Gradle API
- Maven API
- JavaParser
- database APIs
- graph database APIs
- HTTP frameworks
- LLM SDKs
- logging providers
- filesystem adapters
- runtime instrumentation implementation
- visualization libraries
- infrastructure classes

### Application Layer

The application layer contains:

- use cases
- orchestration services
- request and result objects
- analysis pipeline coordination
- port-based access to scanners, repositories, graph storage, LLM providers, and report writers

The application layer may depend on the domain layer and Java standard library.

The application layer must not depend directly on concrete infrastructure adapters.

### Adapters and Infrastructure

Adapters and infrastructure contain:

- Java source scanners
- trace importers
- graph database adapters
- file repositories
- report writers
- visualization exporters
- LLM provider adapters
- CLI, build-tool, or API entry points
- runtime integration code

Adapters may depend inward on application and domain.

Adapters must not become hidden use cases.

---

## SOLID and Architecture Checks

The repository may contain SOLID-oriented quality tests under a path such as:

```text
src/test/java/**/quality/solid/
```

Architecture tests may be located under a path such as:

```text
src/test/java/**/quality/
```

These tests are part of the quality contract.

Do not weaken SOLID, ArchUnit, package-boundary, or dependency-direction tests to make a build pass unless the task explicitly requires a documented architectural decision.

If an architecture rule is wrong, update it only with a clear justification and a dedicated test/documentation adjustment.

---

## Package Coverage Report

The `checkPackageCoverage` task parses the JaCoCo XML report, writes a per-package report, and fails when a package is below the configured repository thresholds.

Unless the repository defines a stricter policy, the expected default thresholds are:

- 80% line coverage
- 80% branch coverage when branch data exists

The report is written to:

```text
build/reports/coverage/package-coverage.txt
```

Example of a passing report:

```text
Package coverage report
Line threshold: 80.00%
Branch threshold: 80.00%
packageName	lineCoverage	branchCoverage	missedLines	missedBranches	totalLines	totalBranches
com.example.analytics.domain	86.20%	84.10%	0	1	40	20
com.example.analytics.adapter.out.graph	91.40%	n/a	2	0	25	0
```

Example of a failing result:

```text
Package coverage report
Line threshold: 80.00%
Branch threshold: 80.00%
packageName	lineCoverage	branchCoverage	missedLines	missedBranches	totalLines	totalBranches
com.example.analytics.domain	72.50%	65.00%	11	7	40	20
```

A failing package coverage report is a blocking quality-gate failure.

Do not lower thresholds to make the gate pass.

Do not exclude production code from coverage unless the task explicitly requires it and the exclusion is justified.

---

## Gradle Dependency Verification

Gradle Dependency Verification is part of the repository quality gate.

Do not disable dependency verification to make a build pass.

Do not use:

```bash
--dependency-verification off
```

If Gradle reports missing verification metadata for expected artifacts, update `gradle/verification-metadata.xml` with the existing checksum-based strategy and review the diff before committing it.

For checksum-only metadata updates, use the task that resolves the failing configuration. Example:

```bash
./gradlew --write-verification-metadata sha256 <task-that-resolves-the-failing-configuration>
```

After updating metadata, rerun the failing command and the full local quality gate with:

```bash
--dependency-verification strict
```

Checksum mismatches or signature mismatches are security-relevant blockers.

They must not be fixed by blindly regenerating metadata.

---

## External Services and Integration Tests

The default local quality gate must not require unavailable external services.

This applies to:

- LLM providers
- SonarCloud
- graph database servers
- remote repositories
- external tracing systems
- external CI services
- cloud services

Default unit and architecture tests must use deterministic fixtures, embedded stores, mocks, fakes, or test adapters.

Integration tests that require external services must be clearly separated and documented.

Do not silently skip integration behavior without reporting it.

Do not fake a successful external service result.

---

## Optional SonarCloud Check

If `SONAR_TOKEN` or `sonar.token` is available, contributors may also run:

```bash
./gradlew sonar --dependency-verification strict --console=plain --stacktrace
```

If the token is not configured locally, skip this step and report that SonarCloud was not executed.

Missing Sonar credentials are not a code failure.

A failed Sonar analysis with valid credentials is a quality blocker unless the failure is clearly caused by an external service outage.

---

## Optional Build-Tool Adapter Validation

If this repository contains Gradle plugin modules, Maven plugin modules, or other build-tool adapters, their validation tasks must be run when relevant.

When Gradle plugin metadata, task inputs, task outputs, or plugin declarations change, also run:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

If `validatePlugins` is not available because the repository is not a Gradle plugin repository, do not invent an equivalent task. Report that plugin validation is not applicable.

For Maven adapter changes, run the repository-documented Maven-adapter verification tasks if they exist. Do not invent Maven commands or switch the project to Maven as the primary quality gate.

---

## Database, Schema, and Migration Quality

If the project uses file-based stores, relational databases, graph databases, or migration scripts, changes must verify that:

- migrations are deterministic
- existing data can still be read or migrated when compatibility is required
- schema changes preserve evidence provenance
- graph relationships retain their type and derivation metadata
- rollback or recovery behavior is documented when supported
- test fixtures cover old and new schema shapes when relevant

If the repository provides migration validation tasks, run them.

If no such tasks exist, run the narrowest meaningful tests that exercise the changed persistence code and report the limitation.

---

## Test Data and Fixtures

Forensic Analytics tests should use realistic but minimal fixtures.

Fixtures should include:

- source-code samples
- runtime trace samples
- exception samples
- graph relationship samples
- replay timeline samples
- finding output samples
- LLM response samples when LLM parsing is tested

Fixtures must not contain secrets, real credentials, private customer data, production logs, private source code, or personally identifiable information unless explicitly anonymized and approved for repository use.

When anonymizing evidence fixtures, preserve the structural properties that the test depends on.

---

## Failure Policy

The quality gate fails if any required task fails.

A failing quality gate blocks commits and pushes.

Coverage thresholds must not be lowered to make the gate pass.

Dependency verification must not be disabled to make the gate pass.

Architecture tests must not be weakened to hide boundary violations.

LLM output must not be accepted as evidence unless explicitly modeled as generated analysis.

Replay output must not invent execution paths.

Graph output must not invent relationships.

If a failure cannot be fixed within the current task scope, the blocker must be documented explicitly.

Agents must stop and report when the gate fails.

Do not proceed silently.

---

## Handling Quality Gate Failures

When a quality command fails, collect and report:

- command executed
- failing task
- failing test class or test method when available
- relevant assertion or exception summary
- whether the failure was introduced by the current change
- whether the failure appears pre-existing
- whether the failure is environment-related
- whether the failure is external-service-related
- the remaining blocker

Fix all failures caused by the current change when realistically possible within the task scope.

After a fix, rerun the failing command first, then rerun the full local quality gate.

Do not claim the gate passed unless it was actually executed successfully.

---

## Commit and Push Policy

Before creating a commit, inspect:

```bash
git status
git diff
git diff --cached
```

If staged and unstaged changes both exist, inspect them separately.

Do not commit unrelated local files.

Do not commit generated build output unless explicitly required.

Do not commit `.gradle`, `build`, IDE workspace files, temporary logs, local databases, trace dumps, or generated reports unless they are intentionally part of the requested change.

A commit must clearly document:

- what changed
- why it changed
- how it changed
- affected domain, application, adapter, infrastructure, or test areas
- whether evidence, graph, replay, finding, LLM, report, or visualization behavior changed
- which verification commands were executed
- whether any known limitations or blockers remain

---

## Final Report Requirements

At the end of every task, report:

1. files changed
2. main changes made
3. tests or verification commands executed
4. commands that failed, if any
5. quality gate result
6. SonarCloud result or explicit skip reason
7. known limitations
8. remaining blockers, if any

Do not claim success for unexecuted verification.

If no files were changed, say so explicitly.

---

## Notes

- Quality checks should prefer lightweight deterministic tests over external service dependencies.
- Dependency verification metadata is committed in `gradle/verification-metadata.xml` and must be updated only when expected build dependencies or resolved source artifacts change.
- The analytics model must keep evidence and generated interpretation separate.
- The graph model must preserve relationship provenance.
- The replay model must represent incomplete traces explicitly.
- LLM output is useful for explanation and remediation support, but it is not primary evidence.
- Visualization output is an adapter concern and must not become the authoritative analytics model.
- To tighten SOLID or architecture rules, adjust the relevant quality tests or extend the support utilities with clear justification.
