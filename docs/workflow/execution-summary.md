# Execution Summary

## Workflow Creation Status

Status: created.

Branch:

```text
architecture/microservices-ecosystem-conversion-20260516
```

Created artifacts:

- `docs/workflow/README.md`
- `docs/workflow/workflow.md`
- `docs/workflow/three-amigos-decision-record.md`
- `docs/workflow/current-state-baseline.md`
- `docs/workflow/governance-conflict-review.md`
- `docs/workflow/slice-dependency-map.md`
- `docs/workflow/agent-handoff-matrix.md`
- `docs/workflow/quality-gate-plan.md`
- `docs/workflow/execution-summary.md`
- `docs/workflow/prompts/microservices-ecosystem-conversion.md`

## Creation Notes

- The previous active workflow under `docs/workflow/**` was replaced as
  required by the workflow-authoring rules.
- No production code was changed during workflow creation.
- No service directories, contracts, deployment manifests or frontend moves were
  created during workflow creation.
- The user draft's German content was normalized into English repository
  documentation.

## Open Execution Preconditions

- Run `workflow execute` before implementation starts.
- Confirm that final commit and push remain desired before Slice 20 executes.
- Verify local Docker and Kubernetes tooling before requiring Docker or
  Kubernetes commands.
- Verify service-specific Gradle or service-local build commands after each
  service is actually added.

## Latest Known Verification

Executed during workflow creation:

```bash
git diff --check
```

Result: passed.

Gradle quality gates were not run during workflow creation because this slice
changed only workflow documentation and did not modify production code, tests,
build logic, contracts or deployment descriptors.

## Workflow Execution Status

Execution started from `workflow execute` on branch
`architecture/microservices-ecosystem-conversion-20260516`.

Completed slices:

- Slice 00 documented the current repository state, current coupling and build
  baseline.
- Slice 01 documented the target microservice architecture, service boundaries,
  communication matrix, data ownership and ADR-0017.
- Slice 02 prepared placeholder service, frontend, contract and deployment
  roots without moving monolith production code.
- Slice 03 added provisional logical contracts under `contracts/` and ADR-0018.
- Slice 04 implemented `forensic-ingestion-service` as an independent Spring
  Boot service with service-local gRPC generation, tests, health check and
  Dockerfile.
- Slice 05 implemented the initial `analysis-store-service` boundary for
  `AnalysisJobService` job lifecycle and artifact metadata registration.
- Slice 06 implemented the initial `repository-analysis-service` boundary for
  clean-HTTPS repository preparation, service-owned workspaces, Git checkout,
  source-root detection and opaque source snapshot handoff.
- Slice 07 implemented the initial `java-ast-analysis-service` boundary for
  JavaParser AST method fact extraction, explicit parse diagnostics,
  provisional gRPC source snapshot handoff and deterministic source fact
  artifact writing.
- Slice 08 implemented the initial `joern-cpg-analysis-service` boundary for
  service-contained Joern CPG/CFG/DFG semantic artifact generation, provenance,
  explicit incompleteness diagnostics and provisional gRPC source snapshot
  handoff.

The user clarification from 2026-05-16 permits plausible provisional
inter-service communication contracts when final details are not yet defined,
provided the communication remains logically consistent and documented.

## Slice 05 Verification

Executed for `analysis-store-service`:

