# Execution Report

## Status

`workflow execute` started. Slices 00, 01, 02, 03, 04, 05 and 06 checkpoints
completed and pushed under `microservices-btm-pipeline-20260517-v1`. The v1
Slice 07 Joern Worker Handoff stopped before implementation because the
required Repository Analysis to Joern transfer and materialization contract was
not verified, and artifact byte-access metadata was not preserved by the
involved service mappings. `microservices-btm-pipeline-20260517-v2` is being
introduced to add the missing Repository Snapshot and Build Artifact Worker
Contract slice before Joern handoff execution resumes.

## Branch

`feature/workflow-microservices-btm-pipeline-20260517`

## Workflow Creation Evidence

Branch-first checks completed before workflow artifact regeneration:

```text
git rev-parse --show-toplevel -> repository root verified
git branch --show-current -> feature/workflow-microservices-btm-pipeline-20260517
git status --short --branch -> clean workflow branch before edits
```

Mandatory subagent and role reviews completed during workflow creation:

- Senior Requirement Engineer review integrated.
- Senior System Architect review integrated.
- Senior Java Backend review integrated.
- Senior React Frontend review integrated.
- Senior Tester review integrated.

The workflow-create package was committed and pushed before `workflow execute`
started:

```text
commit -> 491ba762f877eb5e7c44c112898011ba421c99f7
subject -> docs(workflow): create microservices btm pipeline workflow
upstream -> origin/feature/workflow-microservices-btm-pipeline-20260517
ahead/behind -> 0/0 before Slice 00 edits
```

## Slice 00 - Execution Preflight

### Review Evidence

Read-only Slice 00 reviews completed before this file was changed:

- Senior Workflow Architect: no dependency, metadata or scope blocker.
- Senior Git Workspace Specialist: no branch, ref, worktree, staging,
  line-ending or push-readiness blocker.
- Senior Tester: docs-only Slice 00 gate does not require Gradle because no
  product code, contracts, build logic, runtime files or frontend files change.

### Pre-Change Verification

