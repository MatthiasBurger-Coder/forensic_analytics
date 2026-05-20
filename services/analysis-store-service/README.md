# Analysis Store Service

## Status

Slice 11 Analysis Store-owned job and repository-to-BTM orchestration
readiness implementation.

This service currently implements the service-local `AnalysisJobService` gRPC
contract from `contracts/grpc/analysis-job.proto`. It owns analysis job
lifecycle state, worker leasing, progress persistence, completion/failure state
analysis artifact metadata registration, bounded instrumentation target
selection metadata and the repository-to-BTM orchestration readiness bridge.

The current persistence adapter is intentionally service-local and non-durable
in-memory storage. Durable database selection, migrations, normalized fact
schemas, incident records and correlation indexes remain future slices. Other
services must still use owner APIs or approved events instead of direct database
access.

## Runtime Interfaces

| Interface | Default Port | Notes |
|---|---:|---|
| gRPC `AnalysisJobService` | `9091` | Job submission, leasing, progress, completion, failure, listing, artifact registration, instrumentation target planning and repository-to-BTM orchestration readiness |
| JDK HTTP health endpoint | `8082` | Returns readiness-style health without Spring Actuator |

## Service Boundary

The service is independent from the existing modular-monolith implementation:

- no dependency on `forensic-analytics-domain`;
- no dependency on `forensic-analytics-application`;
- no dependency on `forensic-analytics-persistence`;
- no shared Java DTO, repository, domain, service or fixture module;
- generated protobuf classes are service-local build output only.

Domain and application packages stay free of Spring, gRPC and generated
transport classes. The gRPC adapter maps protobuf requests into service-owned
domain objects and maps domain state back into transport responses.

The service-owned model preserves accepted job schema version, correlation ID,
attributes, progress percentage, diagnostics, failure metadata and artifact
metadata. Concurrent in-memory lifecycle mutations are serialized by the
application service so a dispatchable or retryable job is leased to only one
worker at a time.

Instrumentation target planning is Analysis Store-owned. It accepts bounded
static source facts and accepted static semantic artifact references, produces
stable target IDs, deterministic ordering, a selection fingerprint and explicit
completeness diagnostics. Missing semantic-node mappings are reported as
incomplete rather than invented, and generated targets are not runtime
execution evidence.

Repository-to-BTM orchestration readiness is Analysis Store-owned. Gateway
submits public requests through the `StartRepositoryToBtm` gRPC method instead
of calling worker services directly. The current bridge creates deterministic
repository-analysis dispatch state, keeps BTM delivery unavailable until
accepted package metadata exists, and reports unavailable source/build packages
with explicit incomplete diagnostics so Joern is skipped safely.

## Verification

Service-specific verification:

```bash
./gradlew --no-daemon :services:analysis-store-service:test :services:analysis-store-service:jacocoTestReport :services:analysis-store-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
```

Package coverage is expected to remain above the repository package thresholds
for the service-owned packages.

Package the service:

```bash
./gradlew --no-daemon :services:analysis-store-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Build the container image:

```bash
docker build -f services/analysis-store-service/Dockerfile -t analysis-store-service:slice05 .
```
