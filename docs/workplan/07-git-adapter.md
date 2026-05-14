# Git Port And Adapter

The Git integration is an outbound adapter concern. Application services use a port; the concrete adapter performs Git operations in the server-side workspace.

## Required Port Operations

```text
cloneRepository
fetch
checkoutBranch
checkoutCommit
resolveCurrentCommit
detectRemoteUrl
cleanupRepository
```

Each operation returns explicit success, failure or diagnostic information. Exceptions may be used for fail-fast adapter internals, but application results must preserve meaningful checkout diagnostics.

## Operation Responsibilities

### cloneRepository

Creates the local repository working copy inside a prepared workspace path. It must respect timeout and workspace policy.

### fetch

Updates remote references when a branch or commit cannot be resolved from the initial clone.

### checkoutBranch

Checks out the requested branch when present. If the branch is marked required and cannot be resolved, the operation fails explicitly.

### checkoutCommit

Checks out the requested commit when present. If the commit is marked required and cannot be resolved, the operation fails explicitly.

### resolveCurrentCommit

Returns the effective commit after checkout. This value is part of the evidence trail for later parser execution.

### detectRemoteUrl

Returns the effective remote URL from the local Git metadata so the response can report what was actually checked out.

### cleanupRepository

Deletes or resets the working copy according to workspace cleanup policy and reports cleanup failures explicitly.

## Timeout Behavior

Every external Git operation must have a configured timeout. Timeout diagnostics must include the operation name and the policy limit. A timeout must not be reported as a successful checkout.

## Error Handling

The adapter must distinguish:

- invalid repository URL
- clone failure
- fetch failure
- branch not found
- commit not found
- checkout conflict
- timeout
- workspace path or permission failure
- cleanup failure

The result must not hide the original error category behind a generic analysis failure.

## Large Repository Handling

Large repositories may require:

- optional shallow clone
- optional partial clone
- optional sparse checkout in a later dedicated step
- disk-size guardrails
- file-count measurement
- clear timeout defaults

These optimizations are allowed only when requested by `WorkspacePolicy` and supported by the verified adapter. The adapter must report when a requested optimization is unavailable.

## WildFly Hardening Target

WildFly is the large-repository hardening target:

```text
https://github.com/wildfly/wildfly.git
```

The WildFly scenario validates clone, checkout, resolved commit detection, source-root detection and cleanup under realistic load. It does not execute parsers.

## Non-Scope

The Git adapter must not:

- run JavaParser
- run Joern
- generate BTM files
- infer runtime execution
- write analysis findings
- persist sessions directly
