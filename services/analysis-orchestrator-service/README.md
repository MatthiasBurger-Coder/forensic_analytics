# Analysis Orchestrator Service

## Status

Slice 09 initial independent Spring Boot gRPC service.

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

The `PlanInstrumentationTargets`, `StartRepositoryToBtm` and
`GetRepositoryToBtmStatus` RPC names remain in the transitional
`analysis-job.proto` contract, but this S09 target service returns
`UNIMPLEMENTED` for them because those behaviors would pull worker or
workflow-specific implementation into the orchestrator.

## Verification

Slice 09 verification commands:

```bash
./gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:analysis-orchestrator-service:build --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

## Known Limits

- The current repository still contains predecessor orchestration behavior in
  `services/analysis-store-service` and monolith modules.
- S09 does not retire `forensic-analytics-engine`,
  `forensic-analytics-application` or `services/analysis-store-service`.
- S09 does not add Docker Compose, Swarm or Kubernetes deployment descriptors
  for this target service.
