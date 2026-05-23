# Execution Report

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v2` |
| Requirement ID | `FA-MSA-001-LMR` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Status | S17 completed; continue with S18 |

## Creation Evidence

| Check | Result |
|---|---|
| Repository root | `/mnt/d/Projects/forensic_analytics` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Profile | `FULL_PATH` |
| Quality authority | `QUALITY.md` |
| Decision record | `READY_FOR_WORKFLOW` after S14 workflow-create refinement |

## Execution Log

| Slice | Title | Responsible role | Changed files | Quality gates | Result | Rollback reference | arc42 | ADR | Push |
|---|---|---|---|---|---|---|---|---|---|
| S00 | Execution Preflight And Evidence Freeze | Senior Execution Orchestrator with swarm-orchestrator subagent review | `docs/workflow/execution-report.md` | `git status --short --branch` PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; governing file `sha256sum` values match context pack | PASS | `9f6764665c121e2aa9a3b0863b0a167c25134dc9` | checked | checked | pushed |
| S01 | Current Caller And Dependency Revalidation | Senior System Architect with Senior Java Backend and Microservice Senior Expert review | `docs/architecture/current-coupling-map.md`; `docs/architecture/service-migration-map.md`; `docs/workflow/execution-report.md` | `git ls-files "*build.gradle.kts" \| xargs rg -n "project\\(\\\":forensic-analytics-"` PASS with non-empty evidence; production import scan PASS with non-empty evidence; test import scan PASS with non-empty evidence; `git diff --check` PASS | PASS: `NO_DELETION_SAFE` | `400c1f3` | checked | checked | pushed |
| S02 | Contract And Runtime Parity Gate | Contract-First API Steward with Senior gRPC/Proto, architecture and quality subagent reviews | `docs/architecture/service-communication-matrix.md`; `docs/architecture/target-microservices-architecture.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json`; `docs/workflow/execution-report.md` | `:services:repository-source-service:test` PASS; `:services:ingestion-service:test` PASS; `:services:java-parser-analysis-service:test` PASS; `:services:joern-analysis-service:test` PASS; `:services:analysis-orchestrator-service:test` PASS; `:services:query-report-api-service:test` PASS; `:services:cli-client:test` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: current transitional contract surface verified; full target runtime parity remains later-slice work | `be98793` | checked | checked | pushed |
| S03 | Repository Source Parity And Handoff Readiness | Senior Java Backend with Microservice Senior Expert, Security/Sandbox and Senior Tester subagent reviews | `services/repository-source-service/**`; `services/analysis-orchestrator-service/**`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | targeted repository-source tests PASS; targeted analysis-orchestrator tests PASS; `./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-adapter-repository-source:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped repository-source legacy import scan PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: repository-source service boundary hardened; local/file repository inputs explicitly deprecated at service boundary; Java AST analysis remains unimplemented here; no Proto contract mutation; no full orchestrator runtime-readiness claim | `09c423c` | checked | checked | pushed |
| S04 | Ingestion Service Parity And Handoff Readiness | Senior Java Backend with Senior gRPC/Proto, Ingestion Handoff, Microservice Senior Expert and Senior Tester subagent reviews | `services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/adapter/in/grpc/ForensicIngestionGrpcEndpointTest.java`; `services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/adapter/in/grpc/ForensicIngestionRequestValidatorTest.java`; `services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/application/IngestionApplicationServiceTest.java`; `services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/adapter/in/file/EngineIngestionRequestImporterTest.java`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | targeted S04 ingestion tests PASS; `./gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-ingestion-grpc:test :forensic-analytics-ingestion-request:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped ingestion-service legacy import scan PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS | PASS_WITH_LIMITATIONS: service-local accepted payload handoff, invalid stream rejection, validator edges and importer session-id custody are proven by tests; no production, Proto or event-contract changes; default accepted-ingestion handoff remains no-op, so no external handoff runtime is claimed; `AnalyzeRepository` remains `UNIMPLEMENTED`; legacy ingestion modules retained as rollback evidence | `7e3594c` | checked | checked | pushed |
| S05 | JavaParser Service Parity And Handoff Readiness | Senior Java Backend with Source Analysis, Microservice Senior Expert and Senior Tester subagent reviews | `services/java-parser-analysis-service/**`; `forensic-analytics-adapter-javaparser/**`; `docs/architecture/service-migration-map.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/08-crosscutting-concepts.md`; `services/java-parser-analysis-service/README.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | targeted S05 service tests PASS; targeted legacy JavaParser test PASS; `./gradlew :services:java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-adapter-javaparser:test --dependency-verification strict --console=plain --stacktrace` PASS; `git ls-files` based java-parser-analysis-service legacy import scan PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS | PASS_WITH_LIMITATIONS: source-fact artifact writes are immutable and idempotent for identical bytes; JavaParser service and legacy adapter projections are parity-tested; parse errors remain explicit diagnostics in the service rather than legacy `java-parse-error` facts; source-fact bytes preserve `STATIC_SOURCE_FACT`; no Proto change, no external handoff runtime claim, no Swarm/Kubernetes readiness claim and legacy adapter retained as rollback evidence | `3a039cd` | checked | checked | pushed |
| S06 | Joern Service Parity And Handoff Readiness | Senior Joern CPG Specialist with Senior Java Backend, Senior DevOps, Microservice Senior Expert and Senior Tester subagent reviews | `contracts/grpc/joern-cpg-analysis.proto`; `services/joern-analysis-service/**`; `.dockerignore`; `docs/architecture/service-migration-map.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/07-deployment-view.md`; `services/joern-analysis-service/README.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | targeted S06 service tests PASS; targeted legacy Joern Docker adapter test PASS; `./gradlew :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-adapter-joern-docker:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped joern-analysis-service legacy import scan PASS; `./gradlew :services:joern-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS | PASS_WITH_LIMITATIONS: Joern service now owns semantic artifact byte retrieval through `GetSemanticArtifactBytes`; artifact references no longer use the Analysis Store byte alias; runtime unavailable, timeout and missing artifact states stay explicit diagnostics with retryable status where appropriate; Docker build context allows the service boot jar; no `bootRun`, live health probe, Docker image build, Joern runtime smoke test, Compose/Swarm/Kubernetes readiness claim or legacy adapter removal | `36630ee` | checked | checked | pushed |
| S07 | Orchestration Service Parity And Application Split Readiness | Senior Java Backend with Distributed Systems, Data Ownership, Microservice and Senior Tester subagent reviews | `services/analysis-orchestrator-service/**`; `contracts/grpc/README.md`; `docs/architecture/service-migration-map.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/service-communication-matrix.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/07-deployment-view.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-engine:test :forensic-analytics-application:test :forensic-analytics-domain:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped analysis-orchestrator legacy import scan PASS; `git diff --check` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS | PASS_WITH_LIMITATIONS: Orchestrator now accepts `StartRepositoryToBtm` and serves `GetRepositoryToBtmStatus` as process-local pending readiness only; no worker dispatch, repository checkout, JavaParser, Joern, BTM generation, report rendering, artifact byte custody, durable persistence, event outbox, distributed orchestration or Docker runtime readiness is claimed; legacy engine/application/domain modules remain retained rollback evidence | `b853bc9` | checked | checked | pushed |
| S08 | Query Report API And Runtime Replacement Readiness | Senior Java Backend with Contract Governance, Senior DevOps, Senior React Frontend, Microservice Senior Expert and Senior Tester subagent reviews | `services/query-report-api-service/**`; `contracts/openapi/**`; `docs/architecture/service-migration-map.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/service-communication-matrix.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/07-deployment-view.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-rest:test :forensic-analytics-bootstrap:test :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped query-report-api-service legacy import scan PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `./gradlew :services:query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS | PASS_WITH_LIMITATIONS: Query Report API now points repository-analysis submission/status to S07 `analysis-orchestrator-service` pending readiness while preserving the public REST/OpenAPI shape; orchestrator `INCOMPLETE` plus `WAITING_FOR_REPOSITORY` maps to public `ACCEPTED`, `BTM_DELIVERY_NOT_READY` and incomplete diagnostics; no REST/bootstrap/Boot removal, report assembly, source snapshot availability claim, worker dispatch, repository checkout, JavaParser, Joern, BTM generation, artifact byte custody, Docker image build, Compose, Swarm or Kubernetes readiness is claimed | `0d5a112` | checked | checked | pushed |
| S09 | CLI Client Parity And Decoupling Readiness | Senior Java Backend with Contract Governance, Senior UX Designer and Senior Tester subagent reviews | `services/cli-client/**`; `forensic-analytics-cli/**`; `contracts/cli/**`; `contracts/openapi/**`; `docs/architecture/service-boundaries.md`; `docs/architecture/service-communication-matrix.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/07-deployment-view.md`; `docs/contracts/contract-test-plan.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :services:cli-client:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace` PASS; scoped cli-client legacy import scan PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace` FAILED at `:checkPackageCoverage` for pre-existing non-S09 branch coverage packages `services.analysisorchestrator.adapter.in.grpc`, `services.analysisorchestrator.application` and `services.joernanalysis.adapter.out.filesystem` | PASS_WITH_LIMITATIONS: S09 required and targeted gates pass; target `cli-client` remains an HTTP JSON public API client with no project-module dependencies; predecessor `forensic-analytics-cli gateway-submit` keeps compatibility behavior with redacted validation and public error output; CLI contract ownership is corrected to S09 and `buildContext.attributes={}` is explicit; `analyze`, `ingest-request`, status and report commands are not routed or added; local/private repository host policy remains owned by `query-report-api-service`; legacy CLI module remains rollback evidence; full local package-coverage repair is outside the S09 file locks and remains a separate quality blocker | `0288709` | checked | checked | pushed |
| S10 | Observability And Logging Replacement Readiness | Senior DevOps with Observability Runtime Diagnostics, Senior Java Backend, Security Threat Modeling, Senior System Architect, Senior gRPC/Proto and Senior Tester subagent reviews | `deployment/README.md`; `deployment/observability/README.md`; `deployment/observability/service-diagnostics-policy.yaml`; `services/observability-stack/README.md`; `services/observability-stack/src/test/java/de/burger/forensics/analytics/services/observabilitystack/ObservabilityStackPolicyTest.java`; `docs/arc42/08-crosscutting-concepts.md`; `docs/architecture/service-boundaries.md`; `docs/workflow/execution-report.md` | `./gradlew :services:observability-stack:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-logging:test :forensic-analytics-observability:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped productive service logging/observability import scan PASS; scoped productive service build-file leakage scan excluding non-production `services:testbed` PASS; `git diff --check` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS | PASS_WITH_LIMITATIONS: Productive `services/*/src/main` code has no shared `forensic-analytics-logging` or `forensic-analytics-observability` imports and productive service build files have no direct project dependency on those modules; `services:testbed` keeps test-scoped rollback dependencies and remains S13/S14 evidence, not a productive service dependency; `observability-stack` stays deployment/policy material and not a shared Java runtime module; diagnostics policy now tests allowed fields, redaction, missing values, diagnostic exposure and diagnostics-not-evidence semantics; no external telemetry service, Docker Compose, Swarm, Kubernetes, Prometheus, Grafana, OpenTelemetry collector or log-shipping readiness is claimed; `forensic-analytics-logging` and `forensic-analytics-observability` remain rollback evidence; ingestion upload correlation preservation is not certified because the current ingestion gRPC contract has no verified correlation carrier, and remediation belongs to a contract-authorized ingestion refinement slice | `240f498` | checked | checked | pushed |
| S11 | Persistence Ownership And Replacement Readiness | Senior Analysis Storage Architect with Data Ownership/Persistence, Senior Java Backend, Microservice Senior Expert and Senior Tester subagent reviews | `contracts/events/analysis-events.md`; `docs/architecture/data-ownership.md`; `docs/architecture/service-migration-map.md`; `docs/workflow/execution-report.md` | `./gradlew :forensic-analytics-persistence:test :services:analysis-orchestrator-service:test :services:query-report-api-service:test :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped productive service persistence import scan PASS; scoped affected-service build-file persistence dependency scan PASS; `git diff --check` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS | PASS_WITH_LIMITATIONS: S11 resolves the ownership ambiguity for central persistence-retirement planning and replaces event-contract placeholders with producer-local metadata and query-report ownership rules; productive target services do not import or build-depend on `forensic-analytics-persistence`; `services:testbed` keeps a test-scoped rollback dependency and legacy runtime callers remain, so physical removal stays blocked for S14; workspace/project administration, membership, asset, audit, retention and legacy project-storage behavior remain retained legacy evidence until a later owner requirement or explicit deprecation exists; no durable store, schema/table, event outbox, broker runtime, event-sourced/audit-grade ordering or artifact byte custody transfer is claimed | `65beb49` | checked | checked | pushed |
| S12 | Service-Local Domain And Application Readiness | Senior System Architect with Senior Java Backend, Microservice Senior Expert, ArchUnit Review and Senior Tester subagent reviews | `services/analysis-orchestrator-service/src/test/java/de/burger/forensics/analytics/services/analysisorchestrator/quality/AnalysisOrchestratorServiceBuildIsolationTest.java`; `docs/architecture/service-migration-map.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/07-deployment-view.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :forensic-analytics-domain:test :forensic-analytics-application:test --dependency-verification strict --console=plain --stacktrace` PASS; productive service domain/application import scan PASS; productive service build-file leakage scan excluding non-production `services:testbed` PASS; `./gradlew :services:analysis-orchestrator-service:test --tests "de.burger.forensics.analytics.services.analysisorchestrator.quality.AnalysisOrchestratorServiceBuildIsolationTest" --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: productive services prove service-local domain/application ownership with no central domain/application imports and no productive build-file dependency on `forensic-analytics-domain`, `forensic-analytics-application` or another `services:*` implementation project; per-service ArchUnit rules plus the S12 build-isolation regression cover forbidden dependencies; false future S15 deployment and quality-pass claims are corrected to requirements; central domain/application modules and `services:testbed` test-scoped dependencies remain rollback/regression evidence for S13/S14; no module deletion, no runtime rerouting and no Docker Compose, Swarm or Kubernetes readiness is claimed | `4a6bc36` | checked | checked | pushed |
| S13 | Service Testbed Parity And Monolith Coupling Readiness | Senior Tester with Microservice Senior Expert, Senior DevOps and Senior Java Backend subagent reviews | `services/testbed/src/main/.gitkeep`; `docs/architecture/current-coupling-map.md`; `docs/architecture/monolith-caller-retirement-plan.md`; `docs/architecture/service-boundaries.md`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/07-deployment-view.md`; `docs/testing/wildfly-hardening.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :forensic-analytics-testbed:test --dependency-verification strict --console=plain --stacktrace` PASS; S13 testbed package import scan PASS after adding a tracked empty `services/testbed/src/main` path; `docker compose -f deployment/docker-compose/repository-to-btm.local.yml config --quiet` PASS as model-syntax-only evidence; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: `services:testbed` and `forensic-analytics-testbed` have matching service-root and legacy testbed coverage after package relocation, including 6 Java test classes, 7 `@Test` methods, 2 default-skipped external scenarios and matching repository E2E resources; test data remains non-production and not forensic evidence; no production code or service build file depends on testbed; both testbeds intentionally retain 13 test-scoped legacy module dependencies as rollback/regression evidence, so S13 is not deletion approval; Compose validation is syntax-only and does not claim image build, startup, healthcheck, Swarm or Kubernetes readiness | `3dcc21b` | checked | checked | pushed |
| S14 | Retirement Readiness Reconciliation | Senior System Architect with Senior DevOps, Senior Java Backend, Microservice Senior Expert and Senior Tester subagent reviews | `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | legacy build-reference scan PASS with retained blocker evidence: 29 matches, including 16 `settings.gradle.kts` legacy registrations and 13 `services:testbed` test-scoped legacy dependencies; productive service main-source legacy import scan PASS with no matches; `services/testbed/src/test/java` legacy import scan PASS with retained blocker evidence: 74 import matches across 4 test files; `./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: S14 completes as `NO_REMOVAL_SAFE`; no modules, source trees, tests or Gradle registrations were removed; productive service code remains clean under S14 scans; non-production `services:testbed` retains legacy dependencies/imports as rollback and regression evidence; S15-S18 must replace or explicitly deprecate the remaining testbed, runtime, public API, boot/bootstrap and ownership blockers before S19 can attempt candidate-specific removal | `fbb1577` | checked | checked | pushed |
| S15 | Testbed Architecture And Hardening Relocation | Senior Tester with Senior Java Backend, Microservice Senior Expert, Senior DevOps and Senior System Architect subagent reviews | `services/testbed/**`; service-local `*ArchitectureTest.java`; `docs/architecture/current-build-and-test-map.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/07-deployment-view.md`; `docs/arc42/08-crosscutting-concepts.md`; `docs/testing/wildfly-hardening.md`; `services/testbed/README.md`; `docs/workflow/workflow.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | `./gradlew :services:testbed:test --tests "*WildFlyRepositoryHardeningTest" --dependency-verification strict --console=plain --stacktrace` PASS with WildFly external scenario default-skipped; `./gradlew :services:analysis-orchestrator-service:test :services:repository-source-service:test :services:ingestion-service:test :services:java-parser-analysis-service:test :services:joern-analysis-service:test :services:query-report-api-service:test :services:cli-client:test --tests "*ArchitectureTest" --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: broad logging and Spring architecture rules are removed from the legacy-dependent `services:testbed` classpath only after service-local replacement rules pass for the seven target services; Spring is restricted to each productive service `bootstrap..` package and forbidden from `cli-client`; `ingestion-service` also blocks central `forensic-analytics-logging`; `services:testbed` retains default-skipped WildFly hardening as non-production evidence; no module deletion, Docker image build, Compose startup, health probe, Swarm or Kubernetes readiness is claimed | `0e4a78d` | checked | checked | pushed |
| S16 | Testbed Runtime Scenario Replacement Or Deprecation | Senior Java Backend with Senior Tester, Microservice Senior Expert, Senior DevOps and Senior System Architect subagent reviews | `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/RepositoryAnalysisTestbedTest.java`; `services/testbed/README.md`; `services/cli-client/README.md`; `services/query-report-api-service/**`; `services/java-parser-analysis-service/**`; `services/joern-analysis-service/**`; `contracts/cli/gateway-cli-contract.md`; `docs/architecture/monolith-runtime-isolation.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/workflow/execution-report.md` | initial S16 testbed gate failed on brittle documentation-fragment assertions and was repaired; `./gradlew :services:testbed:test --tests "de.burger.forensics.analytics.services.testbed.RepositoryAnalysisTestbedTest.*" --dependency-verification strict --console=plain --stacktrace` PASS with Joern Docker smoke env unset; `./gradlew :services:cli-client:test :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :services:java-parser-analysis-service:test :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped `RepositoryAnalysisTestbedTest` legacy import scan PASS; `./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `git diff --check` PASS | PASS_WITH_LIMITATIONS: S16 retires the service-root in-process repository-analysis testbed scenario by explicit deprecation rather than replacement parity; target `cli-client` supports only `gateway-submit` and rejects local `analyze`, `ingest-request`, `status` and `report`; `query-report-api-service` remains accepted/pending and keeps jobs/results/replay/reports unavailable; JavaParser and Joern target services expose source-fact and semantic artifact contracts without BTM rules, report output, runtime facts or semantic graph parity; Joern Docker smoke is optional external verification, not default target runtime readiness; no `forensic-analytics-*` module, Gradle registration, Docker/Compose/Swarm/Kubernetes path or legacy rollback module was removed | `bf05d27` | checked | checked | pushed |
| S17 | Repository Checkout And Ingestion Testbed Replacement | Senior Java Backend with Senior gRPC/Proto, Ingestion Handoff, Microservice Senior Expert and Senior Tester subagent reviews | `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/RepositoryAnalysisMiniEndToEndTest.java`; `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/RepositoryAnalysisRealRepositoryEndToEndTest.java`; `services/testbed/README.md`; `services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/filesystem/FileSystemRepositoryWorkspaceAdapterTest.java`; `services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryCheckoutAdapterTest.java`; `services/ingestion-service/src/test/java/de/burger/forensics/analytics/services/ingestion/adapter/in/grpc/ForensicIngestionGrpcEndpointTest.java`; `services/analysis-orchestrator-service/src/test/java/de/burger/forensics/analytics/services/analysisorchestrator/adapter/in/grpc/AnalysisJobGrpcEndpointTest.java`; `services/ingestion-service/README.md`; `services/analysis-orchestrator-service/README.md`; `docs/architecture/monolith-runtime-isolation.md`; `docs/arc42/06-runtime-view.md`; `docs/workflow/execution-report.md`; `docs/workflow/context-pack.md`; `docs/workflow/context-pack.json` | initial S17 testbed gate failed on brittle documentation-fragment assertions and was repaired; initial orchestrator gate failed on a generic validation-description expectation and was repaired; `./gradlew :services:testbed:test --tests "de.burger.forensics.analytics.services.testbed.RepositoryAnalysisMiniEndToEndTest.*" --tests "de.burger.forensics.analytics.services.testbed.RepositoryAnalysisRealRepositoryEndToEndTest.*" --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace` PASS; `./gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace` PASS with pre-existing deprecated test API warning; `./gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace` PASS; scoped Mini/Real testbed legacy import scan PASS; `git diff --check` PASS; `python3 -m json.tool docs/workflow/context-pack.json >/dev/null` PASS; `./gradlew test --dependency-verification strict --console=plain --stacktrace` PASS | PASS_WITH_LIMITATIONS: S17 replaces the service-root mini and real repository E2E tests with explicit deprecation and target-service boundary evidence; `AnalyzeRepository`, local or file repository checkout, monolith analysis-session registration and direct workspace cleanup remain legacy rollback evidence only; `repository-source-service` owns clean HTTPS repository preparation, opaque workspace IDs, cleanup and source snapshot descriptors; `ingestion-service` proves `AnalyzeRepository` stays `UNIMPLEMENTED`; `analysis-orchestrator-service` rejects local/file/non-HTTPS repository-to-BTM inputs and remains pending `WAITING_FOR_REPOSITORY` without worker dispatch; no session-registration parity, source-package byte retrieval RPC, cross-service handoff runtime, module deletion, Docker image build, Compose startup, Swarm or Kubernetes readiness is claimed | `5aee0a0` | checked | checked | pushed |

