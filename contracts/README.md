# Contracts

## Status

Planned external contract root.

This directory is contract-only. It may contain `.proto` files, OpenAPI files,
event schema documents and contract documentation. It must not contain Java
implementation code, generated Java classes, shared DTOs, shared domain
models, mappers, Spring configuration, fixtures, jars or shared runtime
libraries.

The active workflow owns Gateway HTTP and public gRPC BTM delivery contract
updates in Slice 02, artifact-byte and instrumentation-target ownership
contracts in Slice 03, and the Analysis Store-owned repository-to-BTM
orchestration owner API in Slice 11. Slice 12 owns the Java AST source-fact
byte retrieval contract and Repository Analysis to Java AST handoff closure.
Existing contract files include:

- `grpc/forensic-ingestion.proto`
- `grpc/analysis-job.proto`
- `grpc/repository-analysis.proto`
- `grpc/java-ast-analysis.proto`
- `grpc/joern-cpg-analysis.proto`
- `grpc/btm-generation.proto`
- `openapi/gateway-api.yaml`
- `events/analysis-events.md`

These files are interface descriptions only. Generated code must remain
service-local in later implementation slices.
