# Analysis Orchestrator Service

## Status

FA-MSA-001-LMR S07 independent Spring Boot gRPC service with
repository-to-BTM acceptance and status-only orchestration.

This service owns orchestration state only: analysis job lifecycle, workflow
status, worker leases and attempts, retry and failure state, dead-letter
state, correlation references and job-to-artifact references.

## Boundary

- Public gRPC contract: `contracts/grpc/analysis-job.proto`
- Default gRPC port: `9098`
- Default health port: `8089`
- Docker base: digest-pinned Java 25 runtime image

The service does not own repository checkout, JavaParser scanning, Joern
execution, BTM generation, report rendering, canonical analysis facts,
producer-local artifact catalogs, artifact byte custody, private databases or
private workspaces.

Artifact fields stored by this service are coordination references only. Owner
services remain responsible for artifact bytes, producer-local metadata and
retrieval APIs.

`StartRepositoryToBtm` accepts a validated repository-to-BTM request and
`GetRepositoryToBtmStatus` returns the stored readiness state. The current
state is intentionally incomplete: the service records that repository source
handoff has not completed, reports `waiting for repository source handoff`,
BTM delivery is not ready and Joern is skipped. It does not dispatch
repository workers, run checkout, call JavaParser, call Joern, generate BTM
files, render reports or expose artifact bytes.

`PlanInstrumentationTargets` remains `UNIMPLEMENTED` because instrumentation
target selection would pull worker or analysis implementation into the
orchestrator.

Current orchestration state is process-local and in-memory. S07 does not claim
durable persistence, distributed worker coordination, event outbox publishing,
cross-instance idempotency or production runtime readiness.

## Verification

Service-local verification commands:

```bash
./gradlew :analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :analysis-orchestrator-service:bootJar :analysis-orchestrator-service:bootRun --dry-run --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Local operator start command:

```bash
./gradlew :analysis-orchestrator-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

## Known Limits

- The current repository still contains predecessor orchestration behavior in
  `analysis-store-service` and historical pre-retirement source trees
  until the final legacy source-tree retirement workflow deletes them.
- This service does not retire `analysis-store-service`; the final
  legacy source-tree retirement workflow owns removal of predecessor engine,
  application and domain source trees.
- S07 does not add Docker Compose, Swarm or Kubernetes deployment descriptors
  for this target service.