## Pending Slice Status

| Slice | Status |
|---|---|
| S00 | COMPLETED |
| S01 | COMPLETED |
| S02 | COMPLETED |
| S03 | COMPLETED |
| S04 | COMPLETED |
| S05 | COMPLETED |
| S06 | COMPLETED |
| S07 | COMPLETED |
| S08 | COMPLETED |
| S09 | COMPLETED |
| S10 | COMPLETED |
| S11 | COMPLETED |
| S12 | COMPLETED |
| S13 | COMPLETED |
| S14 | COMPLETED |
| S15 | COMPLETED |
| S16 | COMPLETED |
| S17 | COMPLETED |
| S18 | NEXT |
| S19 | PENDING |
| S20 | PENDING |

## Notes

Direct deletion of the listed legacy modules remains blocked until execution
records caller-free proof, replacement parity, rollback or deprecation notes
and the required quality-gate results.

S00 confirms the active branch is
`architecture/workflow-legacy-module-retirement-20260522`, the local branch ref
exists, the working tree was clean before S00 report documentation, context
pack JSON is valid and governing-file hashes match the recorded context pack.

S01 confirms direct deletion is still unsafe. The current inventory finds 72
direct legacy Gradle project references, 653 production legacy-package imports
and 628 test legacy-package imports. `forensic-analytics-testbed` and
`services:testbed` each still test-depend on 13 retained legacy modules.

