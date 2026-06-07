---
name: architecture-hexagonal
description: Use for architecture work that must preserve hexagonal boundaries, domain isolation, ports/adapters separation, and evidence semantics.
---

# Hexagonal Architecture

## Purpose

Protect the current project core from technical frameworks, storage clients, runtime infrastructure, UI decisions and provider-specific SDKs.

## Practices

- Keep domain models and domain services independent from adapters and infrastructure.
- Keep application services focused on use cases and ports.
- Put CLI, gRPC, persistence, scanner, Joern, LLM and reporting integrations in adapter or infrastructure modules.
- Express cross-boundary behavior through explicit request, result and port types.
- Prefer declarative orchestration and named policies over imperative control-flow blocks when the use case is selecting, validating, mapping or projecting evidence.
- Use Strategy Pattern behind ports when behavior varies by provider, storage backend, scanner, parser, worker, artifact type or report/export format.
- Keep strategy interfaces in the owning domain or application layer only when they express business behavior; keep technical strategies in adapters or infrastructure.
- Keep evidence provenance and uncertainty visible across layer boundaries.

## Verification

- Inspect package dependencies before changes.
- Add or update architecture tests for boundary-sensitive work.
- Run targeted tests first, then the applicable gate from `QUALITY.md`.