```text
git rev-parse --show-toplevel -> repository root verified
git branch --show-current -> feature/workflow-microservices-btm-pipeline-20260517
git show-ref --verify --quiet refs/heads/feature/workflow-microservices-btm-pipeline-20260517 -> passed
git status --short --branch -> clean, tracking origin
git diff --stat -> empty
git diff --name-status -> empty
git diff --check -> passed
```

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=00
sliceTitle=Execution preflight, branch verification and baseline refresh
responsibleAgent=Workflow Executor with Senior Workflow Architect, Senior Git Workspace Specialist and Senior Tester reviews
changedFiles=docs/workflow/execution-report.md
qualityGateCommands=git rev-parse --show-toplevel; git branch --show-current; git show-ref --verify --quiet refs/heads/feature/workflow-microservices-btm-pipeline-20260517; git status --short --branch; git diff --stat; git diff --name-status; git diff --check
qualityGateResult=PASS
commitHash=28f0f6ba34b1d35501334ac1a18d6f55f50b2a20
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=491ba762f877eb5e7c44c112898011ba421c99f7
arc42Updated=not required for Slice 00
adrUpdated=not required for Slice 00
```

## Slice 01 - Contract Gap And Service Boundary Freeze

### Review Evidence

Read-only Slice 01 reviews completed before boundary documentation was
changed:

- Senior System Architect: core owners are mostly documented, but Gateway to
  Repository Analysis, BTM byte delivery, remaining monolith owner mapping and
  stale workflow evidence needed correction.
- Microservice Senior Expert: no shared service Java implementation-module
  dependency was found, but target service inventory, slice-number references,
  communication alternatives and contract-test wording needed correction.
- Senior Analysis Storage Architect: Analysis Store, Repository Analysis and
  BTM Generation ownership are clear at a high level, but BTM byte ownership
  and Joern source handoff needed explicit freezing before contract slices.

Scope note: direct edits under `contracts/**` and `services/**` are deferred
to later slices with matching write scope. Slice 01 freezes the active boundary
decisions in `docs/architecture/**` and records stale contract-root or service
README wording as documentation drift to resolve when those paths enter scope.

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=01
sliceTitle=Contract gap and service-boundary freeze for the BTM pipeline
responsibleAgent=Workflow Executor with Senior System Architect, Microservice Senior Expert and Senior Analysis Storage Architect reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; docs/workflow/workflow.md; docs/workflow/three-amigos-decision-record.md; docs/architecture/service-boundaries.md; docs/architecture/data-ownership.md; docs/architecture/service-communication-matrix.md; docs/architecture/service-migration-map.md; docs/architecture/current-state.md; docs/architecture/contract-versioning.md; docs/architecture/target-microservices-architecture.md; docs/architecture/current-build-and-test-map.md; docs/architecture/monorepo-service-build-strategy.md
qualityGateCommands=git status --short --branch; git diff --stat; git diff --name-status; git diff --check
qualityGateResult=PASS
commitHash=516fbd23869850866dba846724e87e68827ac958
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=28f0f6ba34b1d35501334ac1a18d6f55f50b2a20
arc42Updated=not required for Slice 01; supporting architecture docs updated
adrUpdated=not required for Slice 01
```

## Slice 02 - Gateway HTTP And gRPC BTM Delivery Contracts

### Review Evidence

Read-only Slice 02 reviews completed before contract files were changed:

- Senior gRPC Proto Specialist: required an additive public BTM artifact
  delivery RPC without renumbering existing `btm-generation.proto` fields, with
  request identity, idempotency, correlation, size limits, manifest-first
  streaming and deterministic chunk metadata.
- Senior Java Backend: required explicit Gateway async submission semantics,
  public delivery contract coverage, service-local generated Protobuf
  verification and reconciliation of prior workflow bookkeeping.
- Senior Tester: blocked the slice until an executable OpenAPI contract check
  existed for `contracts/openapi/gateway-api.yaml`, then required focused
  OpenAPI, Protobuf generation and BTM contract tests plus the repository
  minimum `./gradlew test` gate.

### Verification

```text
git diff --check -> PASS
./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:btm-generation-service:generateProto --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:btm-generation-service:test --tests '*BtmGenerationContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew test --dependency-verification strict --console=plain --stacktrace -> PASS
```

The full local quality gate remains deferred to later implementation or final
acceptance slices because Slice 02 changed contracts, documentation and focused
contract tests only.

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=02
sliceTitle=Gateway HTTP and gRPC BTM delivery contracts
responsibleAgent=Workflow Executor with Senior gRPC Proto Specialist, Senior Java Backend and Senior Tester reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; contracts/README.md; contracts/grpc/README.md; contracts/grpc/btm-generation.proto; contracts/openapi/README.md; contracts/openapi/gateway-api.yaml; docs/architecture/contract-versioning.md; docs/contracts/contract-test-plan.md; forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/rest/GatewayOpenApiContractTest.java; services/btm-generation-service/src/test/java/de/burger/forensics/analytics/services/btmgeneration/adapter/in/grpc/BtmGenerationContractTest.java
qualityGateCommands=git diff --check; ./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:btm-generation-service:generateProto --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:btm-generation-service:test --tests '*BtmGenerationContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew test --dependency-verification strict --console=plain --stacktrace
qualityGateResult=PASS
commitHash=1b65e12c4a51421a73c60935ceb8d4973df331c7
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=516fbd23869850866dba846724e87e68827ac958
arc42Updated=not required for Slice 02; contract-versioning updated
adrUpdated=not required for Slice 02
```

## Slice 03 - Artifact And Instrumentation Target Ownership Contract

### Review Evidence

Read-only Slice 03 reviews completed before ownership contract files were
changed:

- Senior Analysis Storage Architect: confirmed Analysis Store owns canonical
  facts and accepted artifact metadata, not artifact bytes; requested explicit
  byte-custody metadata, target-selection ownership and event wording that
  metadata acceptance does not transfer byte custody.
- Senior gRPC Proto Specialist: required additive fields only, no existing
  field renumbering or RPC renaming, with byte-access metadata on
  `AnalysisArtifactReference` and target-selection provenance on BTM
  generation/delivery messages.
- Senior Tester: required focused Protobuf generation and contract tests,
  a focused executable event contract test when `analysis-events.md` changes,
  and the repository minimum `./gradlew test` gate before checkpoint.

### Verification

```text
git diff --check -> PASS
./gradlew :services:analysis-store-service:generateProto :services:btm-generation-service:generateProto :services:java-ast-analysis-service:generateProto :services:joern-cpg-analysis-service:generateProto --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:analysis-store-service:test --tests '*AnalysisJobContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:btm-generation-service:test --tests '*BtmGenerationContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :forensic-analytics-rest:test --tests '*AnalysisEventsContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS after whitespace-normalized Markdown assertion fix
./gradlew test --dependency-verification strict --console=plain --stacktrace -> PASS
```

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=03
sliceTitle=Artifact and instrumentation target ownership contract
responsibleAgent=Workflow Executor with Senior Analysis Storage Architect, Senior gRPC Proto Specialist and Senior Tester reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; contracts/grpc/README.md; contracts/grpc/analysis-job.proto; contracts/grpc/btm-generation.proto; contracts/events/analysis-events.md; docs/architecture/data-ownership.md; docs/architecture/service-communication-matrix.md; forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/contracts/AnalysisEventsContractTest.java; services/analysis-store-service/src/test/java/de/burger/forensics/analytics/services/analysisstore/adapter/in/grpc/AnalysisJobContractTest.java; services/btm-generation-service/src/test/java/de/burger/forensics/analytics/services/btmgeneration/adapter/in/grpc/BtmGenerationContractTest.java
qualityGateCommands=git diff --check; ./gradlew :services:analysis-store-service:generateProto :services:btm-generation-service:generateProto :services:java-ast-analysis-service:generateProto :services:joern-cpg-analysis-service:generateProto --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:analysis-store-service:test --tests '*AnalysisJobContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:btm-generation-service:test --tests '*BtmGenerationContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :forensic-analytics-rest:test --tests '*AnalysisEventsContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew test --dependency-verification strict --console=plain --stacktrace
qualityGateResult=PASS
commitHash=acccc6cd1f14b0535e5d923d5a64731c88a3c461
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=1b65e12c4a51421a73c60935ceb8d4973df331c7
arc42Updated=not required for Slice 03; data-ownership and service-communication matrix updated
adrUpdated=not required for Slice 03
```

## Slice 04 - Gateway Service Bootstrap

### Review Evidence

Read-only Slice 04 reviews completed before service bootstrap files were
changed:

- Microservice Senior Expert: approved a strict service-owned Gateway runtime
  shell with no worker service Java dependencies, no shared DTO/test-fixture
  modules, no database access and no worker orchestration logic.
- Senior Java Backend: approved a minimal service-local Spring Boot shell with
  `bootstrap`, `adapter.in.http`, `application` and `domain` packages, JDK HTTP
  endpoints only and service-local architecture tests.
- Senior DevOps: approved Gradle registration, service-local Dockerfile,
  profile-specific properties and healthcheck behavior without Actuator or
  Protobuf plugin work in this slice.
- Senior Tester: required focused Gateway tests, architecture leakage checks,
  service coverage, bootJar, OpenAPI contract smoke coverage, the repository
  minimum test gate and the full local quality gate.

Post-change reviews completed after implementation and verification:

- Microservice Senior Expert: READY; no service-autonomy blocker found.
- Senior Java Backend: READY; no Java/backend blocker found.
- Senior DevOps: READY; no build/runtime-shell blocker found.
- Senior Tester: READY; no test/quality blocker found.

### Verification

```text
git diff --check -> PASS
./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayServiceApplicationTest' --tests '*ForensicGatewayHttpAdapterTest' --tests '*ForensicGatewayServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:forensic-gateway-service:test --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:forensic-gateway-service:jacocoTestReport :services:forensic-gateway-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace -> PASS
java -jar services/forensic-gateway-service/build/libs/forensic-gateway-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=test --forensics.gateway.service.http.port=18080 plus curl /api/health and --healthcheck -> PASS
rg leakage gate for project(...) in services/*/build.gradle.kts -> PASS
rg leakage gate for forbidden worker service imports in forensic-gateway-service -> PASS
./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew test --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace -> PASS after adding service-local domain model branch coverage
```

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=04
sliceTitle=Gateway service bootstrap
responsibleAgent=Workflow Executor with Microservice Senior Expert, Senior Java Backend, Senior DevOps and Senior Tester reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; settings.gradle.kts; services/forensic-gateway-service/README.md; services/forensic-gateway-service/Dockerfile; services/forensic-gateway-service/build.gradle.kts; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/domain/DownstreamServiceStatus.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/domain/GatewayStatusSnapshot.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/application/GatewayStatusService.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/adapter/in/http/GatewayHttpHandler.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceApplication.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceConfiguration.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceProperties.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServicePropertiesConfiguration.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/GatewayHttpServerLifecycle.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/HealthProbe.java; services/forensic-gateway-service/src/main/resources/application.properties; services/forensic-gateway-service/src/main/resources/application-test.properties; services/forensic-gateway-service/src/main/resources/application-docker.properties; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceApplicationTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/adapter/in/http/ForensicGatewayHttpAdapterTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/quality/ForensicGatewayServiceArchitectureTest.java
qualityGateCommands=git diff --check; ./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayServiceApplicationTest' --tests '*ForensicGatewayHttpAdapterTest' --tests '*ForensicGatewayServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:test --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:jacocoTestReport :services:forensic-gateway-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace; java -jar services/forensic-gateway-service/build/libs/forensic-gateway-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=test --forensics.gateway.service.http.port=18080 plus curl /api/health and --healthcheck; rg leakage gate for project(...) in services/*/build.gradle.kts; rg leakage gate for forbidden worker service imports in forensic-gateway-service; ./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew test --dependency-verification strict --console=plain --stacktrace; ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
qualityGateResult=PASS
commitHash=6b33f6378afd14e5f747904cf87a9c0c9d2fbdd8
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=acccc6cd1f14b0535e5d923d5a64731c88a3c461
arc42Updated=not required for Slice 04; runtime shell documented in service README
adrUpdated=not required for Slice 04
```