```bash
./gradlew --no-daemon :services:analysis-store-service:test :services:analysis-store-service:jacocoTestReport :services:analysis-store-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:analysis-store-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/analysis-store-service/Dockerfile -t analysis-store-service:slice05 .
git add -N .
git diff --check
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: all passed.

The Docker image built as:

```text
analysis-store-service:slice05
sha256:36fa2db17d1eb5444537185e67c844ff78fff276705c3a1bd5e33f551158739b
```

Scope note: Slice 05 intentionally does not implement durable database
migrations, normalized fact schemas, incident records or correlation indexes.
Those remain later slices requiring explicit storage and contract decisions.

Post-review Slice 05 blockers were resolved before completion:

- in-memory lifecycle mutations are serialized in the application service, so
  concurrent workers cannot lease the same dispatchable or retryable job twice;
- retryable jobs are included in leasing again after retryable failure;
- `ListAnalysisJobs` uses explicit offset page tokens and returns
  `next_page_token` when a response is truncated;
- accepted job schema version, correlation ID, attributes and percent progress
  are preserved in the service-owned model and gRPC response;
- workflow and service documentation now describe the narrower Slice 05 scope
  instead of claiming durable normalized fact storage or cluster deployment
  readiness.

## Slice 06 Refinement Status

Slice 06 read-only pre-implementation review found that the service boundary is
valid but required security and contract refinement before implementation.

Refinements recorded before service code:

- `contracts/grpc/repository-analysis.proto` defines the provisional
  repository-preparation gRPC contract.
- Slice 06 routing now requires Security Sandbox Specialist review.
- Slice 06 write scope explicitly allows the repository-analysis contract,
  `settings.gradle.kts`, and root `build.gradle.kts` only for generated
  protobuf coverage exclusion.
- The repository-analysis contract uses clean HTTPS repository URLs only and
  forbids userinfo, embedded credentials, query secrets, URL fragments, local
  paths, file URLs, SSH and SCP-style remotes.
- Public handoff is limited to opaque workspace IDs, source snapshot IDs,
  relative source roots, artifact references, completeness and diagnostics.
  Private workspace paths remain service-internal.
- Public artifact references are opaque keys or source-snapshot-relative paths,
  never absolute workspace paths or `file:` URIs.
- `safe_attributes` are low-sensitivity metadata only and must reject or redact
  secrets, credentials, tokens, local/private paths, raw repository content and
  unvalidated echoed input.
- Branch/commit modes, bounded workspace policies and deterministic source
  snapshot identity are now documented in the repository-analysis contract and
  contract-test plan.

## Slice 06 Verification

Executed for `repository-analysis-service`:

```bash
./gradlew --no-daemon :services:repository-analysis-service:test :services:repository-analysis-service:jacocoTestReport :services:repository-analysis-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:repository-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/repository-analysis-service/Dockerfile -t repository-analysis-service:slice06 .
python3 - <<'PY'
import re, yaml
from pathlib import Path
data=yaml.safe_load(Path('contracts/openapi/gateway-api.yaml').read_text())
patterns=[
 data['components']['schemas']['StartRepositoryAnalysisRequest']['properties']['repositoryUrl']['pattern'],
 data['components']['schemas']['RepositoryAnalysis']['properties']['repositoryUrl']['pattern'],
 data['components']['schemas']['RepositoryAnalysis']['properties']['resolvedRemoteUrl']['pattern'],
]
negative=[
 'https://LOCALHOST/repo.git',
 'https://127.0.0.1/repo.git',
 'https://10.0.0.1/repo.git',
 'https://172.16.0.1/repo.git',
 'https://192.168.0.1/repo.git',
 'https://[FC00::1]/repo.git',
 'https://[FE80::1]/repo.git',
 'https://[::ffff:127.0.0.1]/repo.git',
 'https://[::ffff:10.0.0.1]/repo.git',
]
positive=['https://example.com/acme/repo.git']
for pattern in patterns:
    compiled=re.compile(pattern)
    assert all(not compiled.fullmatch(value) for value in negative)
    assert all(compiled.fullmatch(value) for value in positive)
PY
git diff --check
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: all passed.

The Docker image built as:

```text
repository-analysis-service:slice06
sha256:e7b8f08b08d835d30edf30d380580857112d66b519d7777ec24d9af6492985f8
```

Post-review Slice 06 blockers were resolved before completion:

- clean HTTPS repository URL validation now rejects localhost, local/private
  IPv4, IPv6 loopback, IPv6 unique-local and IPv6 link-local hosts in both the
  service model and Gateway schema;
- gRPC error descriptions no longer expose raw Git output or service workspace
  paths;
- workspace IDs are UUID-based opaque IDs and do not embed analysis run IDs or
  filesystem paths;
- Git checkout enforces the configured workspace byte quota after checkout and
  cleans prepared workspaces when checkout fails;
- the production Git command runner has tests for safety options, sanitized
  environment, timeout behavior and bounded output;
- repository-analysis adapter-out tests are explicitly unignored by Git.
- the full repository quality gate, including package-level coverage
  verification, passed after targeted coverage fixes.

## Slice 07 Refinement Status

Slice 07 read-only pre-implementation review found that implementation could
continue only after the provisional Java AST contract, Gradle service
registration and scope were made explicit.

Refinements recorded before and during service code:

- `contracts/grpc/java-ast-analysis.proto` defines the provisional
  JavaParser AST worker contract.
- The contract imports only `analysis-job.proto` and uses service-local
  generated DTOs behind the gRPC adapter boundary.
- The service receives source snapshots as explicit inline Java source files,
  emits deterministic static source facts and writes a service-owned artifact
  reference instead of exposing local filesystem paths.
- Runtime execution, symbol-resolution truth, graph edges, BTM output and LLM
  findings remain out of Slice 07 scope.
