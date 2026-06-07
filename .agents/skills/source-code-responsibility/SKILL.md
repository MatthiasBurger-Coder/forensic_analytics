---
name: source-code-responsibility
description: Use when creating, reviewing, or reworking source code to enforce one class one responsibility, one method one responsibility, one variable one meaning, declarative before imperative programming, Strategy Pattern for varying behavior, small-file discipline, and IF-less design before implementation or during class-size rework.
---

# Source Code Responsibility

## Purpose

Keep new and reworked source code small, focused, readable and testable without speculative architecture migration.

Use this skill with Java/backend, frontend, adapter, persistence, parser, orchestration, test, and configuration-code slices when source code is created or an existing class is large enough to require responsibility review.

## Core Rules

- One class has one responsibility and one reason to change.
- One method performs one operation at one abstraction level.
- One variable has one meaning for its whole lifetime.
- Prefer declarative programming before imperative programming when the behavior can be expressed as data transformation, mapping, composition, pipeline, specification, policy or configuration.
- Use the Strategy Pattern for behavior that varies by evidence type, worker kind, storage backend, parser, provider, transport, status, artifact type or output format.
- Prefer table-driven, polymorphic or strategy-based code over nested conditionals.
- Do not remove `if` statements mechanically; replace conditionals only when the replacement makes responsibilities clearer and behavior easier to test.
- Keep forensic evidence categories explicit. Do not hide unknowns, unresolved references or missing runtime facts behind convenience defaults.

## Read-Only Audit Before Coding

Before creating or reworking source code:

1. Inspect the target file and direct collaborators.
2. Count approximate file size and identify mixed responsibilities.
3. Identify methods that combine validation, mapping, I/O, orchestration, persistence, formatting or error handling.
4. Identify variables whose names or reassignment mix different concepts.
5. Identify conditional clusters that encode dispatch, status mapping, validation policy or type-specific behavior.
6. Check whether an imperative loop, mutation-heavy block or branching cluster can be expressed as a declarative pipeline, mapper, collector, policy table or strategy set without hiding evidence semantics.
7. Confirm the exact symbols and contracts before changing anything.

## Size Triggers

Treat these as review triggers, not automatic failures:

- Production class over 250 lines: check for split candidates.
- Production class over 500 lines: require an explicit responsibility decision before adding more behavior.
- Method over 40 lines: check whether it mixes steps or abstraction levels.
- Method over 80 lines: require extraction or a documented reason to keep it intact.
- More than 5 branch points in one method: check for policy object, enum behavior, lookup table, mapper or strategy.
- More than 2 behavior variants selected by conditionals: check for Strategy Pattern.
- Repeated loops that filter, map, group, validate or project data: check for declarative collection processing or named pipeline steps.
- Test files may be larger, but fixture setup, assertions and scenario construction should still be separated.

## Split Patterns

Use the smallest safe extraction:

- gRPC/HTTP endpoints: keep transport methods in the endpoint; extract request mappers, response mappers, error mappers and pagination/token helpers.
- Persistence adapters: keep port implementation and transaction flow in the adapter; extract schema initialization, SQL statements, row mapping and JSON codecs.
- Domain container classes: split stable value objects, validation policies, identity parsing and domain operations into focused types only after callers are verified.
- Orchestration services: keep use-case flow visible; extract command construction, status projection, diagnostics mapping and idempotency key generation.
- Configuration descriptors such as Compose files: one service or scenario per file; avoid duplicating divergent service definitions across scenario and service descriptors.

## Declarative First

Use declarative programming before imperative programming when it preserves clarity and forensic semantics:

- Prefer named transformations over step-by-step mutation.
- Prefer immutable intermediate values over mutable accumulators.
- Prefer stream, collection, collector, mapper or specification objects for filtering and projection.
- Prefer explicit policy tables for closed mappings such as status, worker kind, artifact type or completeness.
- Keep loops when they are clearer for I/O, resource cleanup, short-circuiting, checked exceptions or ordered side effects.
- Do not compress complex evidence logic into dense streams when named methods would be clearer.

## Strategy Pattern

Use Strategy Pattern when behavior varies by a stable axis:

- Define a small interface named after the behavior, not after the caller.
- Implement one strategy per variant or provider.
- Select strategies through constructor injection, explicit registry, enum-backed registry or `Map<Key, Strategy>`.
- Keep strategy selection at the adapter/application boundary; do not let domain code depend on framework containers.
- Test strategy selection and at least one representative strategy behavior.
- Do not introduce Strategy Pattern for a one-off branch with no realistic second variant.

## IF-Less Rework Options

Prefer these when they reduce responsibility mixing:

- Enum methods for state-specific conversion or policy.
- `Map<Key, Handler>` dispatch for known closed sets.
- Strategy interfaces for behavior that varies by type, provider or storage backend.
- Guard clauses for mandatory validation instead of nested `if` blocks.
- Dedicated validator or mapper types when validation or mapping dominates a class.
- Explicit result objects for unknown, incomplete or unsupported evidence instead of fallback branches that invent data.

## Stop Conditions

Stop and report when:

- a split would require guessing public API, generated type, schema, event field, graph label or Gradle task names;
- callers cannot be verified exactly;
- the refactor would silently change forensic evidence semantics;
- the change would introduce compatibility aliases or fallback behavior not requested by the task;
- class-size cleanup would become an unrequested architecture migration.

## Verification

- Add or update targeted tests for behavior-preserving extraction when production code changes.
- Run the narrowest affected test first.
- Run the applicable quality gate from `QUALITY.md` before commit readiness.
- For governance-only use of this skill, run `git diff --check` and registry/routing consistency checks.