## Implementation Status

Slice 04 adds an independently registered Gateway service shell with
service-owned HTTP health/status endpoints, configuration, Dockerfile,
architecture checks and tests. It intentionally does not implement repository
submission, orchestration, worker calls, BTM delivery, persistence, frontend code
or deployment orchestration.

## Slice 05 - External Git Repository Workspace Flow

### Review Evidence

Read-only Slice 05 reviews completed before Gateway and Repository Analysis
files were changed:

- Senior Tester: approved implementation after requiring Gateway HTTP tests,
  idempotency/conflict tests, unsafe remote rejection, gRPC fake endpoint tests,
  architecture tests, OpenAPI contract coverage, leakage checks and the full
  `QUALITY.md` gate.
- Senior Java Backend: approved a Gateway facade that validates and maps HTTP
  requests to the existing `repository-analysis.proto` contract without worker
  logic or shared service implementation dependencies.
- Microservice Senior Expert: approved service-local generated gRPC use,
  Gateway-owned request models and explicit Repository Analysis client
  configuration.
- Senior Git Workspace Specialist: approved after Slice 04 bookkeeping was
  reconciled in this report.
- Senior Security Sandbox Engineer: blocked initial readiness until Repository
  Analysis rejected DNS-resolved private addresses before `git clone` and source
  root detection ignored symlinked Java roots; both blockers are fixed in this
  slice.
- Senior Java Backend post-change review blocked redirect handling until Git
  network commands disabled HTTP(S) redirects after pre-clone host validation;
  `SafeGitCommandRunner` now pins `http.followRedirects=false`.
- Senior Security Sandbox Engineer post-change review blocked DNS rebinding and
  unsafe correlation-id echo risks; Git clone commands now pin validated
  addresses through `http.curloptResolve`, and Gateway mutation headers are
  syntax-validated before response-header or JSON echo use.

### Verification

```text
./gradlew :services:forensic-gateway-service:generateProto --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayHttpAdapterTest' --tests '*GatewayRepositoryAnalysisSubmissionServiceTest' --tests '*RepositoryAnalysisGrpcClientTest' --tests '*ForensicGatewayServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:repository-analysis-service:test --tests '*RepositoryAnalysisDomainTest' --tests '*GitRepositoryCheckoutAdapterTest' --tests '*FileSystemRepositoryWorkspaceAdapterTest' --tests '*SafeGitCommandRunnerTest' --tests '*RepositoryAnalysisGrpcEndpointTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:repository-analysis-service:test --tests '*SafeGitCommandRunnerTest' --tests '*GitRepositoryCheckoutAdapterTest' --dependency-verification strict --console=plain --stacktrace -> PASS after redirect blocking fix
./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayHttpAdapterTest' :services:repository-analysis-service:test --tests '*GitRepositoryCheckoutAdapterTest' --tests '*SafeGitCommandRunnerTest' --dependency-verification strict --console=plain --stacktrace -> PASS after DNS pinning and header validation fixes
./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:forensic-gateway-service:test :services:forensic-gateway-service:jacocoTestReport :services:forensic-gateway-service:jacocoTestCoverageVerification :services:forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace -> PASS
rg leakage gate for absolute workspace paths and local file references in Gateway main sources and OpenAPI contract -> PASS; only path-rejection validation patterns are present
rg leakage gate for workspace identifiers in Gateway responses -> PASS; only negative test assertions are present
./gradlew test --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace -> PASS after targeted branch-coverage regression tests and monotonic checkout timing fix
```

