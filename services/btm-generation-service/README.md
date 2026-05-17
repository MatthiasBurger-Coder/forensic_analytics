# BTM Generation Service

## Status

Slice 09 initial independent service implementation.

The service generates deterministic Byteman/BTM rule artifacts from delivered
analysis facts and bounded instrumentation targets. It does not scan
repositories, execute JavaParser or Joern, read graph storage, invent runtime
trace data or claim that generated rules are observed execution evidence.

## Boundary

- Public transport: `contracts/grpc/btm-generation.proto`
- Worker kind: `ANALYSIS_WORKER_KIND_BTM_GENERATION`
- Input ownership: accepted fact artifact references, semantic artifact
  references, source snapshot IDs, analysis/job IDs and bounded inline
  instrumentation targets
- Output ownership: generated `.btm` rule artifact and deterministic rule
  manifest until registered with Analysis Store

The service owns its domain, application behavior, adapters, configuration,
tests, Dockerfile and health checks. It has no project dependency on the
current monolith modules or other service implementation modules.

## Determinism

Rule IDs are derived from:

```text
sha256("btm-rule-v1\0" + source_snapshot_id + "\0" + target_id + "\0" + probe_kind + "\0" + rule_schema_version)
```

Targets are sorted by target ID and probe kind before rendering. Generated
files use LF line endings and contain no timestamps, local paths or runtime
observations.

## Verification

Slice 09 verification commands:

```bash
./gradlew --no-daemon :services:btm-generation-service:test :services:btm-generation-service:jacocoTestReport :services:btm-generation-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/btm-generation-service/Dockerfile --build-arg SERVICE_JAR=services/btm-generation-service/build/libs/btm-generation-service-0.1.0-SNAPSHOT.jar -t btm-generation-service:slice09 .
```

The full repository quality gate remains defined by `QUALITY.md`.

## Known Limits

- Durable artifact registration with `analysis-store-service` remains later
  orchestration scope.
- The provisional contract intentionally accepts bounded inline instrumentation
  targets instead of a final canonical fact schema.
- Docker Compose, Swarm and Kubernetes runtime readiness are not claimed in
  this slice.
