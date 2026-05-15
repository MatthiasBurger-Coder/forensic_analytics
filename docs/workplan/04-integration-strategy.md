# Integration Strategy

## Strategy Summary

Use an explicit adapter-boundary logging strategy first. Treat the SCXML source package as a design input, not as a direct copy target.

The baseline design is:

```text
REST/gRPC/CLI/Bootstrap boundary
  -> correlation scope
  -> sanitized adapter log event
  -> logging facade or level strategy
  -> logging API
```

Domain and application services continue to return explicit results, diagnostics, and errors. They do not write operational logs in the initial integration.

## Proposed Components

The implementation slice should verify and then create a small infrastructure-owned logging package or module with components equivalent to:

- `CorrelationId`
- `CorrelationScope`
- `CorrelationContext`
- `OperationLogEvent`
- `OperationLogger`
- `LevelLogger`
- `LevelLoggerRegistry`
- level strategies for info, warn, and error

Trace/debug argument and result logging should be excluded from the initial baseline.

## REST Integration

REST already handles `X-Correlation-Id` locally. The REST slice should:

- move correlation ID lifecycle into the shared correlation scope only after tests capture current behavior
- keep response header compatibility with current REST responses
- log request method, route category, status category, duration, and sanitized failure code
- avoid logging request bodies or raw diagnostics

The REST adapter can be the first production integration because its correlation ID behavior is already verified in source.

## gRPC Integration

The gRPC slice should:

- log method name, status category, duration, and sanitized failure class/category
- keep gRPC status conversion unchanged
- avoid guessing correlation IDs from `request_id` or `session_id`
- optionally create a generated operational correlation ID for log scope only

Adding a gRPC metadata correlation header is a separate API-contract decision and must not be bundled into the baseline slice.

## CLI Integration

The CLI slice should:

- create a command-level correlation scope
- log command name, exit category, duration, and sanitized exception category
- keep stdout/stderr contract unchanged
- avoid logging command payload file content

## Bootstrap Integration

The bootstrap slice should:

- log server startup and shutdown lifecycle
- log enabled/disabled server decisions without exposing sensitive configuration
- preserve current shutdown behavior
- avoid converting startup failures into success logs

## Aspect And Annotation Decision

The source `@Loggable` and `CentralLoggingAspect` should be handled by a later decision slice only.

That slice must decide whether Forensic Analytics wants:

- no annotation-driven logging
- a repository-owned annotation used only in adapter/infrastructure packages
- AspectJ/Spring AOP adoption with explicit architecture documentation

Until that decision is accepted, do not add Spring AOP, AspectJ, or broad annotation usage.

## Dependency Direction

If a new module is created, preferred dependency direction is:

```text
forensic-analytics-rest
forensic-analytics-ingestion-grpc
forensic-analytics-cli
forensic-analytics-bootstrap
        -> forensic-analytics-observability
```

The observability module must not depend on REST, gRPC, CLI, bootstrap, application, persistence, Joern, JavaParser, or generated protobuf packages.

If the module name changes during implementation, update this workplan only after verifying the selected module path.