- Missing JavaParser symbol solving is represented as the explicit
  `SYMBOL_RESOLUTION_NOT_CONFIGURED` diagnostic when requested.
- `max_source_bytes` is enforced from actual UTF-8 content bytes, not
  caller-reported metadata.
- `timeout_seconds` bounds the scanner response path and maps to gRPC
  `DEADLINE_EXCEEDED` on timeout.
- JavaParser parse diagnostics preserve parser-provided positions when
  available and use `0/0` only when the parser exposes no location.
- The root Gradle build now orders each subproject's non-clean tasks after its
  local `clean` task, which stabilizes the documented combined full gate on
  WSL-mounted worktrees with generated protobuf outputs.

## Slice 07 Verification

Executed for `java-ast-analysis-service`:

```bash
./gradlew --no-daemon :services:java-ast-analysis-service:test --tests "de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser.JavaParserSourceScannerAdapterTest" --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:java-ast-analysis-service:test :services:java-ast-analysis-service:jacocoTestReport :services:java-ast-analysis-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:java-ast-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/java-ast-analysis-service/Dockerfile -t java-ast-analysis-service:slice07 .
```

Result: all passed after targeted fixes.

The Docker image built as:

```text
java-ast-analysis-service:slice07
sha256:ade9b0e20b51eab3ef0460a5650962a4a8af2cc5b421e930af062bcea2fd2ebf
```

Intermediate Slice 07 blockers were resolved before completion:

- the initial `max_source_bytes` check trusted caller-reported `size_bytes`;
  it now computes UTF-8 bytes from actual `content_utf8`;
- the initial `timeout_seconds` field was validation-only; scanner execution is
  now bounded and maps timeouts to `DEADLINE_EXCEEDED`;
- parse diagnostics initially lacked regression coverage for parser positions;
  tests now pin parser-provided source path, line and column behavior plus
  explicit `0/0` unknown fallback;
- generated protobuf source scoping was adjusted so each service compiles only
  the contracts it owns or imports;
- service-local and full repository coverage gaps were closed before the final
  quality gate;
- a WSL-mounted build directory contention issue during the full gate was
  resolved by stopping Gradle daemons, deleting only the verified generated
  `services/java-ast-analysis-service/build` directory, and adding local
  clean-task ordering before the final successful gate.

Post-review approvals were received from source-analysis, gRPC/protobuf,
tester, DevOps and microservice reviewers. Residual Slice 07 risks are accepted
as follow-up scope: timeout cancellation is cooperative after the bounded
response path, artifact JSON remains Gson-shaped rather than a formal artifact
schema, idempotency is required by contract but not yet backed by a durable
deduplication store, and no Swarm/Kubernetes runtime readiness is claimed.

## Slice 08 Refinement Status

Slice 08 read-only pre-implementation review found that implementation could
continue only after the provisional Joern contract and repo-level write scope
were made explicit.

Refinements recorded before and during service code:

- `contracts/grpc/joern-cpg-analysis.proto` defines the provisional Joern
  CPG/CFG/DFG worker contract.
- The service receives opaque workspace IDs, source snapshot IDs, relative Java
  roots and bounded Joern policy data; public contracts do not expose absolute
  workspace paths or `file:` URIs.
- Joern execution is service-contained and optional for the default unit test
  suite; tests use deterministic fake executables instead of Docker or a local
  Joern installation.
- Generated protobuf code remains service-local, and existing services exclude
  the Joern proto from their own generation scope.
- CPG/CFG/DFG outputs are modeled as static semantic artifacts with provenance,
  schema version, Joern version/image reference, query bundle version,
  artifact checksums, completeness and diagnostics.
- Missing query scripts and missing artifacts remain explicit diagnostics and
  never become fabricated semantic evidence.

## Slice 08 Verification

Executed for `joern-cpg-analysis-service`:

```bash
./gradlew --no-daemon :services:joern-cpg-analysis-service:compileTestJava --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:joern-cpg-analysis-service:test :services:joern-cpg-analysis-service:jacocoTestReport :services:joern-cpg-analysis-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:joern-cpg-analysis-service:test :services:joern-cpg-analysis-service:jacocoTestReport :services:joern-cpg-analysis-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:joern-cpg-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/joern-cpg-analysis-service/Dockerfile -t joern-cpg-analysis-service:slice08 .
```

Result: all passed after targeted coverage fixes.

The Docker image built as:

```text
joern-cpg-analysis-service:slice08
sha256:ad3af3eb7811543f26e6f7a84818e04f13c92d6aa03bf497882d160475d80b91
```

