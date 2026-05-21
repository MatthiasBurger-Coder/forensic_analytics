# arc42 Check Status

## Checked Files

- `docs/arc42/03-system-scope-and-context.md`
- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`

## Result

## v4 Workflow-Governance Check

The v4 workflow-governance refinement checked and updated the runtime and
ownership documentation for the new Slice 12 source-fact byte retrieval and
Java AST handoff contract:

- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/architecture/contract-versioning.md`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/service-migration-map.md`
- `docs/architecture/target-microservices-architecture.md`

The v4 check records that Java AST owns produced source-fact bytes until a
verified handoff or object-store contract transfers custody, Analysis Store
must retrieve source-fact bytes through the Java AST owner API using
service-local generated client stubs, Repository Analysis must expose Java AST
handoff completion through a reviewed gRPC service contract and deterministic
local fixtures must not require external Git network access, Docker, Jenkins,
Artifactory or credentials by default.

## v3 Workflow-Governance Check

The v3 workflow-governance refinement checked and updated the runtime and
ownership documentation for the new Slice 11 orchestration contract and
artifact-readiness bridge:

- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/architecture/contract-versioning.md`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/service-migration-map.md`
- `docs/architecture/target-microservices-architecture.md`

The v3 check records that Gateway remains a public facade, Analysis Store is
the preferred repository-to-BTM orchestration owner unless Slice 11 records a
stricter reviewed decision, Java AST source-fact artifacts require
`ArtifactByteAccess`, invalid Joern package descriptors produce incomplete
diagnostics, and public Gateway diagnostics must be allow-listed or redacted.

## v2 Workflow-Governance Check

No arc42 file required a production-claim update during `workflow create`.
Several arc42 and supporting architecture documents were checked for stale
slice-number references and aligned with the active workflow where the
references described current or future migration sequencing rather than
historical implementation evidence.

The checked arc42 material already records:

- plugins as producers and runtime binders;
- server-side repository analysis;
- server-side BTM generation;
- target microservice runtime flow;
- Gateway as a public facade that must not own repository-to-BTM worker
  orchestration state;
- no shared implementation modules between services;
- workflow create and workflow execute governance;
- planned-versus-implemented distinction for microservice paths.

## Supporting Architecture Documents Updated

- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/architecture/target-microservices-architecture.md`
- `docs/architecture/service-migration-map.md`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/service-boundaries.md`

## Follow-Up Rule

Later `workflow execute` slices must update arc42 when implementation changes
verified runtime behavior, service boundaries, deployment topology or
architecture decisions.
