# gRPC Contracts

## Status

Planned gRPC contract root.

Slice 03 introduces:

- `forensic-ingestion.proto`: extracted v1 ingestion compatibility contract,
  preserving current package, service methods, field numbers and enum numbers.
- `analysis-job.proto`: planned initial worker handoff and analysis-job
  contract.
- `repository-analysis.proto`: provisional Slice 06 repository checkout,
  workspace preparation and source-snapshot handoff contract.
- `java-ast-analysis.proto`: provisional Slice 07 JavaParser source-scanning
  contract for bounded source snapshot input, deterministic source-fact
  artifacts, counts and diagnostics.
- `joern-cpg-analysis.proto`: provisional Slice 08 Joern CPG/CFG/DFG
  semantic artifact worker contract.
- `btm-generation.proto`: provisional Slice 09 server-side Byteman/BTM
  generation contract for deterministic rules from delivered analysis facts.

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
analysis. It accepts bounded source files through the service boundary and
returns source-fact artifact metadata instead of unbounded inline facts.
Diagnostics distinguish parse errors, skipped or unsupported source roots and
the current `SYMBOL_RESOLUTION_NOT_CONFIGURED` limitation. Static AST output
must not be presented as runtime execution evidence.

`joern-cpg-analysis.proto` is intentionally limited to static semantic Joern
analysis. It accepts opaque workspace IDs, source snapshot IDs, relative source
roots and bounded policy data through the service boundary. It returns
CPG/CFG/DFG artifact metadata, provenance and diagnostics; missing or partial
artifact mappings remain explicit incompleteness. Static semantic output must
not be presented as runtime execution evidence.

`btm-generation.proto` is intentionally limited to generated instrumentation
artifacts. It accepts accepted fact artifact references and bounded inline
instrumentation targets through the service boundary. It must not expose
repository URLs, workspace paths, source content or runtime trace claims. Stable
rule IDs are derived from source snapshot, target, probe kind and rule schema
version so identical inputs produce reproducible rules.
