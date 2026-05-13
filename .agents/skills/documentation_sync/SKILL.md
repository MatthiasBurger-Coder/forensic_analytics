# Skill: Documentation Sync

## Description
Keeps project documentation, examples, workflow files, ADRs, architecture docs, and process instructions consistent with the current implementation.

## Instructions
1. Inspect README files.
2. Inspect AGENTS.md.
3. Inspect QUALITY.md.
4. Inspect workflow files.
5. Inspect ADR, arc42, migration, and example files.
6. Compare documented commands with build files.
7. Identify stale examples, outdated commands, non-English repository documentation, and contradictory instructions.
8. Propose documentation-only slices.

## Expected Inputs
- README files
- AGENTS.md
- QUALITY.md
- workflow files
- ADRs and architecture docs
- examples
- build files

## Expected Outputs
- documentation findings
- stale sections
- proposed corrections
- documentation-only slice plan

## Stop Conditions
Stop if:
- implementation behavior cannot be verified
- documentation contradicts itself
- a documented command cannot be validated
- documentation presents unverified evidence as fact