Initial full local quality execution reached `checkPackageCoverage` and failed
only on package branch coverage for the new Gateway and Repository Analysis
security paths. The failure was routed as a test/coverage blocker and fixed with
targeted regression tests for HTTP error mapping, Gateway validation, gRPC
status mapping and remote-address rejection. During the coverage rerun,
Repository Analysis also exposed a verified checkout timing defect: wall-clock
duration measurement could produce negative elapsed milliseconds. The adapter now
uses monotonic `System.nanoTime()` measurement before constructing domain
evidence.

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=05
sliceTitle=External Git Repository Workspace Flow
responsibleAgent=Workflow Executor with Senior Tester, Senior Java Backend, Microservice Senior Expert, Senior Git Workspace Specialist and Senior Security Sandbox Engineer reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; contracts/openapi/README.md; contracts/openapi/gateway-api.yaml; forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/rest/GatewayOpenApiContractTest.java; services/forensic-gateway-service/README.md; services/forensic-gateway-service/build.gradle.kts; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/adapter/in/http/GatewayHttpHandler.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/adapter/out/grpc/RepositoryAnalysisGrpcClient.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/application/GatewayIdempotencyConflictException.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/application/GatewayRepositoryAnalysisException.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/application/GatewayRepositoryAnalysisSubmissionService.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/application/port/RepositoryAnalysisPreparationPort.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceConfiguration.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceProperties.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServicePropertiesConfiguration.java; services/forensic-gateway-service/src/main/java/de/burger/forensics/analytics/services/gateway/domain/GatewayRepositoryAnalysis.java; services/forensic-gateway-service/src/main/resources/application.properties; services/forensic-gateway-service/src/main/resources/application-test.properties; services/forensic-gateway-service/src/main/resources/application-docker.properties; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/adapter/in/http/ForensicGatewayHttpAdapterTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/adapter/out/grpc/RepositoryAnalysisGrpcClientTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/application/GatewayRepositoryAnalysisSubmissionServiceTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/bootstrap/ForensicGatewayServiceApplicationTest.java; services/forensic-gateway-service/src/test/java/de/burger/forensics/analytics/services/gateway/quality/ForensicGatewayServiceArchitectureTest.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/GitRepositoryCheckoutAdapter.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/RemoteHostResolver.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/RemoteHostValidator.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/SafeGitCommandRunner.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/SourceRootDetector.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/GitRepositoryCheckoutAdapterTest.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/git/SafeGitCommandRunnerTest.java
qualityGateCommands=./gradlew :services:forensic-gateway-service:generateProto --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayHttpAdapterTest' --tests '*GatewayRepositoryAnalysisSubmissionServiceTest' --tests '*RepositoryAnalysisGrpcClientTest' --tests '*ForensicGatewayServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:test --tests '*RepositoryAnalysisDomainTest' --tests '*GitRepositoryCheckoutAdapterTest' --tests '*FileSystemRepositoryWorkspaceAdapterTest' --tests '*SafeGitCommandRunnerTest' --tests '*RepositoryAnalysisGrpcEndpointTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:test --tests '*SafeGitCommandRunnerTest' --tests '*GitRepositoryCheckoutAdapterTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:test --tests '*ForensicGatewayHttpAdapterTest' :services:repository-analysis-service:test --tests '*GitRepositoryCheckoutAdapterTest' --tests '*SafeGitCommandRunnerTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:forensic-gateway-service:test :services:forensic-gateway-service:jacocoTestReport :services:forensic-gateway-service:jacocoTestCoverageVerification :services:forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace; rg leakage gates; ./gradlew test --dependency-verification strict --console=plain --stacktrace; ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
qualityGateResult=PASS
commitHash=c4b2888b923b88ff1aad6713e0c52452030771e1
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=6b33f6378afd14e5f747904cf87a9c0c9d2fbdd8
arc42Updated=not required for Slice 05; OpenAPI and service README updated
adrUpdated=not required for Slice 05
```

## Implementation Status

Slice 05 implements the Gateway HTTP repository-analysis submission facade for
external HTTPS Git repositories. The Gateway validates request headers,
idempotency keys, repository references, requested outputs, build context and
workspace policy, maps accepted requests to the Repository Analysis gRPC
`PrepareRepository` contract, preserves correlation IDs and returns an accepted
repository-to-BTM submission envelope without exposing service-local workspace
paths.

Repository Analysis now rejects DNS-resolved loopback, link-local, site-local,
multicast, carrier-grade NAT and unique-local IPv6 addresses before executing
`git clone`, pins Git clone DNS resolution to the validated addresses, disables
Git HTTP(S) redirects, ignores symlinked Java source roots, and measures
checkout elapsed time with a monotonic clock. Gateway mutation headers now
reject unsupported characters before they are echoed in response headers or JSON
error envelopes.

## Slice 06 - Java AST Worker Handoff

### Review Evidence

Read-only Slice 06 reviews completed before Repository Analysis and Java AST
handoff files were changed:

- Senior gRPC Proto Specialist initially blocked production code because the
  producer-side Repository Analysis to Java AST transfer was not explicit in
  Slice 02 or Slice 03. A second gRPC contract review and Senior System
  Architect conflict review resolved the conflict: Slice 06 may proceed only
  with producer-pushed bounded inline source-file transfer using
  `repository-analysis.proto`, `java-ast-analysis.proto`, `analysis-job.proto`
  and the service communication matrix. Artifact-reference or source-package
  transfer remains out of scope.
- Senior Java Backend approved the bounded inline handoff path and required
  service-local generated gRPC code, relative source roots, bounded source
  files, checksum preservation, idempotency/correlation mapping and explicit
  unresolved-symbol diagnostics.
- Senior Analysis Storage Architect marked Analysis Store changes NOT_READY
  unless `byte_access` metadata preservation is fixed. Slice 06 therefore does
  not register Java AST output with Analysis Store and keeps artifact byte
  custody out of scope.
- Senior Tester required a deterministic Repository Analysis to Java AST
  handoff regression test, Java AST tests, Repository Analysis source-transfer
  tests, coverage/bootJar checks and the full `QUALITY.md` gate before
  checkpoint readiness.
- Final Senior Security Sandbox review blocked checkpoint readiness until the
  source-file collector enforced handoff resource limits before expensive work.
  The collector now streams `Files.walk` results without materializing every
  Java file path and checks `Files.size` before reading UTF-8 content.

### Verification

```text
./gradlew :services:repository-analysis-service:generateProto :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*RepositorySourceSnapshotHandoffServiceTest' --tests '*JavaAstAnalysisGrpcClientTest' --tests '*RepositoryAnalysisServiceApplicationTest' --tests '*RepositoryAnalysisServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace -> PASS after increasing the in-process Java AST client test deadline
./gradlew :services:repository-analysis-service:test :services:java-ast-analysis-service:test --tests '*JavaAstAnalysisContractTest' --tests '*JavaAstAnalysisGrpcEndpointTest' --tests '*JavaAstAnalysisApplicationServiceTest' --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew :services:repository-analysis-service:jacocoTestReport :services:repository-analysis-service:jacocoTestCoverageVerification :services:repository-analysis-service:bootJar :services:java-ast-analysis-service:jacocoTestReport :services:java-ast-analysis-service:jacocoTestCoverageVerification :services:java-ast-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace -> PASS
rg leakage gate for workspace paths in Repository Analysis handoff sources -> PASS; hits are existing path-validation code/tests and private adapter records, not public handoff output
./gradlew :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*JavaAstAnalysisGrpcClientTest' --dependency-verification strict --console=plain --stacktrace -> PASS after adding missing branch coverage for handoff adapter limits and worker response mapping
./gradlew :services:repository-analysis-service:jacocoTestReport :services:repository-analysis-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace -> PASS after branch-coverage regression tests
./gradlew test --dependency-verification strict --console=plain --stacktrace -> PASS
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace -> PASS after stopping a stale Gradle daemon lock from an earlier timed-out tool invocation
./gradlew :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*RepositorySourceSnapshotHandoffServiceTest' --tests '*JavaAstAnalysisGrpcClientTest' --dependency-verification strict --console=plain --stacktrace -> PASS after Security Sandbox blocker fix
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --no-daemon --max-workers=1 --dependency-verification strict --console=plain --stacktrace -> PASS after Security Sandbox blocker fix
git diff --check -> PASS
```

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v1
sliceId=06
sliceTitle=Java AST Worker Handoff
responsibleAgent=Workflow Executor with Senior gRPC Proto Specialist, Senior System Architect, Senior Java Backend, Senior Analysis Storage Architect and Senior Tester reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; contracts/grpc/README.md; contracts/grpc/java-ast-analysis.proto; services/java-ast-analysis-service/README.md; services/repository-analysis-service/README.md; services/repository-analysis-service/build.gradle.kts; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/filesystem/FileSystemSourceSnapshotFileCollector.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/grpc/JavaAstAnalysisGrpcClient.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/application/RepositorySourceSnapshotHandoffService.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/application/port/JavaAstAnalysisPort.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/application/port/SourceSnapshotFileCollectorPort.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/bootstrap/RepositoryAnalysisServiceConfiguration.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/bootstrap/RepositoryAnalysisServiceProperties.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/bootstrap/RepositoryAnalysisServicePropertiesConfiguration.java; services/repository-analysis-service/src/main/java/de/burger/forensics/analytics/services/repositoryanalysis/domain/RepositoryAnalysisDomain.java; services/repository-analysis-service/src/main/resources/application.properties; services/repository-analysis-service/src/main/resources/application-test.properties; services/repository-analysis-service/src/main/resources/application-docker.properties; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/filesystem/FileSystemSourceSnapshotFileCollectorTest.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/adapter/out/grpc/JavaAstAnalysisGrpcClientTest.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/application/RepositorySourceSnapshotHandoffServiceTest.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/bootstrap/RepositoryAnalysisServiceApplicationTest.java; services/repository-analysis-service/src/test/java/de/burger/forensics/analytics/services/repositoryanalysis/quality/RepositoryAnalysisServiceArchitectureTest.java
qualityGateCommands=./gradlew :services:repository-analysis-service:generateProto :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*RepositorySourceSnapshotHandoffServiceTest' --tests '*JavaAstAnalysisGrpcClientTest' --tests '*RepositoryAnalysisServiceApplicationTest' --tests '*RepositoryAnalysisServiceArchitectureTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:test :services:java-ast-analysis-service:test --tests '*JavaAstAnalysisContractTest' --tests '*JavaAstAnalysisGrpcEndpointTest' --tests '*JavaAstAnalysisApplicationServiceTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:jacocoTestReport :services:repository-analysis-service:jacocoTestCoverageVerification :services:repository-analysis-service:bootJar :services:java-ast-analysis-service:jacocoTestReport :services:java-ast-analysis-service:jacocoTestCoverageVerification :services:java-ast-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace; rg leakage gate; ./gradlew :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*JavaAstAnalysisGrpcClientTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:jacocoTestReport :services:repository-analysis-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; ./gradlew test --dependency-verification strict --console=plain --stacktrace; ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; ./gradlew :services:repository-analysis-service:test --tests '*FileSystemSourceSnapshotFileCollectorTest' --tests '*RepositorySourceSnapshotHandoffServiceTest' --tests '*JavaAstAnalysisGrpcClientTest' --dependency-verification strict --console=plain --stacktrace; ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --no-daemon --max-workers=1 --dependency-verification strict --console=plain --stacktrace; git diff --check
qualityGateResult=PASS
commitHash=adce9d721956d0a8172623ae36987e3d064e1971
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=c4b2888b923b88ff1aad6713e0c52452030771e1
arc42Updated=not required for Slice 06; service communication matrix already defines bounded inline source-file handoff
adrUpdated=not required for Slice 06
```

