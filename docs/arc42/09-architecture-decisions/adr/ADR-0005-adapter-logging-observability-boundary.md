# ADR-0005: Adapter logging uses a JDK observability boundary

## Status

Accepted. Partially extended by ADR-0008 for the separate cross-cutting
predecessor `forensic-analytics-logging` module. ADR-0022/S05 later retired
the `forensic-analytics-observability` and `forensic-analytics-logging` source
trees as active implementation source; this ADR remains historical context for
the logging boundary and diagnostics-as-not-evidence rule.

## Context

Forensic Analytics needs operational logs for inbound requests, CLI commands, server lifecycle events, adapter execution, engine execution, and persistence write boundaries. The logging source material inspected from the SCXML example project used Spring AOP, AspectJ, SLF4J MDC and annotation-driven method logging.

The current Forensic Analytics architecture does not use Spring wiring, and runtime/source evidence may contain sensitive values. Logging method arguments, return values, raw payloads or stack traces would risk exposing evidence or secrets and could blur the distinction between operational diagnostics and verified forensic evidence.

## Decision

The original decision created a dedicated `forensic-analytics-observability`
module with a small JDK-based logging boundary.

The predecessor module owned:

- correlation ID scope management
- structured operation event formatting
- a minimal operation logger facade backed by `System.Logger`

Adapter, engine, persistence, ingestion-request and bootstrap code could depend
on this predecessor module. Domain and application code was not allowed to
depend on it.

Do not introduce Spring AOP, AspectJ, SLF4J, Logback, Log4j2 or annotation-driven logging in this slice. Do not log method arguments, return values, payload bytes, source content, LLM prompts or raw exception messages.

## Consequences

- Logging is explicit at operational boundaries instead of implicit through method weaving.
- Strict dependency verification is unchanged because no external logging dependency is added.
- Correlation IDs support operational diagnostics but are not treated as canonical evidence.
- Future SLF4J, OpenTelemetry or logging-provider changes still require a separate architecture and dependency decision. ADR-0008 accepted Spring method interception only in the predecessor `forensic-analytics-logging`; active services must use service-local diagnostics or `observability-stack` deployment material.
