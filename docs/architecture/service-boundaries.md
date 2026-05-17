# Service Boundaries

## Status

Slice 01 service-boundary baseline for the microservices ecosystem conversion
workflow.

These are planned target boundaries. They are not current implemented
microservices.

## Boundary Rules

Every service must have:

- a business responsibility;
- explicit non-scope;
- owned data or owned process responsibility;
- inbound and outbound communication through REST/OpenAPI, gRPC/protobuf or
  approved events;
- service-local domain models;
- service-local tests;
- service-local configuration;
- service-local health checks;
- service-local Dockerfile and README;
- no shared Java runtime implementation modules.

## `forensic-gateway-service`

Business capability: external API and facade for UI, CLI and external clients.

Owns:

- public API composition;
- client-facing analysis-job orchestration facade;
- service endpoint configuration;
- UI-facing status and error translation.

Non-scope:

- AST scanning;
- Joern execution;
- BTM generation;
- canonical evidence storage;
- graph database access;
- report rendering;
- direct worker-service orchestration logic beyond documented APIs;
- direct database access.

Inbound communication:

- REST/OpenAPI from frontend, CLI and external clients.

Outbound communication:

- REST/gRPC to ingestion, analysis store, graph/replay and report services
  after contracts exist.

Current evidence:

- current REST adapter exists in `forensic-analytics-rest`;
- current frontend API adapter uses `/api`;
- no gateway service exists yet.

Stop conditions:

- Gateway reads private service databases;
- Gateway imports worker service Java classes;
- Gateway contains analysis business logic;
- Gateway exposes unredacted evidence without security and audit decisions.

## `forensic-ingestion-service`

Business capability: receive and validate analysis evidence packages from
plugins, scanners and runtime collectors.

Owns:

- raw ingestion intake;
- upload-session lifecycle;
- request validation;
- rejected-ingestion diagnostics;
- handoff to analysis storage through contracts.

Non-scope:

- repository checkout;
- AST or Joern analysis;
- BTM generation;
- canonical normalized fact ownership;
- graph/replay projection;
- report generation.

Inbound communication:

- gRPC/protobuf from plugins, scanners and collectors.

Outbound communication:

- gRPC or event handoff to `analysis-store-service` after contracts exist.

Current evidence:

- `forensic-analytics-ingestion-grpc`;
- `forensic_ingestion.proto`;
- `ForensicIngestionGrpcService`;
- current module is not contract-only and depends on application code.

Stop conditions:

- generated protobuf Java becomes a shared service DTO module;
- ingestion writes canonical analysis facts directly without Analysis Store
  ownership;
- missing payload fields are silently invented;
- raw runtime values are persisted without redaction and retention decisions.

## `repository-analysis-service`

Business capability: prepare repository source input for analysis jobs.

Owns:

- repository checkout;
- branch and commit resolution;
- workspace leases;
- workspace cleanup;
- source snapshot preparation;
- checkout diagnostics.

Non-scope:

- AST scanning;
- Joern analysis;
- canonical fact storage;
- BTM generation;
- report generation.

Inbound communication:

- REST/gRPC analysis-preparation requests from Gateway or ingestion workflows
  through `contracts/grpc/repository-analysis.proto`.

Outbound communication:

- immutable source snapshot or artifact references to AST and Joern services
  through contracts.

Security boundary:

- the networked service contract accepts only clean HTTPS repository URLs;
- userinfo, credentials, query secrets, URL fragments, local paths, file URLs,
  SSH and SCP-style remotes are forbidden unless a later allowlist decision is
  documented;
- Git checkout must not execute hooks, submodules, build scripts, parsers,
  Joern, BTM generation or repository-supplied tools;
- mutable workspace paths remain private to the service. Cross-service handoff
  uses source snapshot IDs, relative source roots, artifact references,
  completeness and diagnostics only.
- public artifact references must be opaque or source-snapshot-relative. Generic
  `safe_attributes` metadata must not contain secrets, credentials, tokens,
  local or private paths, raw repository content or unvalidated echoed input.

Current evidence:

- `forensic-analytics-adapter-repository-source`;
- application ingestion workspace services;
- no independent repository-analysis service exists yet.

Stop conditions:

- another service accesses repository workspace internals directly;
- workspace paths are used as hidden cross-service coupling;
- checkout failure cases are undocumented;
- source snapshot identity is guessed.

## `java-ast-analysis-service`

Business capability: produce deterministic Java source facts from source
snapshots.

Owns:

- JavaParser execution;
- stable source identifiers;
- source locations;
- unresolved-symbol diagnostics;
- AST worker output until accepted by Analysis Store.

Non-scope:

- repository checkout;
- runtime execution truth;
- Joern CPG ownership;
- BTM generation;
- canonical store ownership.

Inbound communication:

- analysis-job or source-snapshot requests through contracts.

Outbound communication:

- source fact results to `analysis-store-service` through gRPC or events after
  contracts exist.

Current evidence:

- `forensic-analytics-adapter-javaparser`;
- `JavaParserSourceScanner`.

Stop conditions:

- static reachability is documented as runtime execution;
- unresolved symbols are dropped silently;
- parser-specific APIs leak into service-neutral contracts.