S02 confirms the current transitional contract surface is testable and the
repository minimum test gate passes. It does not claim full target runtime
parity: OpenAPI still contains planned operations, event contracts are design
artifacts, query-report submission/status still uses predecessor
`analysis-store-service` behavior, and CLI status/report mappings remain later
CLI work.

S02 also refreshed the context-pack architecture hashes because S01 and S02
changed architecture evidence that later slices must re-read.

Workflow-create refinement after the S03 execution blocker changes S03 through
S13 from early deletion slices into parity, handoff, caller-migration and
replacement-readiness slices. The later S14 execution blocker changes S14 into
a no-deletion readiness reconciliation gate and moves physical module
deregistration/source-tree removal to S19 after S15 through S18. Resume
execution at S14 after the refinement checkpoint.

S03 confirms the repository-source service is not a Java parser owner and does
not implement `AnalyzeSourceSnapshotWithJavaAst`. The slice hardens service
metadata against private path leakage, keeps Git command HOME under the
prepared workspace, preserves legacy local/file repository behavior only as
predecessor regression evidence, and requires callers to use source snapshot
IDs plus artifact references instead of private workspace paths. S03 does not
claim Swarm, Kubernetes or full orchestration runtime readiness; those remain
later-slice concerns.

S04 confirms ingestion-service intake and validation parity through service
tests that record accepted payload handoff only after successful validation and
deduplication. Invalid streamed uploads do not trigger handoff, importer
handoffs preserve the produced session ID, and legacy ingestion modules still
pass as rollback evidence. S04 does not mutate gRPC, Proto or event contracts
and does not claim an external runtime handoff because the default service
handoff adapter remains no-op until a later approved slice wires a real
consumer.

