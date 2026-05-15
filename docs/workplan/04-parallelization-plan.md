# Parallelization Plan

Parallel work is allowed when slice dependencies permit it. Workers may review each other, but the orchestrator owns dependency sequencing and final conflict resolution.

## Coordination Rules

- Every worker begins with read-only verification of the files it will touch.
- Each worker owns a disjoint write area wherever possible.
- Cross-slice contracts are reviewed before implementation branches converge.
- gRPC DTO changes, domain model changes and application port changes require explicit review before adapter work depends on them.
- No worker introduces parser, Joern, BTM, replay, LLM or report execution in this workplan.

## Parallel Group A: Contract And Architecture Foundation

Can start immediately.

- Senior gRPC/Proto Specialist works on Slice 1.
- Senior System Architect reviews architecture boundaries for Slices 1 through 3.
- Senior Documentation Engineer prepares the README and architecture documentation structure.
- Senior Agent Swarm Orchestrator records dependency gates.

Review gate: proto names, field numbers, DTO mapping boundaries and planned service names are verified before Group B depends on them.

## Parallel Group B: Workspace And Git Foundations

Can start after the architecture boundary review identifies the correct module ownership.

- Senior Java Backend Developer works on Slice 3 workspace domain.
- Senior Git/Workspace Specialist works on Slice 4 Git port and adapter behavior.
- Senior Tester prepares local repository fixtures for Slices 4, 7 and 9.
- Senior System Architect reviews port placement and dependency direction.

Review gate: the workspace domain and Git port agree on `RepositoryReference`, `BranchReference`, `CommitReference`, `SourceRoot` and `CheckoutResult`.

## Parallel Group C: Ingestion And Plugin Client

Can start after Slice 1 is stable and the gRPC server delegation boundary is verified.

- Senior Java Backend Developer works on Slice 2 server delegation.
- Senior Plugin Integration Developer works on Slice 6 client request construction.
- Senior gRPC/Proto Specialist reviews generated DTO compatibility.
- Senior Tester builds fake-server and fake-client tests.

Review gate: plugin client and server use the same contract, and neither side performs analysis logic.

## Parallel Group D: End-To-End And Session Registration

Can start after Slices 2 through 6 provide an executable path.

- Senior Tester implements Slice 7 mini end-to-end coverage.
- Senior Java Backend Developer implements Slice 8 session and job registration.
- Senior Git/Workspace Specialist implements Slice 9 source-root detection.
- Senior Documentation Engineer updates operational documentation.

Review gate: checkout completion is not documented or stored as parser completion.

## Parallel Group E: WildFly Hardening

Can start only after mini and medium tests pass.

- Senior DevOps Engineer prepares Slice 10 opt-in hardening configuration.
- Senior Git/Workspace Specialist reviews WildFly clone and cleanup constraints.
- Senior Tester executes Slice 11 only under explicit local conditions.
- Senior Documentation Engineer records metrics and known limitations.

Review gate: WildFly hardening remains clone, checkout, resolve commit, detect source roots and cleanup only.

## Final Integration

The Senior Agent Swarm Orchestrator coordinates final readiness:

- collect review findings
- verify no unresolved dependency remains
- run quality gates
- inspect final diff
- stage only in-scope files
- commit and push the work branch
