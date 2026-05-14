---
name: forensic-devops-docker
description: Use for Forensic Analytics Docker workflow, Joern container, and local infrastructure changes that must remain optional unless documented.
---

# Docker

## Purpose

Guide Docker-based local infrastructure and adapter integration.

## Practices

- Keep Docker-dependent workflows optional unless explicitly required.
- Do not require Docker for the default quality gate unless `QUALITY.md` says so.
- Preserve deterministic inputs and outputs for analysis containers.
- Avoid committing generated runtime data or container artifacts.

## Verification

- Run Docker checks only when the task affects Docker behavior or documented workflow.
- Report skipped Docker verification when the local environment cannot support it.