S05 confirms the JavaParser service owns service-local static Java source-fact
production without importing monolith application, domain or adapter modules.
Artifact retrieval references are immutable: identical repeated writes are
idempotent and conflicting bytes for an existing reference are rejected before
published evidence can be replaced. The slice records the intentional migration
from legacy `java-parse-error` source facts to service diagnostics and keeps
unresolved-symbol limitations completeness-affecting. It does not remove
`forensic-analytics-adapter-javaparser`, mutate the gRPC contract or claim
Swarm/Kubernetes readiness.

S06 confirms the Joern service owns service-local runtime invocation,
Joern-owned workspace materialization, CPG/CFG/DFG artifact production,
provenance, diagnostics and semantic artifact byte retrieval. The service
extends `joern-cpg-analysis.proto` with `GetSemanticArtifactBytes`, rejects
private or mismatched artifact-byte requests, exposes retryable timeout and
unavailable diagnostics, and keeps artifact bytes behind Joern-owned public
references instead of the previous Analysis Store byte alias. The legacy
`forensic-analytics-adapter-joern-docker` remains rollback evidence and is not
removed. Docker image build and live Joern smoke testing remain optional
external checks and were not claimed by S06.

S08 confirms the public Query Report API facade now calls
`analysis-orchestrator-service` for repository-to-BTM submission/status while
keeping the OpenAPI/JSON shape stable for current clients. The facade maps
orchestrator pending readiness to public `ACCEPTED`, preserves incomplete
diagnostics, reports `analysis-orchestrator-service` as the downstream status
dependency and keeps legacy REST/bootstrap/Boot modules as rollback evidence.
S08 does not claim completed analysis parity, source snapshot availability for
pending runs, worker dispatch, repository checkout, JavaParser, Joern, BTM
generation, report assembly, artifact byte custody, Docker image build,
Compose, Swarm or Kubernetes readiness.

