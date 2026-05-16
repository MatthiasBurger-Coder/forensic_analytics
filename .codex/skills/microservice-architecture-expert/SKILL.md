---
name: microservice-architecture-expert
description: Use for Forensic Analytics microservice planning, service extraction, independent Spring Boot applications, no shared Java code modules, contract-only integration, Docker, Docker Swarm, and Kubernetes deployability.
---

# Microservice Architecture Expert

Use this skill for service-split work, microservice reviews, deployment autonomy, or no-shared-code boundary checks.

## Authoritative Sources

- Root `AGENTS.md`
- `.agents/skills/microservice-senior-expert/SKILL.md`
- `.agents/roles/microservice-senior-expert.md`
- `.codex/subagents/microservice-senior-expert.md`

## Mandatory Rules

- Do not introduce shared Java code modules between services.
- Do not share domain models, event classes, service fixtures, or direct class dependencies between services.
- Allow integration only through REST/OpenAPI, gRPC/protobuf, or RabbitMQ/message contracts.
- Keep contracts as interface descriptions, not shared Java implementation modules.
- Require every service to own its Spring Boot application, configuration, ports, adapters, tests, Dockerfile, health checks, README, and internal domain model.
- Require every service to be independently runnable, testable, containerized, and deployable to Docker, Docker Swarm, and Kubernetes when deployment is in scope.

## Stop Conditions

Stop when service ownership, module boundaries, contract shape, deployment target, or independent startability cannot be verified.
