# Spring Boot App Container

Build the Boot jar first:

```bash
./gradlew :forensic-analytics-boot-app:bootJar --dependency-verification strict --console=plain --stacktrace
```

Then build the container from the repository root. The default Java runtime image is pinned by digest in the Dockerfile; override `JAVA_RUNTIME_IMAGE` only after reviewing the replacement image.

```bash
docker build -f docker/boot-app/Dockerfile -t forensic-analytics-boot-app:local .
```

The `docker` Spring profile uses `/var/lib/forensic-analytics/workspaces` as the workspace root and keeps gRPC and REST disabled by default. Enable only the required inbound adapter explicitly, for example:

```bash
docker run --rm \
  -p 127.0.0.1:9090:9090 \
  -v forensic-analytics-workspaces:/var/lib/forensic-analytics/workspaces \
  forensic-analytics-boot-app:local \
  --forensics.analytics.ingestion.grpc.enabled=true \
  --forensics.analytics.ingestion.grpc.host=0.0.0.0
```

No healthcheck is defined because the Boot app does not currently include an accepted Actuator endpoint. Joern, relational database, graph database and LLM provider containers remain outside this baseline.