Intermediate Slice 08 blockers were resolved before completion:

- the initial service scope did not explicitly allow the Joern contract,
  Gradle service registration, `.dockerignore` or generated protobuf coverage
  exclusion; the workflow slice scope now records those write boundaries;
- the provenance artifact initially recorded incomplete state in JSON while
  its artifact reference stayed `COMPLETE`; the collector now gives the
  provenance artifact the same completeness as the collected semantic output;
- Joern runtime image provenance initially used the caller's policy value; it
  now uses the service-owned runtime image reference and rejects policy/runtime
  mismatches before Joern invocation;
- an unavailable Joern version initially produced `UNKNOWN` without a
  diagnostic; it now emits `JOERN_VERSION_UNAVAILABLE` and affects
  completeness;
- artifact byte quota enforcement initially excluded `joern-provenance.json`;
  it now includes the generated provenance artifact;
- package-level coverage initially failed for new Joern service packages; tests
  now cover domain validation, gRPC mapping, filesystem collection, process
  timeout/unavailable/query-failure behavior, bootstrap health and architecture
  rules before the final successful full gate.

Post-review Slice 08 risks accepted as follow-up scope: the query bundle
contents are still a later packaging concern, durable artifact indexing and
Analysis Store registration are not implemented yet, and no Swarm/Kubernetes
runtime readiness is claimed for the Joern worker in this slice.

## Slice 09 Refinement Status

Slice 09 read-only pre-implementation review approved a provisional BTM
generation contract because final canonical fact schemas are not yet fixed and
the user explicitly allowed plausible contract details when service
communication remains logical.

Refinements recorded before and during service code:

- `contracts/grpc/btm-generation.proto` defines the provisional
  `BtmGenerationService.GenerateBtmRules` contract.
- `analysis-job.proto` was left stable; the new contract imports worker IDs,
  analysis/job/source snapshot IDs, artifact references and completeness.
- The service accepts accepted fact artifact references, semantic artifact
  references and bounded inline instrumentation targets only. It does not
  accept repository URLs, workspace paths, absolute paths, `file:` URIs or
  source content as generation authority.
- Rule IDs are deterministic for source snapshot ID, target ID, probe kind and
  rule schema version.
- Generated `.btm` output and the rule manifest contain no timestamps, local
  paths or observed runtime claims.
- Generated protobuf code remains service-local. Existing service builds now
  exclude `btm-generation.proto` unless they own or intentionally import it.

## Slice 09 Verification

Executed for `btm-generation-service`:

```bash
./gradlew --no-daemon :services:btm-generation-service:compileJava --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:btm-generation-service:test :services:btm-generation-service:jacocoTestReport :services:btm-generation-service:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:forensic-ingestion-service:compileJava --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:btm-generation-service:test :services:btm-generation-service:jacocoTestReport :services:btm-generation-service:jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew --no-daemon :services:btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/btm-generation-service/Dockerfile --build-arg SERVICE_JAR=services/btm-generation-service/build/libs/btm-generation-service-0.1.0-SNAPSHOT.jar -t btm-generation-service:slice09 .
```

Result: all final commands passed after targeted fixes.

The Docker image built as:

```text
btm-generation-service:slice09
sha256:cc617c5e4e62c341519549755dfa9e3d25c146b6484af991355f1c5811732b41
```

Intermediate Slice 09 blockers were resolved before completion:

- the initial workflow write scope did not explicitly list the BTM contract,
  service registration, `.dockerignore` entry or generated protobuf coverage
  exclusion; the Slice 09 scope now records those boundaries;
- adding `btm-generation.proto` initially caused an unrelated service to
  generate BTM transport classes without `analysis-job.proto`; existing
  service protobuf scopes now exclude the BTM proto unless intentionally used;
- package-level branch coverage initially failed for the new BTM domain and
  gRPC adapter packages; tests and declarative enum mappings now cover
  validation, deterministic generation, gRPC statuses, artifact writing,
  startup and architecture boundaries before the final successful full gate.
- contract review found that `safe_attributes` could still carry local path
  values and `diagnostic.artifact_path` lacked an explicit relative/opaque
  constraint; both are now validated and covered by regression tests before the
  final successful full gate and Docker build.

Post-review Slice 09 risks accepted as follow-up scope: durable artifact
registration with `analysis-store-service` is not implemented yet, final
canonical fact schemas remain future contract work, and no Docker Compose,
Swarm or Kubernetes runtime readiness is claimed in this slice.
