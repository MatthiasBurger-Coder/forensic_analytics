# Skill Audit

## Purpose

This audit records the May 2026 review of the repository-specific agent skills, role files, Codex agent configuration and workflow documentation for the next platform phase:

```text
Plugin
  -> gRPC request
    -> forensic_analytics ingestion
      -> create workspace
        -> clone or checkout repository
          -> register analysis session
            -> later parser and analyzer execution
```

The audit keeps the producer/consumer boundary explicit:

- `forensics_tracing` remains the producer, build adapter and plugin.
- `forensic_analytics` remains the consumer and central analysis platform.
- Parser, AST, Joern and BTM generation are Forensic Analytics server capabilities, not plugin responsibilities.
- The plugin may receive server-generated BTM files and bind them through the runtime agent when debugging requires instrumentation.
- Workspace creation and Git checkout are planned before parser implementation.

## Inspected Areas

- `.agents/`
- `.agents/skills/`
- `.agents/roles/`
- `.agents/orchestrator/`
- `.codex/`
- root `AGENTS.md`
- `QUALITY.md`
- `docs/`
- `docs/workplan/`
- `docs/workflow/` when present
- root `workflow.md` when present

The repository root `README.md` and root `workflow.md` were not present during
the original audit. `docs/workflow/` exists after Governance Flowchart V2 and
is now an active workflow documentation package.

## Repository Baseline Verified

- Java 25 is documented in `AGENTS.md`, `QUALITY.md` and `build.gradle.kts`.
- Gradle 9.4.0 is documented in `QUALITY.md` and verified in `gradle/wrapper/gradle-wrapper.properties`.
- JUnit 6 is documented in `AGENTS.md`, `QUALITY.md` and `gradle/libs.versions.toml`.
- The original audit baseline included legacy `forensic-analytics-*` modules. The verified Gradle project model is service-only under `services:*`; retained legacy source trees are historical pre-retirement evidence until final source-tree deletion and closure complete.
- Existing gRPC ingestion code is an inbound adapter and maps Protobuf DTOs into application commands.

## Audit Result

Existing skills did not contain direct Java 17, Gradle 9.1/9.3, JUnit 5, Spring Boot mandate, or plugin-as-platform rules. No existing skill was rewritten.

The audit added missing platform-phase skills and matching senior roles for distributed systems, workspace lifecycle, large Git repositories, gRPC streaming, analysis storage, replay/runtime correlation, Joern/CPG planning, performance/scalability, swarm coordination and security sandboxing.

The governance update added reusable workflow, requirement, arc42 and engineering governance skills plus Senior Workflow Architect and Senior Requirement Engineer roles. The previous `docs/workplan/` content was removed and replaced with a governance workflow for building that system.

## Audit Files

- [conflicts-resolved.md](conflicts-resolved.md)
- [manual-review-required.md](manual-review-required.md)
- [skill-inventory.md](skill-inventory.md)
- [skill-registry.md](skill-registry.md)
- [skill-registry.json](skill-registry.json)
- [microservice-skill-sharpening-audit.md](microservice-skill-sharpening-audit.md)
- [governance-flowchart-v2-linkage.md](governance-flowchart-v2-linkage.md)

## Persistent Registry Matrix

`skill-registry.md` and `skill-registry.json` provide a hash-invalidated cache
for governance routing and conflict-audit reuse. They are secondary evidence,
not source-of-truth files. Manual review is required whenever governance files
under `.agents/**`, `.codex/**`, `AGENTS.md`, `QUALITY.md`,
`docs/workflow/**`, `docs/skill-audit/**`, `docs/agents/**`,
`docs/process/**` or `docs/governance/**` changed.

## Three-Strand Governance Addendum

The reconstructed governance model adds three process strands:

- `skills-agents`
- `workflow create`
- `workflow execute`

`skills update` is the explicit command for the `skills-agents` strand. It may update skills, agents, roles, prompts, Codex agent definitions, routing rules, organigramm, the skill registry and related process documentation.

`push auto` is restricted to `skills-agents`. Slice checkpoint push belongs to `workflow execute` and is not `push auto`.
