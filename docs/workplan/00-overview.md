# 00 - Overview

## Target Flow

```text
Plugin
  -> gRPC
    -> Analytics Server
      -> Workspace
        -> Git Checkout
          -> Analysis Session
```

The plugin sends repository, branch, commit, build and context data. Analytics receives the request, creates a server-side analysis session, prepares an isolated workspace, checks out the requested repository revision and returns a session ID plus checkout result.

## Current Phase Boundaries

- Parsers come later.
- Joern execution comes later.
- AST analysis comes later.
- BTM generation comes later.
- Replay, graph and UI work come later.
- WildFly is a hardening test, not the first functional test.
- A mini test repository comes first.

## Producer and Consumer Boundary

`forensics_tracing` is the producer, build adapter and plugin. It must not become the analysis platform.

`forensic_analytics` is the consumer and central analysis platform. It owns ingestion, workspace preparation, analysis-session registration and later parser/analyzer execution.

## First Functional Proof

The first proof is intentionally small:

```text
plugin request
  -> analytics gRPC endpoint
  -> analysis session registered
  -> workspace created
  -> repository cloned
  -> branch or commit checked out
  -> resolved commit returned
  -> workspace cleanup verified
```

The WildFly repository is used only after this path works with a mini repository.