## Implementation Status

Slice 06 adds a producer-pushed Java AST handoff path inside Repository
Analysis. Repository Analysis now collects bounded Java source files from
verified relative source roots inside its private workspace, maps only relative
source-root and source-file paths plus UTF-8 content, checksum and byte size to
`java-ast-analysis-service`, and uses service-local generated gRPC classes.

The handoff does not expose workspace paths, does not add a Java AST pull API,
does not register Analysis Store metadata and does not use artifact-reference
source-package transfer. Unresolved Java symbol handling remains an explicit
Java AST diagnostic by forcing symbol-resolution diagnostics in the handoff
request.

## Slice 07 - Joern Worker Handoff

### Read-Only Review Evidence

Read-only Slice 07 reviews completed before any production file changes:

- Senior Swarm Orchestrator: dependency order selects Slice 07 after the pushed
  Slice 06 checkpoint, but implementation must not proceed until the execution
  report records the completed Slice 06 commit and push.
- Senior gRPC Proto Specialist: blocked Repository Analysis to Joern handoff
  because `joern-cpg-analysis.proto` exposes `SourceWorkspace.workspace_id` but
  does not define source-package transfer, artifact-byte retrieval,
  materialization RPC or another explicit owner API.
- Senior Joern CPG Specialist: blocked Joern execution because the current
  workspace adapter resolves a Joern-local filesystem workspace by
  `workspace_id`, while Repository Analysis only implements Java AST handoff and
  no Joern-safe materialization path.
- Senior Analysis Storage Architect: blocked semantic artifact registration
  because `AnalysisArtifactReference.byte_access` exists in the gRPC contract
  but is dropped by Analysis Store and Joern domain models and gRPC mappers.
- Senior Security Sandbox Engineer: blocked execution because proceeding would
  require either shared filesystem coupling or passing Repository Analysis
  private workspace identifiers into Joern.

### Stop Decision

Slice 07 is `BLOCKED` before implementation. Required unblock work:

- define a Repository Analysis to Joern source transfer path using bounded
  source-package transfer, artifact-byte retrieval or another explicit owner
  API;
- materialize transferred input into a Joern-owned workspace with a Joern-local
  identifier;
- preserve and validate `ArtifactByteAccess` across Repository Analysis,
  Joern and Analysis Store paths when artifact references are used;
- reject shared filesystem coupling, Repository Analysis private workspace IDs,
  symlink escapes and path-bearing diagnostics.

No Slice 07 files were modified, and no Slice 07 quality gate was run because
the slice stopped during read-only precondition review.

## Workflow Governance Update - v2

### Review Evidence

Read-only workflow-governance reviews completed before v2 documentation was
changed:

- Senior Workflow Architect: required a new workflow version because adding the
  unblock work changes workflow scope and dependencies; recommended preserving
  completed v1 Slice 00 through Slice 06 checkpoints and renumbering downstream
  v2 slices.
- Senior System Architect: required a dedicated source-package and Joern
  materialization contract before Joern implementation; flagged
  `build-artifact-worker-service` as a new service-boundary decision.
- Senior gRPC Proto Specialist: required additive contract changes, source
  snapshot and complete build-output package descriptors, byte-access
  preservation and contract tests before service communication implementation.
- Senior Analysis Storage Architect: required source package and build-output
  package ownership, `ArtifactByteAccess` preservation and metadata-only
  Analysis Store ownership.
- Senior Security Sandbox Engineer: required sandboxed fallback builds,
  manifest-first package validation, strict archive extraction rules, no
  Repository Analysis workspace IDs in Joern and no path-bearing diagnostics.
