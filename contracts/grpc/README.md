# gRPC Contracts

## Status

Planned gRPC contract root.

The active workflow uses these gRPC contract files for the microservice
boundary plan:

- `forensic-ingestion.proto`: extracted v1 ingestion compatibility contract,
  preserving current package, service methods, field numbers and enum numbers.
- `analysis-job.proto`: worker handoff, analysis-job state,
  instrumentation-target planning and Slice 11 repository-to-BTM
  orchestration owner contract.
- `repository-analysis.proto`: repository checkout, workspace preparation and
  Java AST source-snapshot handoff contract.
- `java-ast-analysis.proto`: JavaParser source-scanning and Java AST-owned
  source-fact artifact byte retrieval contract.
- `joern-cpg-analysis.proto`: provisional Slice 08 Joern CPG/CFG/DFG
  semantic artifact worker contract.
- `btm-generation.proto`: provisional server-side Byteman/BTM generation and
  public BTM artifact delivery contract for deterministic rules from delivered
  analysis facts and completed `.btm` file retrieval.

Generated Java classes from these contracts must be service-local build output.
They must not become shared Java DTO or domain modules.

`repository-analysis.proto` is intentionally stricter than the current
monolith-local checkout adapter. The networked service contract permits only
clean HTTPS repository URLs, keeps mutable workspace paths private to the
service and hands off source snapshots through opaque IDs, relative source
roots, artifact references, completeness and diagnostics.

Repository-analysis artifact references must be opaque artifact keys or
source-snapshot-relative paths. Absolute paths, `file:` URIs, workspace roots
and server-local paths are forbidden in public responses. Fields named
`safe_attributes` are allowlisted low-sensitivity metadata only; they must not
carry secrets, credentials, tokens, local/private paths, raw repository content
or unvalidated echoed request data.

The revision selector accepts branch-only, commit-only and branch-plus-commit
requests. Branch-plus-commit checkout must verify that the resolved commit
matches the requested commit and is reachable from the requested branch.
Workspace policies require positive bounded timeout and quota values; partial
clone and sparse checkout remain rejected until a later contract explicitly
enables them. Source snapshot IDs are deterministic for the sanitized repository
URL, requested revision, resolved commit and manifest artifact checksum.

`java-ast-analysis.proto` is intentionally limited to static Java source
analysis. Slice 06 uses it for producer-pushed, bounded inline source-file
handoff from Repository Analysis to Java AST Analysis. It accepts bounded source
files through the service boundary and returns source-fact artifact metadata
instead of unbounded inline facts.
Diagnostics distinguish parse errors, skipped or unsupported source roots and
the current `SYMBOL_RESOLUTION_NOT_CONFIGURED` limitation. Static AST output
must not be presented as runtime execution evidence.

Slice 12 adds the verified Java AST owner API
`GetSourceFactArtifactBytes`. `ArtifactByteAccess.retrieval_contract` for Java
AST source-fact artifacts names this RPC, and consumers such as Analysis Store
must retrieve bytes through service-local generated Java AST stubs with
expected checksum, expected size and bounded `max_bytes`. Repository Analysis
also exposes `AnalyzeSourceSnapshotWithJavaAst` so Java AST handoff completion,
artifact metadata, byte access, completeness and diagnostics cross the
Repository Analysis boundary through its gRPC contract rather than through
workspace paths or implementation imports.

`joern-cpg-analysis.proto` is intentionally limited to static semantic Joern
analysis. It accepts opaque workspace IDs, source snapshot IDs, relative source
roots and bounded policy data through the service boundary. It returns
CPG/CFG/DFG artifact metadata, provenance and diagnostics; missing or partial
artifact mappings remain explicit incompleteness. Static semantic output must
not be presented as runtime execution evidence.

`analysis-job.proto` also exposes Analysis Store-owned instrumentation target
planning. The planning RPC accepts bounded accepted static source facts,
accepted static artifact metadata and accepted semantic artifact references,
then returns deterministic target snapshots with selection fingerprints,
correlation, completeness and diagnostics. It must not fetch workspace paths,
run JavaParser or Joern, infer runtime execution or invent semantic-node
mappings when no verified semantic schema is available.

Slice 11 adds the Analysis Store-owned repository-to-BTM orchestration bridge
to `analysis-job.proto`. Gateway submits public repository-to-BTM requests to
`StartRepositoryToBtm` and reads status through `GetRepositoryToBtmStatus`.
The response is deliberately a readiness state: unavailable source/build
packages keep Joern skipped with explicit incomplete diagnostics, and completed
BTM bytes remain owned by BTM Generation until the public delivery path returns
them.

`btm-generation.proto` is intentionally limited to generated instrumentation
artifacts. It accepts accepted fact artifact references and bounded inline
instrumentation targets through the service boundary. Slice 03 records Analysis
Store as the target-selection metadata owner and BTM Generation as a bounded
target snapshot consumer. The contract also carries artifact byte-access
metadata so registered artifact metadata does not imply byte-custody transfer.
It must not expose repository URLs, workspace paths, source content or runtime
trace claims. Stable rule IDs are derived from source snapshot, target, probe
kind and rule schema version so identical inputs produce reproducible rules.

The public BTM artifact delivery RPC is additive. It streams a manifest and
bounded file chunks after Analysis Store metadata identifies accepted generated
artifacts. Gateway is a public facade and must not store canonical BTM bytes.
