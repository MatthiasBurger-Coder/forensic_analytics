# Service Boundaries

## Status

FA-MSA-001 Slice 04 service-boundary and data-ownership baseline.

These are target boundaries. Current `services/**` directories and
`forensic-analytics-*` modules are implementation evidence and migration
inputs. They must not be described as the completed FA-MSA-001 service
landscape until the workflow proves independent build, start, test,
configuration, healthcheck, container and deployment evidence.

## Boundary Rules

Every productive service must have:

- a business responsibility;
- explicit non-scope;
- owned data or owned process responsibility;
- inbound and outbound communication through REST/OpenAPI, gRPC/protobuf,
  approved events or documented file contracts;
- service-local domain models;
- service-local application/use-case code;
- service-local adapters;
- service-local configuration and bootstrap;
- service-local tests;
- service-local Dockerfile and README;
- no shared Java runtime implementation modules.

Forbidden:

- direct Gradle project dependencies between services;
- shared Java domain, DTO, repository, service, utility, fixture, logging,
  persistence or internal error-model modules;
- direct cross-service database access;
- private workspace, filesystem or object-prefix coupling;
- artifact byte custody outside the producing or accepting owner service
  without an explicit handoff or scoped-access contract;
- treating generated transport classes as shared domain models.

## `repository-source-service`

Business capability: prepare repository source input for analysis jobs.

Owns:

- repository access;
- branch and commit resolution;
- checkout, clone and fetch;
- workspace leases and cleanup;
- source package byte custody;
- source snapshot descriptors;
- accepted source snapshot metadata;
- checkout diagnostics.

Non-scope:

- JavaParser scanning;
- Joern analysis;
- BTM generation;
- report generation;
- incident analysis;
- canonical evidence persistence outside repository source metadata.

Inbound communication:

- analysis-preparation requests through a REST, gRPC or file contract defined
  before implementation depends on it.

Outbound communication:

- immutable source snapshot IDs, relative source roots, artifact references,
  source package retrieval contracts, completeness markers and diagnostics
  through explicit contracts.

Current evidence:

- `forensic-analytics-adapter-repository-source`;
- `services/repository-analysis-service`;
- `contracts/grpc/repository-analysis.proto`.

Stop conditions:

- another service accesses repository workspace internals directly;
- workspace paths are used as hidden coupling;
- checkout executes repository hooks, build scripts or repository-supplied
  tools without an approved sandbox decision;
- source snapshot identity is guessed.

## `ingestion-service`

Business capability: receive, validate and normalize analysis or runtime data.

Owns:

- gRPC or API intake;
- upload-session lifecycle;
- request validation;
- rejected-ingestion diagnostics;
- raw runtime or analysis payload byte custody before explicit handoff;
- accepted/rejected intake descriptors;
- handoff to the orchestrator or producing service owner through explicit
  contracts.

Non-scope:

- repository checkout;
- JavaParser scanning;
- Joern execution;
- report generation;
- canonical static Java source facts;
- canonical Joern semantic facts;
- orchestration state;
- generated report or LLM package state.

Inbound communication:

- gRPC/protobuf, REST or messaging from producers, scanners and runtime
  collectors.

Outbound communication:

- handoff to `analysis-orchestrator-service` for workflow coordination, or to
  the producing service owner named by an explicit contract.

Current evidence:

- `forensic-analytics-ingestion-grpc`;
- `forensic-analytics-ingestion-request`;
- `services/forensic-ingestion-service`;
- `contracts/grpc/forensic-ingestion.proto`.

Stop conditions:

- generated protobuf Java becomes a shared DTO module;
- missing payload fields are silently invented;
- raw runtime values are stored without redaction and retention decisions.

## `java-parser-analysis-service`

Business capability: produce deterministic Java AST/source facts.

Owns:

- JavaParser execution;
- class, method, call-site and branch extraction;
- stable source identifiers;
- source locations;
- unresolved-symbol diagnostics;
- canonical static Java source facts produced by the service;
- source-fact artifact bytes and producer-local catalog metadata.

Non-scope:

- repository checkout;
- Joern execution;
- runtime execution truth;
- global orchestration;
- canonical Joern semantic facts;
- artifact byte custody for other services;
- UI/report APIs.

Inbound communication:

- source snapshot or analysis requests through explicit contracts.

Outbound communication:

- source-fact metadata, diagnostics and retrievable artifacts through
  service-owned APIs, events or documented artifact contracts.

Current evidence:

- `forensic-analytics-adapter-javaparser`;
- `services/java-ast-analysis-service`;
- `contracts/grpc/java-ast-analysis.proto`;
- `contracts/grpc/java-ast-source-facts-v1.schema.json`.

Stop conditions:

- static reachability is documented as runtime execution;
- unresolved symbols are dropped silently;
- JavaParser APIs leak into domain, application or service-neutral contracts.

## `joern-analysis-service`

Business capability: run Joern and map CPG, CFG and DFG semantic artifacts.

Owns:

- Joern runtime invocation;
- Docker-based Joern integration when Docker is available and documented;
- CPG/CFG/DFG analysis;
- semantic artifact mapping and diagnostics;
- canonical Joern semantic facts produced by the service;
- Joern artifact bytes and producer-local catalog metadata.

Non-scope:

- JavaParser primary analysis;
- repository checkout;
- central API behavior;
- UI query models;
- canonical static Java source facts;
- artifact byte custody for other services;
- runtime trace truth.

Inbound communication:

- source snapshot, source package or materialization requests through explicit
  contracts.

Outbound communication:

- semantic fact metadata, mapping diagnostics and artifact references through
  approved contracts.

Current evidence:

