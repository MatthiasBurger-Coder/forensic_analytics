# Integration Tests

Integration coverage must prove the repository-ingestion path before any parser work begins.

## Test Level 1: Mini Test Repository

Purpose: first functional proof of the complete request-to-checkout path.

The mini repository should be local and deterministic. It can contain a tiny Java project with a single source root and a known commit. It must not require external network access.

Required checks:

```text
Plugin sends request
Analytics receives request
AnalysisSession is created
Workspace is created
Repository is cloned
Branch or commit is checked out
Commit ID is returned
Workspace can be cleaned up
```

Additional checks:

- request ID is preserved
- schema version is preserved
- checkout diagnostics are deterministic
- no parser result is created
- cleanup removes the workspace when policy requires it

## Test Level 2: Medium Multi-Module Test Repository

Purpose: validate module and source-root handling beyond a single project.

Checks:

- clone and checkout a repository with multiple modules
- detect more than one source root deterministically
- preserve declared build modules from `BuildContext`
- keep checkout result independent from parser execution
- handle no-source-root modules with explicit diagnostics

This stage may use a local fixture or a controlled internal repository. External network use should remain opt-in.

## Test Level 3: WildFly Repository

Purpose: harden Git and workspace behavior under realistic size and file-count pressure.

Repository:

```text
https://github.com/wildfly/wildfly.git
```

Checks:

- clone completes or times out with explicit diagnostics
- checkout resolves the requested branch or commit
- resolved commit is returned
- workspace size is measured
- file count is measured
- source roots are detected
- cleanup is verified

WildFly tests are opt-in and must not run in the default unit suite.

## Test Isolation Rules

- Unit tests use temporary directories.
- Network tests are opt-in.
- Generated repositories stay under test temporary directories.
- No test writes to repository source files.
- Synthetic fixtures are clearly named as fixtures.
- Tests do not fabricate runtime traces or parser evidence.
