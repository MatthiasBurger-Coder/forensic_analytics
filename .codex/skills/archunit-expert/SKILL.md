---
name: archunit-expert
description: Use when adding, reviewing, or fixing Forensic Analytics ArchUnit architecture rules for hexagonal boundaries, forbidden dependencies, package ownership, build-tool adapter isolation, graph provider isolation, and LLM provider isolation.
---

# ArchUnit Expert

Use this skill for architecture tests, forbidden dependencies, package-boundary checks, dependency direction, and quality-gate architecture validation.

## Authoritative Sources

- Root `AGENTS.md`
- Root `QUALITY.md`
- `.agents/skills/forensic-architecture-archunit-hexagonal/SKILL.md`
- `.agents/skills/forensic-quality-architecture-validation/SKILL.md`
- `.agents/roles/senior-tester.md`

## Rules

- Domain code must not depend on application, adapters, infrastructure, provider APIs, Gradle APIs, Maven APIs, parser APIs, graph clients, or LLM SDKs.
- Application code may depend on domain and ports, not concrete adapters.
- Adapters may depend inward on application and domain contracts.
- Build-tool adapter APIs must stay inside build-tool adapter packages.
- Do not weaken ArchUnit rules to make a change pass.

## Verification

Locate existing ArchUnit tests before adding rules. Run affected architecture tests and the applicable quality gate from `QUALITY.md` when architecture behavior changes.
