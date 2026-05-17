# Joern CPG Analysis Service

## Status

Slice 08 initial independent Spring Boot gRPC service.

This service owns Joern runtime execution, static CPG/CFG/DFG artifact
metadata, provenance and semantic mapping diagnostics. Joern artifacts remain
service-owned files and are exposed to other services only as artifact
references through contracts.

## Boundary

- Public gRPC contract: `contracts/grpc/joern-cpg-analysis.proto`
- Worker kind: `ANALYSIS_WORKER_KIND_JOERN_ANALYSIS`
- Default gRPC port: `9094`
- Default health port: `8085`
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

Slice 08 verified:

```bash
./gradlew --no-daemon :services:joern-cpg-analysis-service:test :services:joern-cpg-analysis-service:jacocoTestReport :services:joern-cpg-analysis-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:joern-cpg-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/joern-cpg-analysis-service/Dockerfile -t joern-cpg-analysis-service:slice08 .
```

Docker image:

```text
joern-cpg-analysis-service:slice08
sha256:ad3af3eb7811543f26e6f7a84818e04f13c92d6aa03bf497882d160475d80b91
```

## Known Limits

- The contract is provisional and intentionally scoped to logical Slice 08
  communication.
- Joern query bundle scripts are mounted or copied into the configured query
  root by later runtime packaging; missing required scripts currently produce
  incompleteness diagnostics.
- Durable artifact indexing and Analysis Store registration remain later
  integration work.
