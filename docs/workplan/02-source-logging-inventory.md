# Source Logging Inventory

## Source Package

Inspected package:

```text
de.burger.it.scxmlexample.infrastructure.logging
```

The package contains:

| Source class | Responsibility | Portability |
|---|---|---|
| `CentralLoggingAspect` | Around advice for `@Loggable` methods/classes | Not directly portable because it depends on Spring AOP and AspectJ |
| `Loggable` | Runtime annotation with `org.slf4j.event.Level` value | Partly portable, but annotation use in application/domain would violate current boundary intent |
| `LevelLogger` | Strategy interface for level-specific logging | Portable if package and logger dependency are adapted |
| `LevelLoggerRegistry` | EnumMap from SLF4J `Level` to strategies | Portable, but Spring `@Component` must be removed |
| `CorrelationIdManager` | MDC-backed correlation ID lifecycle | Portable with careful inbound-scope ownership rules |
| `TraceLevelLogger` | Trace entry/exit/exception strategy | Sensitive because it logs args/results |
| `DebugLevelLogger` | Debug entry/exit/exception strategy | Sensitive because it logs args/results |
| `InfoLevelLogger` | Info entry/exit/exception strategy without args/results | Safer baseline |
| `WarnLevelLogger` | Warn entry/exit/exception strategy without args/results | Safer baseline |
| `ErrorLevelLogger` | Error exception-only strategy | Safer baseline |

## Source Behavior

`CentralLoggingAspect` does the following:

- initializes a correlation ID through `CorrelationIdManager.initCorrelationId()`
- resolves the target class and annotated method
- selects a `LevelLogger` from `LevelLoggerRegistry`
- logs entry, exit, duration, and exceptions
- clears the correlation ID only when the current invocation created it

`CorrelationIdManager` stores IDs under the MDC key:

```text
correlationId
```

## Source Risks

The source implementation is useful but not safe to copy directly.

Main risks:

- Spring AOP is not part of the verified Forensic Analytics runtime.
- AspectJ introduces implicit control flow and dependency surface.
- Trace and debug strategies log arguments and return values, which can expose source code, runtime values, credentials, stack traces, or repository paths.
- The aspect's anonymous fallback annotation silently substitutes `DEBUG` if no annotation is found.
- The source log messages use non-ASCII symbols; repository edits should stay ASCII unless there is a clear reason.
- MDC context does not automatically cross async boundaries, executor threads, gRPC callbacks, or background workers.

## Portable Pieces

The following source ideas can be adapted:

- correlation ID ownership and cleanup
- level strategy map
- duration measurement around explicit adapter operations
- exception logging at adapter failure boundaries

The following should be postponed or redesigned:

- annotation-driven logging
- method argument/result logging
- Spring component scanning
- AspectJ weaving
- fallback annotation creation

## Source Authority Stop Point

Because the requested `logging.zip` was not found, implementation must stop if the user or project requires the ZIP bytes as the authoritative source. The unzipped source package is sufficient only if accepted as the intended source material.
