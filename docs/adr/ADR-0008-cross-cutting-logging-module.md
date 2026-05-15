# ADR-0008: Cross-cutting logging module with Spring method interception

## Status

Accepted

## Context

ADR-0005 created `forensic-analytics-observability` as a framework-neutral operational logging boundary and explicitly deferred Spring-specific method logging. ADR-0006 kept Spring project-code dependencies inside `forensic-analytics-boot-app` until a later decision named another module.

The platform now needs low-effort operational logging across Spring-wired application beans and an injectable logger wrapper that can be used from arbitrary outer-layer classes without copying logging code into each adapter.

Runtime traces, source content, stack traces, LLM prompts, credentials and method payloads remain sensitive. Automatic logging must therefore record operational method names, phases, duration, correlation IDs and exception categories only. It must not log method arguments, return values, raw exception messages, stack frames, evidence payloads or source content.

## Decision

Introduce `forensic-analytics-logging` as a cross-cutting infrastructure module.

The module owns:

- `ForensicLogger` and `ForensicLoggerFactory` as the injectable logger wrapper API
- sanitized event and operation logging backed by JDK `System.Logger`
- Spring Boot auto-configuration metadata for method interception
- `@ForensicLoggable` for explicit method or type-level logging level selection
- configuration under `forensics.analytics.logging.*`

The module may depend on `forensic-analytics-observability` for correlation context reuse. This keeps one correlation model across explicit operation logging and automatic method logging.

The module may depend on Spring Framework AOP and Spring Context as a named exception to ADR-0006. It may publish Spring Boot auto-configuration metadata so the Boot runtime can discover it. It must not depend on AspectJ annotations or weaving, SLF4J, Logback, Log4j2, Micrometer, OpenTelemetry, domain, application, adapters, persistence, REST, gRPC or generated Protobuf classes.

`forensic-analytics-boot-app` may depend on `forensic-analytics-logging` so the auto-configuration is available to the Spring Boot runtime.

`forensic-analytics-domain` and `forensic-analytics-application` must not depend on `forensic-analytics-logging`.

## Consequences

- Cross-cutting method logging is available in the Boot runtime without adding Spring dependencies to domain or application modules.
- The existing `forensic-analytics-observability` boundary remains framework-neutral.
- Architecture tests must allow Spring dependencies in `de.burger.forensics.analytics.logging.spring..` only for this accepted exception and keep the accepted packages limited to Spring AOP, Beans, Context and Core.
- Logging remains diagnostic output, not verified forensic evidence.
- Final classes without stable interfaces cannot be method-proxied safely and should use `ForensicLoggerFactory` directly when they need special logging.