S09 confirms `services/cli-client` remains a target public API client for the
transitional `gateway-submit` command and does not import monolith or service
implementation packages. The predecessor `forensic-analytics-cli` keeps
`analyze` and `ingest-request` as local in-process rollback evidence, while its
`gateway-submit` path now matches target redaction behavior for malformed
gateway/repository URL, numeric and boolean inputs. S09 does not add status or
report commands, does not route legacy local commands to the public API and
does not remove `forensic-analytics-cli`. Local/private repository host
rejection remains a `query-report-api-service` validation responsibility rather
than duplicated CLI policy.

The full local `QUALITY.md` gate was run after S09 and failed only at
`:checkPackageCoverage` for packages outside the S09 file locks:
`de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc`,
`de.burger.forensics.analytics.services.analysisorchestrator.application` and
`de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem`.
S09 does not change those packages, and this report does not claim that the full
local gate passed.

S10 confirms observability/logging replacement readiness only for productive
service decoupling from shared Java logging and observability modules. The
slice adds deterministic policy coverage for `observability-stack`, expands
the deployment diagnostics policy with explicit correlation and trace-context
fields, missing-value handling, sensitive-value rejection and diagnostic
surface exposure rules, and corrects stale S12 provenance references to the
active S10 slice.

S10 does not remove `forensic-analytics-logging` or
`forensic-analytics-observability`. It also does not modify
`contracts/grpc/forensic-ingestion.proto`, `services/ingestion-service/**` or
legacy ingestion modules. Security and gRPC contract reviews verified that the
current ingestion upload contract has no request correlation carrier and no
verified gRPC metadata/header alternative. That gap remains a contract-owned
ingestion refinement item; S10 must not be read as certifying end-to-end
ingestion upload correlation preservation.