- Senior DevOps: required Jenkins and Artifactory to remain optional and
  skipped by default in local quality gates; Joern Docker may mount only
  Joern-owned materialized workspaces.

### Decision

Create workflow version `microservices-btm-pipeline-20260517-v2`.

V2 inserts:

```text
Slice 07 - Repository Snapshot And Build Artifact Worker Contract
```

The former v1 Slice 07 Joern Worker Handoff becomes v2 Slice 08. Downstream
slices are shifted by one through v2 Slice 17.

### CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v2
sliceId=workflow-governance-update-v2
sliceTitle=Repository Snapshot And Build Artifact Worker Contract insertion
responsibleAgent=Workflow Executor with Senior Workflow Architect, Senior System Architect, Senior gRPC Proto Specialist, Senior Analysis Storage Architect, Senior Security Sandbox Engineer and Senior DevOps reviews
changedFiles=docs/workflow/execution-report.md; docs/workflow/workflow.history.md; docs/workflow/workflow.md; docs/workflow/slice-dependency-map.md; docs/workflow/role-ownership.md; docs/workflow/quality-and-leakage-gates.md; docs/architecture/service-communication-matrix.md; docs/architecture/data-ownership.md; docs/architecture/service-boundaries.md; docs/architecture/service-migration-map.md; docs/architecture/target-microservices-architecture.md; docs/architecture/current-state.md; docs/architecture/current-build-and-test-map.md; docs/architecture/current-coupling-map.md; docs/architecture/monorepo-service-build-strategy.md; docs/architecture/contract-versioning.md; docs/arc42/03-system-scope-and-context.md; docs/arc42/05-building-block-view.md; docs/arc42/06-runtime-view.md; docs/arc42/07-deployment-view.md; docs/arc42/08-crosscutting-concepts.md
qualityGateCommands=git status --short --branch; git diff --stat; git diff --name-status; git diff --check; docs leakage checks; git diff --cached --check
qualityGateResult=PASS
commitHash=40726b7ab1904848b7fe4f399e5d22ac5efcfe2f
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=51954e322d4f29e17c607e66a929e5d174332e27
arc42Updated=checked and updated for source/build package and Joern materialization flow
adrUpdated=not required; ADR impact deferred unless build-artifact-worker-service is implemented as a service root
```

## Slice 07 Execution Attempt - v2

### Scope

Slice 07 implementation was resumed for
`microservices-btm-pipeline-20260517-v2` after the workspace/build-artifact
worker requirement refinement. The slice covers Repository Analysis source
package descriptors, complete build-output package descriptors, Analysis Store
byte-access preservation and Joern-owned workspace materialization from
validated package bytes.

### Review Evidence

- Senior Analysis Storage Architect, Senior Swarm Orchestrator, Senior Security
  Sandbox Engineer, Senior Tester, Senior Java Backend Engineer, Senior gRPC
  Proto Specialist, Senior System Architect and Senior DevOps reviewed the
  Slice 07 unblock scope before implementation.
- Post-fix Senior Security Sandbox review returned `READY` and confirmed that
  Joern now materializes only into Joern-owned workspace IDs, validates package
  manifests and archive bytes, rejects unsafe entries and preserves
  `ArtifactByteAccess`.

### Implementation Evidence

- Repository Analysis now exposes source-package and complete build-output
  package descriptors with ordered resolution candidates:
  Artifact Store/Artifactory, optional Jenkins and build-artifact-worker
  fallback.
- Analysis Store preserves artifact byte ownership, retrieval contract,
  retrieval reference and custody instead of storing only metadata references.
- Joern CPG Analysis now has a materialization RPC and a filesystem
  materializer that reads package bytes from the service-owned cache, verifies
  descriptor checksums, validates manifest entries, enforces quotas, rejects
  traversal/duplicate/unsafe archive entries and cleans incomplete workspaces.
- Joern tests cover byte-access roundtrip, descriptor rejection, checksum
  mismatch, unsafe manifest entries, archive traversal, unexpected entries,
  entry checksum mismatch and workspace quota rejection.

### Quality Evidence

```text
workflowVersion=microservices-btm-pipeline-20260517-v2
sliceId=07
sliceTitle=Repository Snapshot And Build Artifact Worker Contract
targetedGate1=./gradlew --no-daemon --max-workers=1 :services:joern-cpg-analysis-service:test --tests "*.FileSystemJoernWorkspaceMaterializerTest" --dependency-verification strict --console=plain --stacktrace
targetedGate1Result=PASS
targetedGate2=./gradlew --no-daemon --max-workers=1 :services:repository-analysis-service:test :services:analysis-store-service:test :services:joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace
targetedGate2Result=PASS
protoGate=./gradlew --no-daemon --max-workers=1 :services:repository-analysis-service:generateProto :services:analysis-store-service:generateProto :services:joern-cpg-analysis-service:generateProto --dependency-verification strict --console=plain --stacktrace
protoGateResult=PASS
analysisStoreRetest=./gradlew --no-daemon --max-workers=1 :services:analysis-store-service:test --dependency-verification strict --console=plain --stacktrace
analysisStoreRetestResult=PASS
fullGate=./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
fullGateResult=PASS
fullGateAttempt1=FAILED: services:analysis-store-service:test aborted while closing Gradle binary test-result writer with NoSuchFileException; no test XML failures found; isolated analysis-store-service:test passed immediately afterwards.
fullGateAttempt2=FAILED: Gradle DaemonStoppedException during clean phase after stop command received.
fullGateAttempt3=FAILED: WSL/Gradle build ended without test failure summary; dmesg showed WSL instance poweroff/termination sequence and no generated test XML failures were found.
fullGateAttempt4=FAILED: after WSL restart the full gate completed far enough to expose a real checkPackageCoverage blocker; package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem branch coverage was 69.63%.
coverageRemediation=Added focused FileSystemJoernWorkspaceMaterializer branch coverage for directory entries, missing package bytes, package size mismatch, malformed manifests, entry-kind mismatches, missing manifest entries and declared-byte overflow.
coveragePrecheck=./gradlew --no-daemon --max-workers=1 :services:joern-cpg-analysis-service:jacocoTestReport checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
coveragePrecheckResult=PASS
fullGateAttempt5=PASS: ./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
commitHash=d501773e8d30d8838591ec779b218c9aa06f72d4
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=ae6348e38c57e0522d4f8ee36b61c9046bc1bb16
arc42Updated=not changed in this attempt
adrUpdated=not required in this attempt
```

### D8 Decision

Slice 07 is `D8_PASS`. Targeted service and proto gates passed, post-fix
security review is ready, the Joern filesystem package coverage blocker was
remediated, and the mandatory full local quality gate completed successfully
after the WSL environment was restarted.

### Requirement Engineering Remediation - D8

Requirement review confirmed that no requirement or workflow clarification can
unblock D8 without weakening the active quality contract.

Classification:

- Functional: Slice 07 implementation remains limited to repository source
  packages, complete build-output package descriptors, artifact byte access
  preservation and Joern-owned materialization.
- Non-functional: deterministic source/build artifact handling and stable
  verification output remain required.
- Architecture constraint: service ownership and byte-custody boundaries remain
  governed by EPIC v0.2, ADR-0017 and the Slice 07 workflow.
- Resilience requirement: infrastructure instability must be reported as an
  environment blocker, not converted into a successful gate.
- Scalability requirement: no new scalability requirement is introduced by the
  D8 remediation.
- UX requirement: not affected.
- Observability requirement: the failed full-gate attempts must stay recorded
  with enough diagnostic detail to preserve auditability.
- Security requirement: post-fix security readiness does not replace the full
  `QUALITY.md` gate.
- Quality-gate requirement: the full local quality gate remains mandatory for
  Slice 07 checkpoint commit and push readiness.
- Assumption resolved: no test XML failure was found in the infrastructure
  attempts, and the stable post-restart full-gate run completed successfully
  after the concrete coverage blocker was remediated.
- Open question resolved: Slice 07 still matches EPIC v0.2 after the stable
  full-gate run because ownership remains split between Repository Analysis,
  Analysis Store and Joern CPG Analysis without introducing shared Java
  implementation modules.

Drift and conflict findings:

- No EPIC drift was identified in the D8 remediation itself. EPIC v0.2 remains
  the active source for producer-neutral Analytics ownership and evidence
  provenance.
- No service-ownership drift was accepted. Repository Analysis, Analysis Store
  and Joern ownership remains as described in the Slice 07 workflow.
- No quality-gate conflict was found that permits a waiver. `QUALITY.md`,
  ADR-0012, workflow-executor rules and D8 governance all block commit and push
  when a required gate fails or cannot be verified.
- Product-code remediation was requirement-compliant only after the stable WSL
  rerun exposed the concrete Joern filesystem package coverage failure. The
  remediation remained limited to observable materializer behavior and tests.

Completed remediation:

The full local quality gate was rerun in a stable WSL/Gradle environment:

```bash
./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

