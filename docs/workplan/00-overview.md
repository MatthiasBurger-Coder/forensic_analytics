# Overview

The next platform step establishes a reliable repository-ingestion path:

```text
Plugin -> gRPC -> Analytics Server -> Workspace -> Git Checkout -> Analysis Session
```

The plugin collects repository and build context, sends it to the Analytics server through gRPC and waits for an explicit response. The Analytics server validates the request, creates or registers an analysis session, prepares a server-side workspace, performs the requested Git clone or checkout, resolves the effective commit and records the job/workspace context for later analysis.

## Scope

This workplan covers:

- gRPC contract and server-side ingestion boundary
- server-side workspace creation
- Git checkout and resolved commit detection
- analysis session and job registration
- workspace context and cleanup policy
- source-root detection needed to describe the checked-out repository
- mini, medium and WildFly hardening test stages

## Explicit Non-Scope

This workplan does not implement:

- JavaParser or other parser execution
- Joern or Code Property Graph execution
- BTM generation
- runtime replay
- LLM prompt construction
- report generation
- analysis logic inside the plugin

The parser stage comes later, after the platform can prove which source tree was prepared and which analysis session owns it.

## Test Strategy Order

The first functional test must use a mini test repository. It should verify the full request, workspace, clone, checkout, session and cleanup path without involving a large repository.

A medium multi-module repository comes second. It validates that workspace layout and source-root detection do not assume a single-module project.

WildFly is a hardening test, not the first functional test. It is used only after the mini and medium stages pass. The WildFly stage checks whether the Git and workspace system behaves under realistic repository size and file-count pressure:

```text
clone -> checkout -> resolve commit -> detect source roots -> cleanup
```

No parser is executed during the WildFly hardening stage.

## Evidence Principle

The request and response must preserve explicit evidence boundaries:

- requested repository URL
- requested branch
- requested commit, if provided
- resolved remote URL
- resolved commit
- workspace ID
- analysis session ID
- detected source roots
- checkout diagnostics

Missing branch, commit, source root or cleanup evidence must be represented as missing, unresolved or failed. The platform must not infer execution facts from static repository structure.
