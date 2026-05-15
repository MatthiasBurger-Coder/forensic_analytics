# Architecture And Security Constraints

## Hexagonal Boundary

Logging integration must keep dependency direction intact:

```text
bootstrap / adapters / infrastructure
        -> application
        -> domain
```

Domain and application code must not depend on Spring, AspectJ, concrete logging providers, gRPC implementation classes, REST implementation classes, or persistence adapters.

## Allowed Initial Ownership

Logging implementation may be owned by:

- a new infrastructure/observability module, if the module-boundary slice verifies and adds it
- inbound adapters such as REST, gRPC, and CLI for boundary-specific logging
- bootstrap wiring for server lifecycle logs

Logging implementation must not be owned by:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- canonical analysis model classes
- runtime evidence model classes

## Runtime Data Sensitivity

ADR-0003 and arc42 require runtime data to be treated as sensitive by default.

Initial log messages must not include:

- raw runtime payloads
- method arguments
- return values
- source file contents
- repository URLs containing credentials or user info
- local filesystem paths unless sanitized
- stack frames unless explicitly sanitized
- LLM prompt content or model output
- secrets, tokens, passwords, credentials, or API keys

## Correlation Semantics

Correlation IDs in operational logs help diagnose requests. They are not forensic evidence by themselves.

The initial integration should:

- preserve REST `X-Correlation-Id` when present
- generate a correlation ID when absent
- use the existing response header behavior in REST
- introduce an explicit adapter-level context object or scope for correlation lifecycle
- avoid guessing gRPC correlation IDs from unrelated fields

The gRPC proto currently has `request_id`, `session_id`, and build identity fields. None is a verified generic correlation ID. Do not silently treat them as `correlationId` without a dedicated contract decision.

## Dependency Constraints

Adding a logging API dependency requires a dedicated dependency slice.

Before adding a dependency, verify:

- whether the JDK alone is sufficient for the planned behavior
- whether `slf4j-api` is required to preserve the source logging system design
- whether strict Gradle dependency verification requires metadata changes
- whether the dependency would leak into domain or application modules
- whether a concrete logging binding would be introduced transitively

Do not add concrete logging providers unless explicitly requested and documented.

## Architecture Test Expectation

If a shared logging module is added, architecture tests should verify that:

- domain does not depend on the logging module
- application does not depend on the logging module
- logging implementation does not depend on domain behavior to make logging decisions
- gRPC adapter still does not depend on persistence
- REST and gRPC adapters only use logging through approved adapter or infrastructure boundaries
