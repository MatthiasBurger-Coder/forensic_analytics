# Contracts

## Status

Planned external contract root.

This directory is contract-only. It may contain `.proto` files, OpenAPI files,
event schema documents and contract documentation. It must not contain Java
implementation code, generated Java classes, shared DTOs, shared domain
models, mappers, Spring configuration, fixtures, jars or shared runtime
libraries.

Slice 03 owns the first concrete contracts:

- `grpc/forensic-ingestion.proto`
- `grpc/analysis-job.proto`
- `grpc/repository-analysis.proto`
- `openapi/gateway-api.yaml`
- `events/analysis-events.md`

These files are interface descriptions only. Generated code must remain
service-local in later implementation slices.
