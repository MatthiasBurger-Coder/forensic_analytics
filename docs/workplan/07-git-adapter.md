# 07 - Git Adapter

## Purpose

The Git adapter is an outbound adapter. It performs repository operations inside a prepared workspace and returns deterministic checkout metadata to the application.

## Required Operations

- `cloneRepository`
- `fetch`
- `checkoutBranch`
- `checkoutCommit`
- `resolveCurrentCommit`
- `detectRemoteUrl`
- `cleanupRepository`

Names are target operation names for planning. Existing symbols must be verified before implementation.

## Timeout Behavior

Each operation must accept a timeout or execution policy. Timeout failures are reported as explicit checkout diagnostics and must not be converted into successful partial checkout.

## Error Handling

Errors should distinguish:

- invalid repository URL,
- unsupported protocol,
- authentication failure,
- branch not found,
- commit not found,
- checkout conflict,
- repository corruption,
- timeout,
- disk pressure,
- cleanup failure.

Original command failures should be preserved without leaking secrets.

## Large Repositories

Large repository behavior is policy-driven:

- shallow clone is optional,
- partial clone is optional,
- sparse checkout is optional,
- repository mirrors are optional,
- detached head workflows are allowed when commit pinning is explicit.

Optimizations must not make resolved commit identity unverifiable.

## WildFly Hardening

WildFly is the large-repository hardening case:

```text
https://github.com/wildfly/wildfly.git
```

The first WildFly scenario verifies only:

```text
clone -> checkout -> resolve commit -> detect source roots -> cleanup
```

No parser execution, Joern execution or BTM generation is allowed in this test.
