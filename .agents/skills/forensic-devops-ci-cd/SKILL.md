---
name: forensic-devops-ci-cd
description: Use for Forensic Analytics CI/CD workflow changes, local CI equivalents, and quality-command alignment.
---

# CI/CD

## Purpose

Guide continuous integration and delivery changes.

## Practices

- Verify existing workflow files before changing CI behavior.
- Keep CI commands aligned with `QUALITY.md`.
- Do not require external credentials for default local verification.
- Treat optional external checks as additive and report skipped credentials.
- Keep artifacts deterministic and avoid committing generated analysis output.

## Verification

- Run local equivalents of changed CI commands where feasible.
- Report any CI command that cannot be validated locally.
