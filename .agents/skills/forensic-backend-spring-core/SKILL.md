---
name: forensic-backend-spring-core
description: Use only when a verified Forensic Analytics module already uses Spring wiring or bootstrap configuration.
---

# Spring Core

## Purpose

Guide Spring usage if a verified module uses Spring for wiring or runtime bootstrapping.

## Practices

- Verify that Spring is already part of the affected module before using it.
- Keep Spring annotations out of domain and application packages.
- Put configuration and bean wiring in adapter, bootstrap or infrastructure packages.
- Keep tests independent from a full application context unless integration behavior requires it.

## Verification

- Inspect build files for existing Spring dependencies before implementation.
- Run affected module tests and the relevant quality gate from `QUALITY.md`.