S11 confirms productive target services do not use the shared
`forensic-analytics-persistence` Java module. The slice clarifies ownership for
legacy persistence areas that were still ambiguous: workspace/project
administration, workspace/project memberships, workspace/project assets,
workspace audit events, retention policies and legacy project-storage path
resolution are retained legacy evidence outside mandatory FA-MSA-001
repository-to-BTM acceptance until a later requirement assigns an owner or
explicitly deprecates them.

S11 also replaces event-contract placeholder wording for artifact metadata and
report artifact ownership. In the FA-MSA-001 target flow, there is no shared
canonical artifact metadata store: producer services own producer-local
artifact metadata, `analysis-orchestrator-service` may keep job-to-artifact
references only, and `query-report-api-service` owns public projection or
generated-package metadata only after owner-authorized access. S11 does not
claim durable persistence, event sourcing, broker readiness, event outbox
behavior, audit-grade ordering or final caller-free persistence-module
retirement.

S12 resolves the quality-review STOP on build-level proof by extending
`AnalysisOrchestratorServiceBuildIsolationTest` to scan every productive
service build file and fail on central domain/application or cross-service
project dependencies. `services:testbed` is explicitly excluded because it is
non-production regression and rollback evidence owned by S13/S14. S12 also
refreshes stale service-migration context and corrects premature S15 closure
wording in architecture and arc42 documentation.

