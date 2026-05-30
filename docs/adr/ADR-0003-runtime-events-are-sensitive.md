# ADR-0003: Runtime events are sensitive by default

## Status

Accepted

## Context

Runtime events may contain secrets, personal data or business-critical values.

## Decision

Runtime values must be treated as sensitive by default. Redaction, masking, hashing, allowlisting and retention rules must be applied before unsafe persistence or indexing.

## Consequences

- Security is part of the core model.
- Event ingestion must validate and redact data.
- Vector indexing must never receive secrets.
