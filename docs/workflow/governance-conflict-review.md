# Governance Conflict Review

## Branch Rule

The requested branch name is architecture-scoped and was created before
workflow artifacts were regenerated:

```text
architecture/microservices-ecosystem-conversion-20260516
```

Workflow artifacts must not be modified on `main`, `master`, `develop` or any
other shared branch.

## Documentation Language

The user draft is German. Root `AGENTS.md` requires repository documentation to
be English. The workflow was therefore authored in English while preserving the
German draft's architecture intent.

## Quality Command Conflict

The user draft lists simplified commands such as:

```bash
./gradlew clean test
./gradlew check
```

`QUALITY.md` is authoritative and requires strict dependency verification. The
workflow must use `QUALITY.md` commands during execution and may list draft
commands only as non-authoritative intent.

## Existing Workflow Replacement

The previous active workflow under `docs/workflow/**` described Skill and Agent
Integrity Correction. The workflow-authoring rules require deleting and
regenerating `docs/workflow` for a new active workflow unless the user asks to
preserve the previous workflow. The user did not request preservation.

## Target Path Differences

- Current frontend root is `forensic-ui`, while the target landscape names
  `frontend/frontend-web-app`.
- Current contracts live under a module-specific proto directory, while the
  target landscape names `contracts/grpc`, `contracts/openapi` and
  `contracts/events`.
- Current Docker material is Joern-only under `docker/joern`, while the target
  landscape names `deployment/**`.
- Root `README.md` is not currently present. `docs/README.md` exists. A final
  documentation slice may create root `README.md` only after verifying the
  repository convention and documenting the reason.

## Service Landscape Adjustment

`docs/arc42/07-deployment-view.md` currently names a smaller future service
set:

```text
services/forensic-server
services/java-ast-scanner-worker
services/joern-scanner-worker
services/btm-generator-worker
services/report-generator-worker
```

The user-supplied workflow names a broader target ecosystem. This is not treated
as a conflict that blocks workflow creation. It is recorded as an architecture
synchronization task for Slice 01 and later arc42 updates.

## Commit And Push

Workflow creation does not commit or push product implementation slices.
`workflow execute` uses slice-scoped checkpoint commits and branch pushes after
each successful slice quality gate. The checkpoint push goes only to the current
workflow branch, does not merge a PR, does not clean up branches and is not
`push auto`.