S13 resolves the workflow gate failure caused by the missing
`services/testbed/src/main` path by adding a tracked empty marker without
production Java source. The slice verifies that service-root testbed coverage
matches the legacy `forensic-analytics-testbed` tests and resources after
package relocation, while documenting that both testbeds still keep 13
test-scoped legacy dependencies. This is parity and rollback evidence only; it
does not authorize legacy module removal.

S14 deletion review stopped with a real blocker. Senior DevOps, Senior System
Architect, Senior Java Backend, Microservice Senior Expert and Senior Tester
reviews agreed that source-tree deletion is unsafe while
`settings.gradle.kts` still registers all 16 listed legacy modules and
`services:testbed` keeps 13 test-scoped legacy dependencies plus legacy
imports in `RepositoryAnalysisTestbedTest`,
`RepositoryAnalysisMiniEndToEndTest`,
`RepositoryAnalysisRealRepositoryEndToEndTest` and
`WildFlyRepositoryHardeningTest`. Productive services remain clean under the
checked main-source and productive-build scans. Workflow-create refinement
therefore changes S14 into a no-deletion `NO_REMOVAL_SAFE` readiness gate,
adds S15 through S18 migration/deprecation slices, moves deletion to S19 and
moves closure to S20.

S17 confirms that the remaining mini and real repository testbed E2E behavior
cannot be treated as target-service parity. Senior Java Backend, Senior
gRPC/Proto, Ingestion Handoff, Microservice Senior Expert and Senior Tester
reviews all blocked false parity claims for `AnalyzeRepository`, local/file
repository checkout, monolith session registration and direct workspace
cleanup. The slice therefore rewrites the two service-root testbed classes into
boundary/deprecation evidence and strengthens target-service tests instead of
removing modules. Remaining deletion candidates must still pass S18 ownership
exit and S19 caller-free removal checks.
