---
name: workflow-slice-execution
description: "Use for current-project slice execution: read-only verification, minimal implementation, targeted tests, quality gate, and final summary."
---

# Slice Execution

## Purpose

Execute small, traceable implementation increments.

## Practices

1. Read-only verification.
2. Slice plan with affected files and quality checks.
3. Minimal implementation.
4. Targeted tests.
5. Applicable quality gate from `QUALITY.md`.
6. Documentation update when public behavior changes.
7. Clear final summary with commands executed.

## Stop Conditions

- Required symbols, tasks, files or contracts cannot be verified.
- Documentation and source disagree in a behavior-relevant way.
- Continuing would require fabricating evidence or guessing semantics.
