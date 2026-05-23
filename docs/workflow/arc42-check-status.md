# arc42 Check Status

## Workflow Creation Status

Status: checked for workflow creation on 2026-05-23.

The workflow was regenerated because repository state changed since the earlier
legacy-retirement plan. `settings.gradle.kts` now registers only `services:*`
projects, while several arc42 and architecture documents still describe
legacy `forensic-analytics-*` modules as retained active rollback or runtime
evidence. Version 2 inserts a pre-deletion documentation cleanup slice for
stale executable legacy commands. Workflow creation does not update product
behavior or delete source trees, so detailed final arc42 closure is assigned to
execution slice S06 after S05 deletion evidence exists.

## Required Execution Updates

Execution slices must update arc42 when actual state changes:

| Slice | arc42 Area |
|---|---|
| S01 | Reference classification notes for stale legacy module mentions |
| S02 | Deployment/runtime documentation if `docker/boot-app` and boot-app jar references are retired |
| S03 | Quality requirements and regression ownership after legacy module-local tests are superseded or deprecated |
| S04 | Pre-delete deployment-command cleanup in section 07 and related service documentation |
| S05 | Verified source-tree deletion evidence |
| S06 | Sections 05, 06, 07, 08, 09, 10 and 11 for final build topology, runtime/deployment view, crosscutting concepts, ADR references, quality requirements and risks |
| S07 | Final quality-gate evidence and release-readiness status |

## Stop Conditions

Stop execution when docs claim:

- a legacy source tree is removed while `git ls-files "forensic-analytics-*"`
  still lists tracked files;
- a legacy module is an active Gradle project while `settings.gradle.kts` does
  not register it;
- a service is independently deployable without verified build/start/container
  evidence;
- Swarm or Kubernetes readiness without repository manifests and commands;
- persistence ownership without an explicit service owner;
- generated or inferred output as verified forensic evidence.
