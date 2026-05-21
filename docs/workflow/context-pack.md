# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `e2e-wildfly-cli-deploy-20260521-v1` |
| Workflow branch | `feature/workflow-e2e-wildfly-cli-deploy-20260521` |
| Process strand | `workflow create` now; `workflow execute` later |
| Execution profile | `FULL_PATH` |
| Created | `2026-05-21` |

## Purpose

This context pack is a navigation aid for the workflow. It does not replace
root `AGENTS.md`, `QUALITY.md`, ADRs, arc42, routing rules, role files or the
active workflow.

## Affected Areas

- Repository-to-BTM E2E tests.
- Large-repository Git checkout hardening.
- Gateway OpenAPI and CLI-to-Gateway contract.
- CLI adapter boundary.
- Legacy monolith caller inventory and caller-free retirement gates.
- Deployment workflow handoff for Docker Swarm and Kubernetes.

## Forbidden Areas

- No default external WildFly network test.
- No Swarm stack, Kubernetes manifest or Helm chart in this workflow.
- No production deployment readiness claim.
- No shared Java implementation or DTO module between services.
- No legacy module removal without caller-free evidence and replacement parity.
- No live LLM, graph-replay or report-generation implementation.

## Required Roles

- Senior Workflow Architect
- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

## Conditional Roles

- Senior DevOps Engineer
- Microservice Senior Expert
- Contract Governance Expert
- Git Large Repository Specialist
- Security Sandbox Specialist
- Senior Performance Engineer
- Senior Documentation Engineer

## Quality Commands

Minimum quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Slice-level checks must also run:

```bash
git diff --check
```

## Staleness Rules

This context pack is stale when:

- any governing-file hash in `context-pack.json` changes;
- root `AGENTS.md`, `QUALITY.md`, routing rules, ADRs, arc42 or role files are
  modified by a slice;
- the active branch differs from
  `feature/workflow-e2e-wildfly-cli-deploy-20260521`;
- a slice changes product behavior outside the workflow write scope;
- deployment manifests are added without starting the separate deployment
  workflow.
