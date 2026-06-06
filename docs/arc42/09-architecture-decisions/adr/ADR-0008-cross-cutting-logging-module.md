# ADR-0008: Cross-cutting logging module with Spring method interception

## Status

Accepted

Post-S05 closure note: ADR-0022 retires the `forensic-analytics-logging` and
`forensic-analytics-observability` source trees as active implementation
source. This ADR remains historical context for the predecessor monolith logging
exception; productive services must use service-local diagnostics or
`observability-stack` deployment material without shared Java logging modules.

## Context

ADR-0005 created `forensic-analytics-observability` as a framework-neutral operational logging boundary and explicitly deferred Spring-specific method logging. ADR-0006 kept Spring project-code dependencies inside `forensic-analytics-boot-app` until a later decision named another module. ADR-0022/S05 later retired both source trees as active implementation inputs.

At original acceptance time, the platform needed low-effort operational logging across Spring-wired application beans and an injectable logger wrapper that could be used from arbitrary outer-layer classes without copying logging code into each adapter.

Runtime traces, source content, stack traces, LLM prompts, credentials and method payloads remain sensitive. Automatic logging must therefore record operational method names, phases, duration, correlation IDs and exception categories only. It must not log method arguments, return values, raw exception messages, stack frames, evidence payloads or source content.

## Decision

The original decision introduced `forensic-analytics-logging` as a cross-cutting infrastructure module.

The predecessor module owned:

- `ForensicLogger` and `ForensicLoggerFactory` as the injectable logger wrapper API
- sanitized event and operation logging backed by JDK `System.Logger`
- Spring Boot auto-configuration metadata for method interception
- `@ForensicLoggable` for explicit method or type-level logging level selection
- configuration under `forensics.analytics.logging.*`

The predecessor module could depend on `forensic-analytics-observability` for correlation context reuse. This kept one correlation model across explicit operation logging and automatic method logging.

The predecessor module could depend on Spring Framework AOP and Spring Context as a named exception to ADR-0006. It could publish Spring Boot auto-configuration metadata so the Boot runtime could discover it. It was not allowed to depend on AspectJ annotations or weaving, SLF4J, Logback, Log4j2, Micrometer, OpenTelemetry, domain, application, adapters, persistence, REST, gRPC or generated Protobuf classes.

The predecessor `forensic-analytics-boot-app` could depend on `forensic-analytics-logging` so the auto-configuration was available to the Spring Boot runtime.

The predecessor `forensic-analytics-domain` and `forensic-analytics-application` modules were not allowed to depend on `forensic-analytics-logging`.

## Consequences

- Cross-cutting method logging was available in the predecessor Boot runtime without adding Spring dependencies to domain or application modules.
- The predecessor `forensic-analytics-observability` boundary remained framework-neutral.
- Historical architecture tests allowed Spring dependencies in `de.burger.forensics.analytics.logging.spring..` only for this accepted exception and kept the accepted packages limited to Spring AOP, Beans, Context and Core.
- Logging remains diagnostic output, not verified forensic evidence.
- Final classes without stable interfaces cannot be method-proxied safely and should use `ForensicLoggerFactory` directly when they need special logging.
