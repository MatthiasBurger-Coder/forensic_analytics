# 09 - Integration Tests

## Test Levels

### Test Level 1: Mini Test Repository

This is the first functional test and must run before WildFly hardening.

It verifies:

- plugin sends request,
- Analytics receives request,
- `AnalysisSession` is created,
- workspace is created,
- repository is cloned,
- branch or commit is checked out,
- commit ID is returned,
- workspace can be cleaned.

### Test Level 2: Medium Multi-Module Repository

This test verifies a repository shape closer to production while still staying small enough for regular local execution.

It verifies:

- multiple source roots,
- nested modules,
- deterministic source-root detection,
- checkout with branch and commit references,
- cleanup under moderate file count.

### Test Level 3: WildFly Repository

This is a hardening test only. It is not the first functional proof and should not be required for the default quality gate.

It verifies:

- large repository clone/checkout behavior,
- timeout reporting,
- workspace size measurement,
- file count measurement,
- source-root detection as metadata,
- cleanup behavior.

## Test Rules

- Use temporary directories for filesystem tests.
- Do not run repository build scripts.
- Do not execute parsers, Joern, BTM, replay, graph or UI code.
- Keep synthetic fixtures clearly named as fixtures.
- Record skipped external or network-dependent tests explicitly.
