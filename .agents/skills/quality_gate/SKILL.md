# Skill: Quality Gate

## Description
Identifies and executes the repository quality gate without weakening existing verification rules.

## Instructions
1. Read QUALITY.md.
2. Inspect build files.
3. Identify the correct build tool.
4. Identify test, coverage, validation, and dependency-verification commands.
5. Execute commands in the host-appropriate shell environment: use WSL on Windows hosts and native shell access on Linux hosts.
6. Stop and report if WSL is unavailable on Windows or cannot access the worktree.
7. Run checks where feasible.
8. Summarize pass/fail results.
9. Report exact failing commands.

## Expected Inputs
- QUALITY.md
- build.gradle or build.gradle.kts
- settings.gradle or settings.gradle.kts
- current diff

## Expected Outputs
- commands executed
- result summary
- failing checks
- suspected cause
- recommended next step

## Stop Conditions
Stop if:
- the documented quality gate is inconsistent with the build
- a command fails
- a test fails
- required tooling is missing
- thresholds would need to be changed
- dependency verification would need to be weakened
