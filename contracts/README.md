# Contracts

## Status

Planned external contract root.

This directory is contract-only. It may contain `.proto` files, OpenAPI files,
event schema documents and contract documentation. It must not contain Java
implementation code, generated Java classes, shared DTOs, shared domain
models, mappers, Spring configuration, fixtures, jars or shared runtime
libraries.

CLI-facing contract notes live under `cli/`. They describe command-to-contract
mapping only and do not authorize shared Java DTOs or public API implementation
dependencies in the CLI.

FA-MSA-001 Slice 03 aligns these contracts to the target service landscape.
Existing file names such as `openapi/gateway-api.yaml` and
`cli/gateway-cli-contract.md` are transitional predecessor names; their
FA-MSA-001 authorities are `query-report-api-service` and `cli-client`.
Orchestration contract authority belongs to `analysis-orchestrator-service`.
Canonical fact and artifact metadata ownership remains pending S04.
Existing contract files include:

- `grpc/forensic-ingestion.proto`
- `grpc/analysis-job.proto`
- `grpc/repository-analysis.proto`
- `grpc/java-ast-analysis.proto`
- `grpc/joern-cpg-analysis.proto`
- `grpc/btm-generation.proto`
- `openapi/gateway-api.yaml`
- `cli/gateway-cli-contract.md`
- `events/analysis-events.md`

These files are interface descriptions only. Generated code must remain
service-local in later implementation slices.
