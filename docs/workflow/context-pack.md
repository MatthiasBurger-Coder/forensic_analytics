# Workflow Context Pack

This context pack is a workflow-local navigation aid for
`governance-performance-20260521-v1`.

It is not the source of truth. Root `AGENTS.md`, `QUALITY.md`, ADRs, arc42,
routing rules, workflow files and skill files remain authoritative.

## Scope

| Field | Value |
|---|---|
| Repository | `forensic_analytics` |
| Workflow branch | `architecture/workflow-governance-performance-20260521` |
| Active strand | `workflow execute` |
| Execution profile | `FULL_PATH` |
| Automation status | Defined by Slice S03 |

## Required Roles

- Senior Workflow Architect
- Senior Requirement Engineer
- Senior System Architect
- Senior Documentation Engineer
- Senior Tester

Conditional roles:

- Senior Java Backend Developer for N/A impact checks unless product backend
  scope appears.
- Senior React Frontend Developer for N/A impact checks unless frontend scope
  appears.
- Senior Swarm Orchestrator until the dedicated S3D role exists.
- Senior Performance Engineer for the process profiler slice.
- Skill Registry Conflict Auditor for skills, roles and routing changes.

## Quality Commands

Default governance-only command:

```bash
git diff --check
```

JSON validation command:

```bash
python3 -m json.tool docs/workflow/context-pack.json
```

Minimum Gradle command from `QUALITY.md`, required only if scope is refined into
build-influencing files:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate from `QUALITY.md`, required only if scope expands to broad
product/build influence or final release requires it:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Reuse Rule

Read this pack first for orientation. Reopen the authoritative source files
when:

- any recorded hash changed;
- the slice touches governance files;
- a conflict, missing owner, missing role, missing skill, missing route or
  unclear quality command is detected.

## Hash Record

The machine-readable hash record is
[`context-pack.json`](context-pack.json). Slice S03 owns the initial formal
definition; later workflow slices refresh hashes when their governing files
change.
