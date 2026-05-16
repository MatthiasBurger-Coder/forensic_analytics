# Microservice Governance Rules

These rules govern later microservice migration work. They are planning and
workflow rules until `workflow execute` updates the authoritative repository
artifacts.

## No Shared Java Code Modules

Microservices must not depend on shared business-code modules, shared repository
modules, shared domain modules, shared service modules, shared DTO modules,
shared internal error-model libraries, shared event implementation classes,
shared test fixtures or shared utility modules.

Allowed integration mechanisms:

- versioned OpenAPI specifications
- versioned protobuf files
- locally generated contract code per service
- external standard libraries
- build plugins that do not create runtime business coupling

Forbidden examples:

- `forensic-common`
- `shared-domain`
- `shared-dto`
- `shared-service`
- direct Java dependencies between microservices
- shared repository or persistence classes
- a shared internal Java error-model library

## Contract-First Rule

Every cross-service communication path must be described as a contract before
implementation starts.

Required contract data:

- protocol: REST, gRPC or approved eventing
- contract file
- version
- request model
- response model
- error model
- timeout behavior
- idempotency behavior when relevant
- streaming or batch behavior when relevant
- correlation and trace fields when runtime evidence flows across boundaries

## Runtime Independence Rule

A microservice is valid only when it can run independently from the monolithic
application.

Required evidence:

- own build
- own start command
- own test run
- own container
- own configuration
- own healthcheck
- clear ports
- no direct in-memory coupling to another service
- documented observability and diagnostics

## Slice Rule

Each migration slice must be small enough to review, test, revert and explain.

A slice may do one of these:

- prepare one service boundary
- introduce one contract
- add one adapter boundary
- extract one verified business path
- integrate one communication path
- add one test layer

A slice must not deeply change multiple services, split multiple persistence
models, change contracts and implementations together, or change build,
runtime and business logic broadly at the same time.

## Evidence Integrity Rule

Microservice migration must preserve forensic evidence semantics. Runtime facts,
trace correlation, stack frames, source relationships, graph edges, replay
steps and LLM output labels must not be invented, inferred as execution truth or
silently normalized away.