The command completed successfully. Slice 07 is ready for checkpoint commit and
branch push under the workflow-execute checkpoint rules.

## Slice 08 Execution - Joern Worker Handoff

Slice 08 implements the Joern CPG analysis handoff after the Slice 07
workspace and build-artifact contract precondition. The implementation keeps
Joern semantic artifacts inside the Joern CPG Analysis service until they are
registered with Analysis Store through the gRPC artifact registration contract.

Implemented behavior:

- Joern CPG Analysis registers collected semantic artifacts with Analysis
  Store through a service-owned outbound gRPC adapter.
- Runtime unavailable and timeout outcomes produce explicit UNKNOWN Joern
  provenance artifacts instead of fabricating missing CPG, data-flow or slice
  evidence.
- Required data-flow analysis expects both `dataflow.json` and `slices.json`,
  and Joern clears its service-owned output directory before every execution so
  stale artifacts cannot be registered as current evidence.
- Joern provenance writing rejects final-file symlinks and writes with
  create-new semantics so pre-existing symlinks cannot redirect provenance
  output.
- Joern runtime version output is accepted only for Joern-prefixed or
  semver-like values; unsafe paths, URIs and source-like snippets are recorded
  as UNKNOWN.
- Analysis Store validates artifact paths and byte-access references as public
  non-traversing references, including rejection of current-directory path
  segments.

### Slice 08 Reviews

- Security Reviewer: READY after provenance symlink and strict version-output
  remediation.
- Senior Joern CPG Specialist: READY after stale artifact cleanup remediation.
- Quality / ArchUnit Reviewer: READY; targeted gates pass and no threshold or
  architecture-rule weakening was found.
- Git Commit Reviewer: initial product-only staging was NOT READY because the
  Slice 08 CP_RECORD documentation duty was not staged. This CP_RECORD entry is
  staged before the product checkpoint commit as required by the workflow.

