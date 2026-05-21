# Repository To BTM Fixture v1

## Purpose

This fixture shape defines the deterministic local readiness path for the
repository-to-BTM workflow. It is intentionally metadata-only: it proves the
handoff contract between Gateway submission, Analysis Store orchestration,
Repository Analysis source snapshots, Java AST source-fact artifacts, Joern
availability, target selection, BTM generation request state, BTM manifest
state and public delivery readiness without requiring external Git network
access, Docker, Jenkins, Artifactory, credentials or host workspace mounts.

## Service-Local Resource Copies

Each consuming service owns a local copy under:

- `services/repository-analysis-service/src/test/resources/repository-to-btm/v1/source-fact-artifact.properties`
- `services/java-ast-analysis-service/src/test/resources/repository-to-btm/v1/source-fact-artifact.properties`
- `services/analysis-store-service/src/test/resources/repository-to-btm/v1/source-fact-artifact.properties`

The resource must not be moved to `contracts/**` and must not become a shared
Java fixture module. Service-local tests load their own copy through the
existing Gradle `test` task.

## Metadata Fields

Required fields:

- `schemaVersion`: `repository-to-btm-fixture-v1`
- `gatewaySubmissionState`: public Gateway submission state
- `analysisStoreOrchestrationState`: Analysis Store orchestration readiness
- `repositoryAnalysisSourceSnapshotId`: opaque source snapshot ID
- `repositoryAnalysisResolvedCommit`: synthetic commit-like value for stable
  fixture identity
- `repositoryAnalysisSourceRoot`: source-snapshot-relative Java root
- `sourceFactArtifactPath`: Java AST source-fact artifact path
- `sourceFactArtifactType`: Java AST source-fact artifact media type
- `sourceFactArtifactSha256`: expected SHA-256 checksum
- `sourceFactArtifactSizeBytes`: expected artifact size
- `sourceFactByteOwnerService`: Java AST owner service
- `sourceFactByteRetrievalContract`: Java AST owner RPC name
- `sourceFactByteRetrievalReference`: owner API retrieval reference
- `joernAvailability`: `UNAVAILABLE` or `AVAILABLE`
- `targetSelectionState`: Analysis Store target-planning state
- `btmGenerationRequestState`: BTM request readiness state
- `btmManifestState`: generated manifest state
- `btmDeliveryState`: public delivery state

The Java AST source-fact artifact referenced by `sourceFactArtifactType` uses
the external payload contract in
`contracts/grpc/java-ast-source-facts-v1.schema.json`. Service-local tests may
use synthetic source-fact JSON that follows this schema, but shared Java DTOs
or shared fixture modules are forbidden. Analysis Store tests must parse the
payload into Analysis Store-owned static fact models only.

No field may contain raw source code, local absolute paths, `file:` URIs,
credentials, tokens, private repository coordinates or host workspace details.
Missing build-output or Joern inputs remain explicit unavailable diagnostics.
