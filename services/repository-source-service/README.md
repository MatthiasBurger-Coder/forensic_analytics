# Repository Source Service

## Scope

This service owns repository checkout, revision resolution, workspace
preparation, source-root detection, source snapshot descriptors and source
package byte custody. Other services receive opaque workspace IDs, source
snapshot IDs and artifact references only; private service filesystem paths are
never part of the public contract.

## Runtime

- gRPC port: `9092`
- health port: `8083`
- Docker profile workspace root:
  `/var/lib/forensic-analytics/repository-workspaces`

The service accepts clean HTTPS repository URLs only. Local paths, `file:`
URLs, SSH/SCP remotes, submodules, build execution and parser execution are out
of scope for S05.

Repository checkout runs in a service-owned workspace. Public responses expose
opaque workspace IDs, source snapshot IDs, relative source roots and artifact
references only. Git command output and filesystem paths are not returned in
public error descriptions.

## Local Runtime

Package and build this service:

```bash
./gradlew --no-daemon :services:repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/repository-source-service/Dockerfile --build-arg SERVICE_JAR=services/repository-source-service/build/libs/repository-source-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/repository-source-service:local .
```

Run the service locally:

```bash
./gradlew --no-daemon :services:repository-source-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The default health endpoint listens on `127.0.0.1:8083` and the service gRPC
port listens on `127.0.0.1:9092`. S05 does not claim Docker Compose, Swarm or
Kubernetes runtime readiness for this target service.
