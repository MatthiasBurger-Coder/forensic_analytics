# Implementation Slices

Each slice starts with read-only verification of the exact existing files, classes, proto messages, Gradle tasks and tests it intends to touch. If an expected artifact is missing or named differently, the slice stops and reports the mismatch instead of guessing.

## Slice 1: Proto Contract Define Or Verify

Goal: Verify and, if needed, refine the repository-ingestion gRPC contract.

Affected modules: `forensic-analytics-ingestion-grpc`, `forensic-analytics-application`, tests for DTO mapping.

Subagents: Senior gRPC/Proto Specialist, Senior System Architect, Senior Tester.

Inputs: existing `forensic_ingestion.proto`, current mapper tests, user-visible request requirements.

Expected result: `AnalyzeRepositoryRequest`, `RepositoryReference`, `BranchReference`, `CommitReference`, `WorkspacePolicy`, `BuildContext`, `AnalyzeRepositoryResponse`, `AnalysisSessionId`, `WorkspaceId` and `CheckoutResult` are verified or updated with explicit compatibility decisions.

Tests: proto generation, mapper tests, request validator tests.

Acceptance criteria: the contract carries repository, branch, commit, workspace policy and build context; the response carries session ID, workspace ID and checkout result; unknown or missing values remain explicit.

Risks: accidental field renumbering, hidden compatibility behavior, treating transport DTOs as domain models.

Dependencies: none.

## Slice 2: Analytics gRPC Server Prepare

Goal: Prepare the inbound server path for repository analysis requests.

Affected modules: `forensic-analytics-ingestion-grpc`, `forensic-analytics-bootstrap`, `forensic-analytics-application`.

Subagents: Senior Java Backend Developer, Senior gRPC/Proto Specialist, Senior Tester.

Inputs: verified proto contract, existing gRPC service implementation, bootstrap wiring.

Expected result: the gRPC adapter receives `AnalyzeRepository`, validates request fields and delegates to an application use case without performing checkout work itself.

Tests: gRPC service tests, validator tests, architecture tests for adapter boundaries.

Acceptance criteria: invalid requests are rejected with explicit diagnostics; valid requests reach the application boundary; no Git, filesystem or parser logic enters the gRPC adapter.

Risks: leaking Protobuf classes into application/domain, doing execution work in the adapter, vague error mapping.

Dependencies: Slice 1.

## Slice 3: Workspace Domain Model

Goal: Model workspace preparation concepts needed for server-side checkout.

Affected modules: `forensic-analytics-domain`, `forensic-analytics-application`.

Subagents: Senior Java Backend Developer, Senior Git/Workspace Specialist, Senior System Architect.

Inputs: existing workspace domain classes, repository checkout domain classes, AGENTS architecture rules.

Expected result: domain concepts represent workspace ID, path, policy, lease, cleanup policy, status and prepared workspace state without framework dependencies.

Tests: domain model tests for validation, immutability and lifecycle consistency.

Acceptance criteria: status values include `REQUESTED`, `CREATING`, `READY`, `FAILED` and `CLEANED` where that lifecycle is required; workspace path and cleanup data are explicit.

Risks: colliding with existing collaboration workspace concepts, overloading one `Workspace` type with two meanings, hidden filesystem assumptions.

Dependencies: none, but must stay compatible with Slice 5.

## Slice 4: Git Port And Git Adapter

Goal: Define the application port and outbound adapter for repository checkout.

Affected modules: `forensic-analytics-application`, `forensic-analytics-adapter-repository-source`, `forensic-analytics-domain`.

Subagents: Senior Git/Workspace Specialist, Senior Java Backend Developer, Senior Tester.

Inputs: existing repository-source adapter, existing Git-related tests, planned checkout operations.

Expected result: a verified port supports `cloneRepository`, `fetch`, `checkoutBranch`, `checkoutCommit`, `resolveCurrentCommit`, `detectRemoteUrl` and `cleanupRepository`.

Tests: adapter tests with temporary repositories, timeout/failure tests, deterministic checkout result tests.

Acceptance criteria: branch and commit checkout are explicit; checkout failures include diagnostics; no parser execution is added.

Risks: process timeout leaks, platform-specific path behavior, large repository performance, network-dependent unit tests.

Dependencies: Slice 3 for domain result objects.

## Slice 5: WorkspacePreparationService

Goal: Implement the application orchestration for workspace allocation and checkout preparation.

Affected modules: `forensic-analytics-application`, `forensic-analytics-domain`, repository-source adapter tests.

Subagents: Senior Java Backend Developer, Senior Git/Workspace Specialist, Senior System Architect.

Inputs: workspace domain, Git port, filesystem workspace port, cleanup policy.

Expected result: the service creates a workspace, applies policy, delegates clone/checkout to ports and returns a prepared workspace with checkout metadata.

Tests: application service tests with fake ports, failure cleanup tests, deterministic diagnostics tests.

Acceptance criteria: no concrete adapter is referenced by the application service; failures preserve causes and workspace state; cleanup policy is respected.

Risks: partial workspaces after failure, hidden fallback behavior, nondeterministic IDs in tests.

Dependencies: Slices 3 and 4.

## Slice 6: Plugin gRPC Client Bind

Goal: Bind the producer-side plugin client to the repository-analysis gRPC request.

Affected modules: plugin repository or producer module, `forensic-analytics-ingestion-request` only if it contains shared request manifest support.

Subagents: Senior Plugin Integration Developer, Senior gRPC/Proto Specialist, Senior Tester.

