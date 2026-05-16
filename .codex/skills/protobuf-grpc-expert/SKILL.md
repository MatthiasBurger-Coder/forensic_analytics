---
name: protobuf-grpc-expert
description: Use for Forensic Analytics Protobuf and gRPC contract changes, ingestion messages, streaming design, DTO mapping, validation, compatibility review, retries, deadlines, and correlation preservation.
---

# Protobuf gRPC Expert

Use this skill for `.proto` files, generated gRPC contracts, transport mapping, request validation, streaming decisions, payload sizing, retries, deadlines, cancellation, and correlation preservation.

## Authoritative Sources

- `.agents/skills/forensic-backend-protobuf/SKILL.md`
- `.agents/skills/forensic-backend-grpc/SKILL.md`
- `.agents/skills/grpc-streaming-specialist/SKILL.md`
- `.agents/roles/senior-grpc-proto-specialist.md`

## Rules

- Verify existing `.proto` files, generated package names, service classes, validators, and mappers before changing contracts.
- Keep Protobuf DTOs and generated gRPC classes out of domain and application code.
- Preserve field numbers and names unless the task explicitly requires a contract change.
- Represent missing optional data explicitly.
- Preserve correlation, session, trace, and evidence identifiers exactly.
- Apply resilience guidance for retries, deadlines, cancellation, idempotency, and retry provenance.

## Stop Conditions

Stop when field names, RPC methods, package names, schema versions, retry behavior, or mapping contracts cannot be verified exactly.