### Slice 08 CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v2
sliceId=08
sliceTitle=Joern Worker Handoff
responsibleAgent=Workflow Executor with Security Reviewer, Senior Joern CPG Specialist and Quality / ArchUnit Reviewer
changedFiles=services/analysis-store-service/**; services/joern-cpg-analysis-service/**; docs/workflow/execution-report.md
qualityGateCommands=./gradlew --no-daemon --max-workers=1 :services:joern-cpg-analysis-service:test :services:analysis-store-service:test --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:joern-cpg-analysis-service:jacocoTestReport :services:joern-cpg-analysis-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:repository-analysis-service:test :services:analysis-store-service:test :services:joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; git diff --check; git diff --cached --check
qualityGateResult=PASS
commitHash=bc102d119389604ab75e18b884015049a1da0108
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=7f7a7d5a9c14a83b0baa42c8a6a67a8373f2066a
arc42Updated=not changed in this slice
adrUpdated=not required in this slice
```

### Slice 08 D8 Decision

Slice 08 is `D8_PASS`. Targeted Joern and Analysis Store tests passed, package
coverage passed after focused branch-coverage remediation, Security / Joern /
Quality reviews are READY, `git diff --check` and `git diff --cached --check`
passed, and the mandatory full local quality gate completed successfully.

After `CP_COMMIT` and `CP_PUSH`, this CP_RECORD must be updated with the actual
checkpoint commit hash and push result.

## Slice 09 Execution - Instrumentation Target Planning

Slice 09 implements Analysis Store-owned instrumentation target planning from
accepted static source facts and registered static analysis artifacts. Target
planning stays deterministic, rejects unregistered artifact metadata, keeps
semantic-node mapping gaps explicit and returns target selections as planning
metadata rather than evidence.

Implemented behavior:

- Analysis Store exposes `PlanInstrumentationTargets` on the analysis-job gRPC
  contract.
- Target planning verifies requested source and semantic artifact references
  against the stored Analysis Store job before selecting targets.
- Probe kinds are deduplicated and sorted before target creation so planning
  output is stable for semantically identical policies.
- Target-limit truncation, unsupported fact types, missing accepted artifacts,
  incomplete artifacts and missing semantic mappings are reported as
  completeness-affecting diagnostics.
- Planning attributes reject secrets, URIs, traversal segments and local
  filesystem paths before they can be echoed in responses.

### Slice 09 Reviews

- Senior System Architect: PASS; scope stays inside Analysis Store,
  `contracts/grpc/**` and documentation, with no BTM production change and no
  shared Java implementation coupling.
- Senior Java Backend: PASS after idempotency and target-limit remediation.
- Analytics Persistence / Evidence Reviewer: PASS after registered artifact
  verification, probe normalization and path-safety remediation.

### Slice 09 CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v2
sliceId=09
sliceTitle=Instrumentation Target Planning
responsibleAgent=Workflow Executor with Senior System Architect, Senior Java Backend and Analytics Persistence / Evidence Reviewer
changedFiles=contracts/grpc/**; services/analysis-store-service/**; docs/workflow/execution-report.md
qualityGateCommands=./gradlew --no-daemon --max-workers=1 :services:analysis-store-service:generateProto --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:analysis-store-service:test --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:analysis-store-service:jacocoTestReport :services:analysis-store-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:btm-generation-service:test --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; git diff --check; cross-service implementation import scan
qualityGateResult=PASS
commitHash=b52f2dc33e8b67330f971556b68becbb2735186d
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=87eb5b939a52bb33849dcc1555551aab92af6d9f
arc42Updated=not changed in this slice
adrUpdated=not required in this slice
```

### Slice 09 D8 Decision

Slice 09 is `D8_PASS`. Targeted Analysis Store tests, Analysis Store package
coverage, BTM contract-consumer tests, cross-service implementation leakage
checks, specialist reviews, `git diff --check` and the mandatory full local
quality gate completed successfully.

## Slice 10 Execution - BTM gRPC File Delivery

Slice 10 implements bounded gRPC delivery of completed BTM rule and manifest
bytes from the BTM Generation service. Delivery remains producer-owned: BTM
Generation keeps byte custody until the `BtmArtifactDeliveryService` streams the
accepted artifacts, and Analysis Store only records the generated artifact
metadata and byte-access contract.

Implemented behavior:

- BTM Generation writes immutable producer-retained `.btm` rule artifacts and a
  stored rule manifest with `ArtifactByteAccess` pointing to the verified BTM
  delivery gRPC contract.
- The stored manifest records generated rule artifact declarations so delivery
  can bind accepted artifact metadata to the same generation run, job,
  target-selection and reproducibility metadata.
- `BtmArtifactDeliveryService.DownloadBtmArtifacts` streams a delivery manifest
  first, then bounded artifact chunks, then a `DELIVERED` status only after all
  selected artifact bytes have passed streamed size and SHA-256 verification.
- Server-side streaming installs cancellation and readiness handlers before
  delivery work starts on `ServerCallStreamObserver`, so cancelled or not-ready
  observers do not trigger large artifact reads before backpressure gates apply.
- Requested rule subsets still include and stream the selected stored manifest
  artifact, so subset delivery cannot report `DELIVERED` from unverified
  manifest bytes.
- Delivery rejects incomplete accepted handoffs when the stored manifest
  declares generated rule artifacts that are missing from
  `accepted_generated_artifacts`.
- Analysis Store accepts generated BTM artifact metadata with producer-retained
  byte access and rejects generated artifacts without byte-access metadata.
- BTM Generation preserves source fact artifact references, semantic artifact
  references and target-selection metadata in generated rules, manifests and
  responses.

### Slice 10 Reviews

- Senior gRPC Proto Specialist: initial review blocked on late
  cancellation/readiness setup, incomplete handoff acceptance and an
  imprecise delivery contract identifier. The blockers were remediated by
  preparing server streaming only after readiness, enforcing manifest-declared
  rule coverage and changing `BTM_DELIVERY_CONTRACT` to the verified
  `de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryService.DownloadBtmArtifacts`
  contract. Final re-review returned `PASS`.
- Analytics Persistence / Evidence Reviewer: initial review blocked on a
  stored-manifest subset-delivery gap where `DELIVERED` could be emitted after
  streaming only rule bytes. The blocker was remediated by always retaining the
  selected manifest artifact in delivery plans and streaming/verifying it before
  `DELIVERED`. Final re-review returned `PASS`.
- Quality / ArchUnit Reviewer: initial review blocked on forbidden service
  package globs that missed the actual Gateway and Ingestion package names.
  The blocker was remediated by using `services.gateway..` and
  `services.ingestion..`, with focused ArchUnit and full-gate verification.
  Final re-review returned `PASS`.

### Slice 10 CP_RECORD

```text
workflowVersion=microservices-btm-pipeline-20260517-v2
sliceId=10
sliceTitle=BTM gRPC File Delivery
responsibleAgent=Workflow Executor with Senior gRPC Proto Specialist, Analytics Persistence / Evidence Reviewer and Quality / ArchUnit Reviewer
changedFiles=services/btm-generation-service/**; services/analysis-store-service/src/test/java/de/burger/forensics/analytics/services/analysisstore/adapter/in/grpc/AnalysisJobGrpcEndpointTest.java; docs/workflow/execution-report.md
qualityGateCommands=./gradlew --no-daemon --max-workers=1 :services:btm-generation-service:test --tests "de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc.BtmArtifactDeliveryGrpcEndpointTest" --tests "de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryApplicationServiceTest" --tests "de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactReaderTest" --tests "de.burger.forensics.analytics.services.btmgeneration.quality.BtmGenerationServiceArchitectureTest" --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:btm-generation-service:test --tests "de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc.BtmGenerationGrpcEndpointTest" --tests "de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationApplicationServiceTest" --tests "de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomainTest" --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:analysis-store-service:test --tests "de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest" --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 :services:btm-generation-service:test :services:analysis-store-service:test :services:btm-generation-service:jacocoTestReport :services:btm-generation-service:jacocoTestCoverageVerification :services:analysis-store-service:jacocoTestReport :services:analysis-store-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; ./gradlew --no-daemon --max-workers=1 clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace; git diff --check; git diff --cached --check
qualityGateResult=PASS
commitHash=149374ecc784b7b14873fe4e1a8969f6d6ed1abe
pushResult=PUB_DONE to origin/feature/workflow-microservices-btm-pipeline-20260517
rollbackReference=d61e4c6644e379fba1567ae63d53032d380287ba
arc42Updated=not changed in this slice
adrUpdated=not required in this slice
```

### Slice 10 D8 Decision

Slice 10 is `D8_PASS`. Targeted BTM delivery tests, BTM generation tests,
Analysis Store artifact-registration tests, service coverage/package coverage,
specialist reviews, `git diff --check`, `git diff --cached --check`, the
mandatory full local quality gate and the slice checkpoint push completed
successfully on the final Slice 10 implementation state.
