# ADR-0015: Use skill registry and conflict auditing

## Status

Accepted

## Context

The repository contains many skills, roles, callable agents and workflow documents. As the skill landscape grows, overlapping responsibilities can silently weaken architecture, quality, security or release governance.

## Decision

Skill Registry & Conflict Auditor is required for new skill creation, workflow execution and governance changes that affect skill ownership.

It classifies conflicts as:

- `BLOCKING`
- `NON_BLOCKING`

Blocking conflicts must be resolved before workflow execution continues.

## Consequences

- New skills must define mission, responsibilities, authority, forbidden scope, inputs, outputs, collaboration rules and STOP rules.
- Missing owners, incompatible outputs, quality-gate downgrades and microservice-boundary violations are blocking.
- Project-specific rules must not be hidden in portable `.codex` assets.
- Conflict reports become part of workflow evidence.
