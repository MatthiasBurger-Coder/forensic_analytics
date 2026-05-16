---
name: hexagonal-architecture-expert
description: Use for Forensic Analytics architecture work that must preserve hexagonal boundaries, domain isolation, application use cases, ports/adapters separation, dependency direction, and evidence semantics.
---

# Hexagonal Architecture Expert

Use this skill for architecture work involving domain, application, ports, adapters, infrastructure, dependency direction, or evidence semantics.

## Authoritative Sources

- Root `AGENTS.md`
- `.agents/skills/forensic-architecture-hexagonal/SKILL.md`
- `.agents/skills/forensic-architecture-modular-monorepo/SKILL.md`
- `.agents/roles/senior-system-architect.md`

## Rules

- Keep domain code independent from application, adapters, infrastructure, provider SDKs, filesystem adapters, graph clients, parser APIs, and UI code.
- Keep application code depending on domain and ports, not concrete adapters.
- Keep adapters thin and dependent inward.
- Preserve evidence provenance and explicit unknown or incomplete states across boundaries.
- Add or update architecture tests for boundary-sensitive changes.

## Verification

Inspect current packages, module dependencies, ArchUnit tests, and affected build files before changing architecture. Stop when a boundary or dependency cannot be verified.
