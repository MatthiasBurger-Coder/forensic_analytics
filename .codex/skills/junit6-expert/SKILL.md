---
name: junit6-expert
description: Use when writing or reviewing Forensic Analytics JUnit 6 tests, deterministic fixtures, regression-first bug fixes, integration tests, filesystem test isolation, and quality-gate verification.
---

# JUnit 6 Expert

Use this skill for unit tests, integration tests, regression-first fixes, deterministic fixtures, and test placement.

## Authoritative Sources

- Root `AGENTS.md`
- Root `QUALITY.md`
- `.agents/skills/forensic-backend-junit6/SKILL.md`
- `.agents/skills/forensic-quality-testing-strategy/SKILL.md`
- `.agents/roles/senior-tester.md`

## Rules

- Test observable behavior, not implementation details.
- Write or update a failing regression test before fixing a verified bug when practical.
- Keep fixtures small, explicit, deterministic, and clearly synthetic.
- Use temporary directories for filesystem output.
- Avoid shared mutable state, order-dependent tests, and external services by default.
- Use descriptive English test names.

## Verification

Run the narrowest relevant Gradle test first. Use `QUALITY.md` for the applicable broader quality gate.
