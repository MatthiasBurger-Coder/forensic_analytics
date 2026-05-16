# Commit Rules

## Required Commit Message Content

- Summary
- Why
- What changed
- Added or changed skills
- Updated governance
- Updated prompts or workflow docs
- Validation performed
- Risks and follow-ups

## Required Evidence

- changed-file review
- quality gate status
- diff check status
- unresolved risks
- commit scope

## Rules

- Stage explicit files only.
- Do not stage unrelated user-owned changes.
- Do not commit with failed required gates.
- Do not include generated or cache output unless explicitly required.

## STOP Rules

Stop when:

- commit message is incomplete;
- required quality evidence is missing;
- staged files include unrelated changes;
- commit would mix unrelated scopes.