Inputs: verified proto contract, plugin repository/build context, current plugin task behavior.

Expected result: plugin code constructs and sends the gRPC request, handles response IDs and checkout result, and reports errors clearly.

Tests: client mapping tests, fake gRPC server tests, error-path tests.

Acceptance criteria: plugin performs no AST analysis, Joern execution or BTM generation; plugin does not become the analysis platform.

Risks: duplicated server logic in the plugin, missing commit context, leaking secrets in request attributes.

Dependencies: Slices 1 and 2.

## Slice 7: Mini End-To-End Test

Goal: Prove the minimal request-to-checkout path with a tiny repository.

Affected modules: `forensic-analytics-testbed`, `forensic-analytics-bootstrap`, `forensic-analytics-ingestion-grpc`, repository-source adapter.

Subagents: Senior Tester, Senior Java Backend Developer, Senior gRPC/Proto Specialist.

Inputs: local mini test repository fixture, bootstrap server configuration, fake or local plugin client.

Expected result: a deterministic test proves request receipt, session creation, workspace creation, clone, checkout, commit resolution and cleanup.

Tests: mini end-to-end test running against local fixtures only.

Acceptance criteria: test does not require external network access; resolved commit matches the fixture; workspace cleanup is verified.

Risks: flaky ports, temporary directory leakage, fixture accidentally becoming a fake analysis trace.

Dependencies: Slices 1 through 6.

## Slice 8: Analysis Session And Job Registration

Goal: Persist or register the analysis session and job/workspace context.

Affected modules: `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-persistence`, gRPC mapping tests.

Subagents: Senior Java Backend Developer, Senior System Architect, Senior Tester.

Inputs: prepared workspace result, checkout result, persistence adapter boundary.

Expected result: an `AnalysisSession` is created with a job entry that points to workspace and checkout evidence.

Tests: application tests, persistence adapter tests, deterministic ID and status tests.

Acceptance criteria: session status does not imply parser execution; job registration records checkout readiness only; generated analysis output remains absent.

Risks: presenting checkout completion as completed analysis, persistence leaking into domain, missing provenance.

Dependencies: Slices 3, 5 and 7.

## Slice 9: SourceRoot Detection

Goal: Detect source roots as repository structure metadata after checkout.

Affected modules: `forensic-analytics-adapter-repository-source`, `forensic-analytics-domain`, application result mapping.

Subagents: Senior Git/Workspace Specialist, Senior Java Backend Developer, Senior Tester.

Inputs: checked-out workspace, known Java source root conventions, existing source-root detector tests.

Expected result: source roots are detected deterministically and attached to `CheckoutResult`.

Tests: single-module, multi-module and no-source-root fixtures.

Acceptance criteria: source roots are sorted deterministically; unresolved or absent roots are explicit; no parser is run.

Risks: confusing source roots with parsed source facts, ignoring generated or unusual layouts without diagnostics.

Dependencies: Slice 4 and Slice 5.

## Slice 10: WildFly Hardening Prepare

Goal: Prepare a controlled hardening scenario for the WildFly repository.

Affected modules: `forensic-analytics-testbed`, documentation, optional integration-test configuration.

Subagents: Senior DevOps Engineer, Senior Git/Workspace Specialist, Senior Tester.

Inputs: `https://github.com/wildfly/wildfly.git`, timeout policy, workspace size limits, network isolation rules.

Expected result: a disabled-by-default or explicitly tagged hardening scenario that can clone, checkout, measure and clean up WildFly.

Tests: configuration validation test; no default network test in the unit suite.

Acceptance criteria: scenario requires explicit opt-in; timeout and cleanup behavior are documented; no parser execution is included.

Risks: network flakiness, disk pressure, long-running CI jobs, accidental default execution.

Dependencies: Slices 4, 5 and 9.

## Slice 11: WildFly Hardening Execute

Goal: Execute the WildFly hardening scenario under explicit local conditions.

Affected modules: hardening scenario outputs and documentation only, unless defects are found.

Subagents: Senior DevOps Engineer, Senior Git/Workspace Specialist, Senior Tester, Senior Documentation Engineer.

Inputs: opt-in WildFly scenario, configured timeout, available disk space.

Expected result: measured clone time, checkout time, workspace size, file count, source-root count, resolved commit and cleanup result.

Tests: opt-in hardening run, not part of the default unit suite.

Acceptance criteria: clone, checkout, resolve commit, detect source roots and cleanup complete or fail with explicit diagnostics.

Risks: external service rate limits, local disk exhaustion, treating hardening metrics as functional proof of analysis.

Dependencies: Slice 10.

## Slice 12: Cleanup, Documentation, Quality Gate, Commit, Push

Goal: Close the implementation with documentation, verification and Git publication.

Affected modules: all modules changed by earlier slices, `docs/`, quality reports if explicitly required.

Subagents: Senior Documentation Engineer, Senior Tester, Senior Agent Swarm Orchestrator, Senior DevOps Engineer.

Inputs: final diffs, test evidence, quality-gate evidence, architecture review findings.

Expected result: documentation is aligned, quality gates pass or blockers are documented, commit is created and branch is pushed.

Tests: run the gates in [11-quality-gates.md](11-quality-gates.md).

Acceptance criteria: no unrelated files are committed; no generated build output is committed; branch is not `main`; push does not force-push.

Risks: line-ending-only diffs on WSL, stale documentation, skipped verification without explanation.

Dependencies: all prior slices.
