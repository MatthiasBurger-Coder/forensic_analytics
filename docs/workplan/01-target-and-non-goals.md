# Target And Non-Goals

## Target Outcome

The integration should produce a small observability/logging capability that can be used by inbound adapters and bootstrap code without changing forensic evidence semantics.

The target behavior is:

- create or preserve a correlation ID at inbound boundaries
- expose the correlation ID to logging infrastructure for the current request or command
- log adapter entry, successful completion, and failure summaries
- avoid logging raw payload bytes, runtime values, repository credentials, local filesystem details, or unsanitized diagnostics
- keep log output deterministic in message shape and field names
- make tests independent from a live logging backend or external service

## Preferred Integration Shape

The preferred baseline is an adapter-scoped logging utility rather than direct Spring AOP adoption.

The current repository does not use Spring. The source package's `CentralLoggingAspect` depends on:

- `org.aspectj.lang.*`
- `org.aspectj.lang.annotation.*`
- `org.springframework.stereotype.Component`
- `org.slf4j.*`

Introducing Spring AOP or AspectJ would be a cross-cutting architecture decision, not a mechanical copy. The first implementation should therefore extract portable behavior into a repository-owned observability package or module and wire it explicitly at adapter boundaries.

## Explicit Non-Goals

Do not implement these in the initial integration:

- no domain or application annotations for logging
- no logging of method parameters or return values from core analysis, ingestion payloads, runtime traces, source content, stack traces, or LLM prompts
- no Spring dependency unless a dedicated architecture slice accepts it
- no AspectJ weaving or proxy system unless explicitly approved and tested
- no concrete logging provider such as Logback, Log4j2 binding, `slf4j-log4j12`, or `slf4j-reload4j`
- no broad rewrite of REST, gRPC, CLI, bootstrap, persistence, or analysis flows
- no runtime event inference from logs
- no treating logs as verified forensic evidence
- no changes to public gRPC schema unless a separate API-contract slice verifies and documents the change

## Evidence Semantics

Logs are operational diagnostics. They must not become canonical analysis facts unless a future, explicit evidence-ingestion feature models them as raw evidence with provenance, sensitivity, retention, and verification status.

The initial integration must keep these categories separate:

- operational logs
- audit events
- runtime trace evidence
- static analysis facts
- replay-derived facts
- LLM-generated hypotheses

## User-Facing Scope

This workplan prepares implementation. It does not implement the logging integration by itself.
