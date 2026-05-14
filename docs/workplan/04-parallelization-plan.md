# 04 - Parallelization Plan

## Rules

- Multiple workers or subagents may work in parallel when slice dependencies allow it.
- Shared interfaces are fixed first.
- Agents may review each other.
- The orchestrator coordinates dependencies and file ownership.
- Conflicts are documented instead of resolved silently.
- Parallel workers must not edit the same files at the same time.

## Parallel Group A

- Senior gRPC/Proto Specialist works on Slice 1.
- Senior System Architect reviews architecture boundaries.
- Senior Documentation Engineer prepares the README and contract documentation structure.
- Senior Tester drafts contract-test expectations.

Exit condition: the request/response contract and adapter boundary are stable enough for implementation.

## Parallel Group B

- Senior Git/Workspace Specialist works on the Git port and workspace lifecycle model.
- Senior Java Backend Developer works on workspace domain and application services.
- Senior Tester creates mini repository scenarios.
- Senior Security/Sandbox Engineer reviews safe Git and cleanup boundaries.

Exit condition: workspace and checkout contracts are ready for the mini end-to-end test.

## Parallel Group C

- Senior Plugin Integration Developer binds the gRPC client.
- Senior gRPC/Proto Specialist checks contract compatibility.
- Senior Tester builds the end-to-end test.
- Senior Analysis Storage Architect reviews session registration and persistence expectations.

Exit condition: the mini test proves request, session, workspace, checkout and cleanup.

## Parallel Group D

- Senior Performance Engineer plans WildFly metrics.
- Senior Security/Sandbox Engineer checks untrusted repository and filesystem risks.
- Senior DevOps Engineer checks environment and command constraints.
- Senior Git/Workspace Specialist prepares large repository checkout policy.

Exit condition: WildFly hardening can run without parser, Joern, BTM, graph, replay or UI execution.

## Conflict Handling

The orchestrator records:

- conflicting file ownership,
- contract changes after dependent work starts,
- unverified repository symbols,
- quality-gate blockers,
- producer/consumer boundary drift,
- any attempt to move analysis back into the plugin.
