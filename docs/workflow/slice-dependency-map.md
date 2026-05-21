# Slice Dependency Map

## Graph

```text
S00 Execution Preflight And Context Freeze
|-- S01 Real Repository End-To-End Test
|   `-- S02 WildFly Hardening Preparation
|-- S03 CLI Gateway Contract
|   `-- S06 CLI First Caller-Free Migration
|-- S04 Separate Swarm And Kubernetes Workflow Handoff
|-- S05 Legacy Monolith Caller Inventory And Retirement Gates
|   |-- S06 CLI First Caller-Free Migration
|   `-- S07 Conditional Legacy Runtime Path Retirement
`-- S08 Final Documentation And Quality Gate
```

## Execution Groups

| Group | Slices | Parallelization |
|---|---|---|
| G00 | S00 | Must run first. |
| G01 | S01, S03, S04 | May run in parallel after S00 if file locks remain disjoint. |
| G02 | S02 | Runs after S01. |
| G03 | S05 | Runs after S03 because caller gates need contract context. |
| G04 | S06 | Runs after S03 and S05. |
| G05 | S07 | Runs after S05 and S06. |
| G06 | S08 | Runs last after S01 through S07. |

## Lock Summary

| Slice | Primary locks |
|---|---|
| S01 | `forensic-analytics-testbed/src/test/**`, real repository fixture files |
| S02 | `WildFlyRepositoryHardeningTest`, WildFly hardening documentation |
| S03 | `contracts/**`, CLI contract tests, Gateway OpenAPI tests |
| S04 | deployment workflow handoff docs only |
| S05 | monolith caller inventory docs |
| S06 | CLI implementation and CLI tests |
| S07 | conditional legacy module paths and `settings.gradle.kts` |
| S08 | workflow, architecture and arc42 synchronization docs |

## S3D Notes

S3D must stop before implementation if a slice uses dependency ranges, unknown
slice IDs, overlapping file locks, overlapping contract locks or unclear
architecture locks.
