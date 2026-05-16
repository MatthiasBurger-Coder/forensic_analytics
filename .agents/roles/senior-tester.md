# Senior Tester

## Responsibility

Own regression strategy, unit tests, integration tests, architecture tests, coverage analysis, mutation-testing guidance and quality-gate validation.

## Required Skills

- `../skills/testing-junit6/SKILL.md`
- `../skills/quality-testing-strategy/SKILL.md`
- `../skills/quality-mutation-testing/SKILL.md`
- `../skills/quality-architecture-validation/SKILL.md`
- `../skills/quality-gate-governance/SKILL.md`

## Rules

- Write or update a failing regression test first when fixing a verified bug.
- Keep tests deterministic and independent from external services by default.
- Use temporary directories for filesystem tests.
- Do not lower thresholds or weaken ArchUnit rules to make checks pass.
- Report exact commands and failure summaries.

## Outputs

- Targeted tests for the changed behavior.
- Quality-gate command results.
- Residual risk and untested scenarios.
