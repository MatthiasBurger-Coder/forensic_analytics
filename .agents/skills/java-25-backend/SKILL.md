---
name: java-25-backend
description: Use for Java backend implementation that must follow the configured Java 25 Gradle toolchain and repository style.
---

# Java 25

## Purpose

Guide backend implementation using the repository Java baseline without changing toolchain versions.

## Practices

- Use the configured Java toolchain from the Gradle build.
- Prefer immutable value objects and explicit records where they fit existing style.
- Prefer declarative programming before imperative programming when behavior is naturally expressed as mapping, filtering, grouping, validation, composition or policy selection.
- Use Strategy Pattern for behavior that varies by worker kind, artifact type, storage backend, parser, provider, transport, status or output format.
- Prefer explicit strategy registries, enum-backed policy methods or named mapper components over nested conditional dispatch.
- Keep exceptions descriptive and preserve original causes.
- Avoid static mutable state and hidden side effects.
- Keep comments and JavaDoc in English.

## Verification

- Confirm toolchain configuration in the build before changing Java language assumptions.
- Run targeted module tests with the Gradle wrapper through WSL on Windows hosts.
