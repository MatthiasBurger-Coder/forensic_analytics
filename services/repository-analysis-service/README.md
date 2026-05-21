# Repository Analysis Service

## Scope

This service owns repository checkout, revision resolution, workspace
preparation, source-root detection and source snapshot handoff. Other services
receive opaque workspace and artifact references only; private service
filesystem paths are never part of the public contract.

## Runtime

- gRPC port: `9092`
- health port: `8083`
- Docker profile workspace root:
  `/var/lib/forensic-analytics/repository-workspaces`

The service accepts clean HTTPS repository URLs only. Local paths, `file:`
URLs, SSH/SCP remotes, submodules, build execution and parser execution are out
of scope for Slice 06.

Repository checkout runs in a service-owned workspace. Public responses expose
opaque workspace IDs, source snapshot IDs, relative source roots and artifact
references only. Git command output and filesystem paths are not returned in
public error descriptions.

## Java AST Handoff

Slice 06 adds the producer-pushed Java AST handoff path. Repository Analysis
collects bounded UTF-8 Java source files only from verified relative Java source
roots inside its private workspace, computes checksums, and sends relative
source-root paths plus inline source content to `java-ast-analysis-service` over
that service's gRPC contract.

The handoff never exposes private workspace paths, does not add a Java AST pull
API, and does not transfer source-package artifact byte custody. Artifact-byte
or source-package retrieval remains a later contract-governance decision.

## Local Runtime

Package and build this service for the local repository-to-BTM Compose
landscape:

```bash
./gradlew --no-daemon :services:repository-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/repository-analysis-service/Dockerfile --build-arg SERVICE_JAR=services/repository-analysis-service/build/libs/repository-analysis-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/repository-analysis-service:local .
```

When started through `deployment/docker-compose/repository-to-btm.local.yml`,
the health endpoint is published on `127.0.0.1:18083` and the service gRPC port
is published on `127.0.0.1:19092` for local diagnostics.
