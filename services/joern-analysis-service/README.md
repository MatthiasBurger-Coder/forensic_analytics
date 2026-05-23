# Joern Analysis Service

## Status

FA-MSA-001-LMR S06 independent Spring Boot gRPC service.

This service owns Joern runtime execution, static CPG/CFG/DFG artifact
metadata, provenance and semantic mapping diagnostics. Joern artifacts remain
service-owned files and are exposed to other services only as artifact
references through contracts. Artifact bytes are retrieved through the
service-owned `GetSemanticArtifactBytes` RPC; other services receive metadata
and public retrieval references, not private filesystem paths.

## Boundary

- Public gRPC contract: `contracts/grpc/joern-cpg-analysis.proto`
- Worker kind: `ANALYSIS_WORKER_KIND_JOERN_ANALYSIS`
- Default gRPC port: `9096`
- Default health port: `8087`
- Docker image base: digest-pinned Joern runtime plus copied Java 25 runtime

The service accepts opaque workspace IDs, source snapshot IDs, relative source
roots and bounded policy values. Absolute workspace paths, `file:` URIs and
mutable server paths are not part of the public contract.

`joern_image_reference` in the request is a digest-pinned runtime-image
constraint. The service records the configured runtime image as provenance and
rejects requests that do not match that service-owned runtime reference.

CPG/CFG/DFG outputs are static semantic analysis artifacts. They must not be
presented as observed runtime execution, branch decisions, parameter values or
confirmed findings. Missing Joern query scripts or missing artifacts are
reported as explicit diagnostics and incomplete analysis state.

## Verification

S06 verification commands:

```bash
./gradlew :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-adapter-joern-docker:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:joern-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Local operator start command:

```bash
./gradlew :services:joern-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Docker image builds and Joern runtime smoke tests are optional external
verification for this slice because they may pull the digest-pinned Joern base
image or create local container state.

S16 does not retain the service-root testbed Joern Docker smoke path as target
runtime readiness. Joern Docker smoke remains optional external verification,
not target runtime readiness and not evidence of completed repository-to-BTM
analysis parity.

## Known Limits

- The contract is provisional and intentionally scoped to logical Slice 06
  communication.
- Joern query bundle scripts are mounted or copied into the configured query
  root by later runtime packaging; missing required scripts currently produce
  incompleteness diagnostics.
- Durable artifact indexing and target owner registration remain later
  integration work. Current predecessor integration may involve
  `analysis-store-service` for artifact metadata registration. Artifact byte
  reads stay owned by this service through `GetSemanticArtifactBytes`.

The default service bind address is `0.0.0.0` with health on port `8087` and
gRPC on port `9096`. Local clients and the Docker health probe can address the
health endpoint through `127.0.0.1:8087` when the service runs on the local
host. S06 does not add Docker Compose, Swarm or Kubernetes deployment for this
target service; those claims require dedicated deployment files and
verification.
