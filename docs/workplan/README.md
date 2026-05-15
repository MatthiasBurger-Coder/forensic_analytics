# Logging System Integration Workplan

## Goal

Integrate the logging system supplied from the SCXML example project into Forensic Analytics without weakening evidence integrity, hexagonal boundaries, dependency verification, or runtime-data sensitivity rules.

The source ZIP referenced by the request was not present at the supplied path. The equivalent unzipped source package was found and inspected under:

```text
D:/Projects/SCXMLExample/src/main/java/de/burger/it/scxmlexample/infrastructure/logging
```

The workplan treats that inspected source package as the current source material. If the ZIP file itself is required as the authoritative input, execution must stop until the ZIP path is corrected.

## Target Outcome

After this plan is executed, Forensic Analytics has a small, testable observability/logging layer that:

- preserves incoming REST correlation IDs and generates explicit IDs when missing
- can attach correlation IDs to adapter-level logs
- logs inbound adapter lifecycle and failure boundaries without logging raw sensitive evidence
- keeps domain and application packages free from logging framework dependencies
- avoids Spring AOP and AspectJ unless a later architecture decision explicitly accepts them
- keeps concrete logging providers out of the repository unless explicitly approved

## Implementation Status

This workplan has been executed for the initial adapter-scoped observability slice.

Implemented:

- `forensic-analytics-observability` module
- JDK `System.Logger` operation logging facade
- correlation ID scope handling
- REST operation logging with existing `X-Correlation-Id` behavior preserved
- gRPC operation logging without proto changes or correlation inference from request/session fields
- CLI command logging without stdout/stderr contract changes
- bootstrap lifecycle logging without startup/shutdown behavior changes
- ArchUnit guardrails for logging boundary dependencies
- ADR and arc42 documentation synchronization

Not implemented:

- Spring AOP
- AspectJ
- SLF4J, Logback or Log4j2 dependencies
- annotation-driven `@Loggable` usage
- method argument or return-value logging
- treating logs as canonical forensic evidence

## Workplan Files

1. [00-verified-baseline.md](00-verified-baseline.md) - verified repository, quality, source, and architecture facts.
2. [01-target-and-non-goals.md](01-target-and-non-goals.md) - target integration shape and explicit non-goals.
3. [02-source-logging-inventory.md](02-source-logging-inventory.md) - source logging package inventory and portability assessment.
4. [03-architecture-and-security-constraints.md](03-architecture-and-security-constraints.md) - boundaries, sensitivity rules, and dependency constraints.
5. [04-integration-strategy.md](04-integration-strategy.md) - proposed adapter-scoped integration approach.
6. [05-implementation-slices.md](05-implementation-slices.md) - ordered implementation slices with owners, write scopes, and stop points.
7. [06-dependency-graph-and-parallelization.md](06-dependency-graph-and-parallelization.md) - slice dependency graph and parallel work notes.
8. [07-quality-gates.md](07-quality-gates.md) - targeted and full verification commands.
9. [08-documentation-sync.md](08-documentation-sync.md) - documentation and ADR synchronization points.
10. [09-stop-conditions-and-uncertainty.md](09-stop-conditions-and-uncertainty.md) - conditions that require stopping instead of guessing.
11. [10-commit-and-push-plan.md](10-commit-and-push-plan.md) - commit preparation notes if implementation is later requested.

## Execution Rule

This workplan is planning material only. It does not authorize speculative implementation. Each slice must begin with read-only verification of the exact files it will touch.
