# 7. Deployment View

## 7.1 MVP Deployment View

```text
Developer Machine / CI Environment
├── Target Java Project
├── Gradle or Maven Plugin
├── Server-generated BTM Files
├── Runtime Application with Agent-bound BTM instrumentation
└── Forensics Platform
    ├── gRPC Ingestion Server
    ├── Workspace and Git Checkout
    ├── Server-side Parser / Joern / BTM Generation
    ├── Canonical Store
    ├── Event Store
    ├── Simple Graph Projection
    └── LLM Diagnosis Adapter
```

## 7.2 Later Deployment View

```text
Forensics Platform Environment
├── Forensics API
├── Forensics UI
├── gRPC Ingestion Server
├── Relational Store
├── Graph DB
├── Vector DB
├── Event Store
├── Runtime Collector
├── LLM Adapter
└── Repair Orchestrator
```

## 7.3 Deployment Constraints

- The MVP may start as a local or CI-attached analysis platform.
- Runtime event ingestion may initially use JSONL files.
- Parser, Joern and BTM generation run on the Forensics Platform side.
- The plugin may bind server-generated BTM files to the runtime agent for debugging.
- HTTP collector support can be introduced later.
- Multi-tenant production deployment is out of MVP scope.

## 7.4 gRPC Ingestion Configuration

The bootstrap module can start the gRPC ingestion server. The default port is `9090`.

```properties
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.port=9090
```

Environment variable equivalents:

```text
FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED=true
FORENSICS_ANALYTICS_INGESTION_GRPC_PORT=9090
```

## 7.5 Spring Boot Deployment Direction

ADR-0006 accepts `forensic-analytics-boot-app` as the outer server boundary. The Boot app owns Spring Boot startup, typed configuration, profiles and adapter lifecycle wiring for verified inbound adapters.

A minimal Boot startup does not require a database, Joern container, graph database, vector database or live LLM provider. The existing bootstrap module remains available while parity is phased in.

Boot configuration is provided through `application.properties` and profile-specific `.properties` files. The `docker` and `prod` profiles disable gRPC and REST by default; operators must explicitly enable the inbound adapter they intend to expose.

The Boot app can be packaged with:

```bash
./gradlew :forensic-analytics-boot-app:bootJar --dependency-verification strict --console=plain --stacktrace
```

The Docker baseline lives under `docker/boot-app/`. It copies only the generated Boot jar, defines `/var/lib/forensic-analytics/workspaces` as the workspace volume and does not define an Actuator healthcheck because no accepted health endpoint exists yet.