- `forensic-analytics-adapter-joern-docker`;
- `services/joern-cpg-analysis-service`;
- `contracts/grpc/joern-cpg-analysis.proto`;
- `docker/joern/**`.

Stop conditions:

- Joern mounts another service's private workspace;
- incomplete mappings are treated as confirmed facts;
- CPG artifacts become shared filesystem coupling;
- Joern unavailability or timeout is hidden.

## `analysis-orchestrator-service`

Business capability: coordinate complete analysis workflows.

Owns:

- analysis job coordination;
- workflow status;
- worker leases and attempts;
- retry and timeout orchestration;
- failure categorization;
- dead-letter and unavailable-state coordination;
- job-to-artifact references;
- correlation of service results through explicit contracts.

Non-scope:

- repository checkout implementation;
- JavaParser scanning;
- Joern execution;
- report rendering;
- public query API ownership;
- canonical source, static analysis, semantic analysis or runtime evidence
  facts;
- artifact bytes and producer-local artifact catalogs;
- generated report packages or LLM-ready packages;
- private persistence owned by another service.

Inbound communication:

- public job requests from `query-report-api-service` or internal job requests
  from approved producers.

Outbound communication:

- REST/gRPC/messaging/file-contract calls to repository source, ingestion,
  JavaParser, Joern and query/report owners.

Current evidence:

- `forensic-analytics-engine`;
- orchestration portions of `forensic-analytics-application`;
- orchestration/status portions of `services/analysis-store-service`.

Stop conditions:

- the orchestrator becomes a new monolith by embedding other service
  responsibilities;
- orchestration state ownership is unclear;
- it reads private databases or workspaces directly.

## `query-report-api-service`

Business capability: expose public query, status and report APIs for clients.

Owns:

- REST/OpenAPI facade;
- client-facing status and error translation;
- report/query response assembly from owner APIs;
- public read models and cache state;
- generated report package state;
- LLM-ready package state;
- public response redaction.

Non-scope:

- analysis execution;
- repository checkout;
- JavaParser or Joern processing;
- private persistence access;
- canonical repository, ingestion, static analysis, semantic analysis or
  orchestration facts;
- artifact bytes outside generated report or LLM-ready packages;
- treating LLM output as verified evidence.

Inbound communication:

- REST/OpenAPI from UI, CLI and external clients.

Outbound communication:

- owner APIs for orchestration, status, reports, facts, graph/replay or
  artifacts after contracts exist.

Current evidence:

- `forensic-analytics-rest`;
- public API portions of `services/forensic-gateway-service`;
- `contracts/openapi/gateway-api.yaml`.

Stop conditions:

- public responses leak private paths, secrets, raw diagnostics or unverified
  hypotheses as evidence;
- API code performs analysis execution or direct worker orchestration;
- API code reads private service databases.

## `cli-client`

Business capability: command-line access to public platform APIs.

Owns:

- CLI parsing and output formatting;
- public API invocation;
- local developer interaction state only.

Non-scope:

- domain logic;
- analysis execution;
- parser logic;
- Joern control;
- persistence access;
- direct service implementation dependencies.

Current evidence:

- `forensic-analytics-cli`;
- `contracts/cli/gateway-cli-contract.md`.

Stop conditions:

- CLI imports service implementation classes;
- CLI depends on shared monolith domain/application modules after migration;
- legacy local commands are removed without parity or explicit deprecation
  tests.

## `observability-stack`

Business capability: deployment and operational observability.

Owns:

- logging configuration;
- metrics configuration;
- tracing configuration;
- dashboards;
- OpenTelemetry, Prometheus or Grafana configuration when introduced;
- deployment observability material.

Non-scope:

- shared Java logging library;
- domain or application behavior;
- evidence storage;
- hidden coupling between services.

Current evidence:

- `forensic-analytics-observability`;
- `forensic-analytics-logging`;
- deployment documentation.

Stop conditions:

- the stack becomes a shared Java runtime dependency;
- diagnostics expose secrets, private paths or raw sensitive runtime values;
- operational logs are treated as forensic evidence.

## `testbed`

Business capability: non-production integration and system test environment.

Owns:

- integration environment;
- Docker Compose or later deployment test environment;
- deterministic example repositories;
- end-to-end tests;
- test data.

Non-scope:

- production service behavior;
- shared service implementation code;
- central runtime dependency.

Current evidence:

- `forensic-analytics-testbed`;
- `deployment/docker-compose/repository-to-btm.local.yml`;
- service-local tests under current service slices.

Stop conditions:

- any production service depends on testbed source or fixtures;
- monolith regression coverage is removed before replacement service E2E
  exists;
- deployment commands are documented without verified files.

## Deferred Or Optional Boundaries

The following names are optional or deferred unless a later requirement makes
them mandatory:

| Boundary | Status |
|---|---|
| `btm-generation-service` | Existing partial service evidence exists, but FA-MSA-001 marks it optional. |
| `graph-replay-service` | Projection service candidate; not mandatory for FA-MSA-001 closure. |
| `report-generation-service` | Existing deferred report root; report/query responsibility moves first to `query-report-api-service`, while standalone report generation remains later work unless a new requirement makes it mandatory. |
| `incident-analysis-service` | Candidate for incident ownership; not mandatory for FA-MSA-001 closure. |
| `frontend-web-app` | Adjacent frontend boundary, not a mandatory FA-MSA-001 service root. |

## Cross-Cutting Stop Conditions

Stop a later migration slice when:

- service ownership is unclear;
- data ownership is unclear;
- a contract would require guessing fields or endpoints;
- a service would depend on another service's Java classes;
- shared Java modules are proposed;
- direct cross-service database access is proposed;
- runtime readiness is claimed without build, start, test, healthcheck,
  container and deployment evidence.