## `joern-cpg-analysis-service`

Business capability: run Joern and map CPG/CFG/DFG semantic artifacts.

Owns:

- Joern runtime invocation;
- Joern execution artifacts;
- semantic mapping diagnostics;
- Joern worker output until accepted by Analysis Store.

Non-scope:

- JavaParser source-fact ownership;
- BTM generation;
- canonical store ownership;
- graph/replay ownership.

Inbound communication:

- analysis-job or source-snapshot requests through contracts.

Outbound communication:

- semantic fact and artifact references to `analysis-store-service`;
- optional artifact access through owner APIs.

Current evidence:

- `forensic-analytics-adapter-joern-docker`;
- `docker/joern/**`.

Stop conditions:

- Joern local installation is required outside service/container behavior;
- incomplete semantic mappings are treated as confirmed facts;
- CPG artifacts become shared filesystem coupling.

## `btm-generation-service`

Business capability: generate deterministic Byteman/BTM rule artifacts from
delivered analysis facts.

Owns:

- rule-generation policy inside the service boundary;
- deterministic BTM artifact generation;
- rule ID stability;
- generated BTM artifact bytes until registered.

Non-scope:

- repository scanning;
- source fact creation;
- runtime trace invention;
- canonical fact storage.

Inbound communication:

- REST/gRPC generation requests containing contract-defined facts or references.

Outbound communication:

- generated artifact metadata and bytes through Analysis Store or artifact
  owner APIs after contracts exist.

Current evidence:

- `RuleGenerationPort`;
- `RuleGenerationRequest`;
- `RuleGenerationResult`;
- `.btm` tests and arc42 concepts.

Stop conditions:

- BTM generation scans repositories directly;
- generated rules are nondeterministic for the same input;
- rule IDs are guessed or unstable.

## `analysis-store-service`

Business capability: authoritative storage and query boundary for canonical
analysis facts and evidence state.

Owns:

- normalized analysis facts;
- canonical analysis sessions and jobs;
- correlations and indexes;
- incident records after contracts define them;
- artifact catalog metadata;
- workspace/project/audit/retention state unless later ownership docs assign
  a narrower owner.

Non-scope:

- graph database projection ownership;
- replay algorithms;
- report rendering;
- direct Joern execution;
- direct repository checkout;
- direct LLM invocation.

Inbound communication:

- writes from ingestion and worker services through owner APIs or events;
- reads from Gateway, graph/replay and report services through owner APIs.

Outbound communication:

- event publication or query responses after contracts exist.

Current evidence:

- `forensic-analytics-persistence` in-memory stores;
- application ports;
- domain analysis/session/artifact models.

Stop conditions:

- another service writes canonical facts directly;
- another service reads private database tables directly;
- persistence schema names are invented before a storage slice;
- evidence provenance, sensitivity or completeness is lost.

## `graph-replay-service`

Business capability: build graph/runtime overlays and reconstruct
exception-centered replay from available evidence.

Owns:

- graph projection stores;
- replay projection stores;
- replay query behavior;
- missing-evidence representation in replay outputs.

Non-scope:

- canonical evidence ownership;
- primary incident storage;
- report rendering;
- LLM package ownership.

Inbound communication:

- replay and graph requests from Gateway or report service through contracts.

Outbound communication:

- reads from Analysis Store owner APIs;
- replay responses to Gateway or report service.

Current evidence:

- arc42 graph/replay concepts;
- semantic graph domain model;
- no standalone graph/replay service exists yet.

Stop conditions:

- graph projection becomes source of truth;
- replay presents speculative paths as executed runtime paths;
- direct Analysis Store database access is introduced.

## `report-generation-service`

Business capability: produce reports, incident context packages and LLM-ready
or generated analysis packages.

Owns:

- report artifacts;
- incident context package structure;
- LLM-ready evidence packages;
- generated LLM output labels when live generation is later approved.

Non-scope:

- canonical evidence ownership;
- graph database ownership;
- Gateway orchestration;
- treating LLM output as verified evidence.

Inbound communication:

- report requests from Gateway through contracts.

Outbound communication:

- reads from Analysis Store APIs;
- reads from Graph Replay APIs;
- optional LLM provider calls only after provider and security decisions exist.

Current evidence:

- arc42 reporting and LLM concepts;
- no standalone report-generation service exists yet.

Stop conditions:

- generated text overwrites evidence;
- LLM output is labeled as confirmed fact;
- report service reads foreign private databases directly.

## `frontend-web-app`

Business capability: human-facing evidence review and analysis workflow UI.

Owns:

- browser UI;
- frontend state;
- client-side API adapters;
- accessibility and user workflow behavior.

Non-scope:

- forensic data ownership;
- direct worker-service communication;
- internal service orchestration;
- backend persistence or analysis logic.

Inbound communication:

- user interaction.

Outbound communication:

- REST through Gateway/public APIs only.

Current evidence:

- `forensic-ui`;
- `forensic-ui/src/adapters/api/**`;
- `forensic-ui/Dockerfile`.

Stop conditions:

- frontend calls internal worker services directly;
- frontend depends on generated internal service DTOs;
- UI presents hypotheses or LLM output as verified evidence.
