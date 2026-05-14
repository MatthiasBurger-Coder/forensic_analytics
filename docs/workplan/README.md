# Workspace and gRPC Workplan

## Goal

This workplan defines the next Forensic Analytics platform phase:

```text
Plugin
  -> gRPC request
    -> forensic_analytics ingestion
      -> create workspace
        -> clone or checkout repository
          -> register analysis session
            -> later parser and analyzer execution
```

The immediate goal is to make Analytics able to receive a repository-analysis request, prepare an isolated server-side workspace, check out the requested repository revision, register an analysis session and return a deterministic session and checkout result.

## Why Workspace and gRPC Come Before Parsers

Parser, Joern, AST, BTM, replay and graph work all depend on a stable source input. The platform must first prove that it can receive repository context from the plugin, create a workspace, clone or checkout the exact requested revision, register the analysis session and clean up safely.

Without this foundation, parser results would depend on uncontrolled local paths, mutable branch state or plugin-side analysis behavior. The workspace/gRPC phase creates the server-side input boundary that later analyzers can trust.

## Affected Modules

Planned slices affect or may add contracts around these verified modules:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-persistence`
- `forensic-analytics-bootstrap`
- future Git and filesystem workspace outbound adapters
- plugin-side client integration in the producer repository

No parser, Joern, BTM, replay, graph or UI implementation is part of this workplan.

## How To Execute

1. Fix shared Protobuf and application contracts first.
2. Model workspace and repository checkout domain concepts before adapters.
3. Implement Git and filesystem workspace adapters behind ports.
4. Connect the plugin gRPC client to the new request/response contract.
5. Verify with a mini repository before any large repository hardening.
6. Use WildFly only as a Git/workspace hardening scenario.
7. Run targeted tests first, then the applicable quality gate from `QUALITY.md`.

## Participating Subagents

The planned subagents are:

- Senior System Architect
- Senior Java Backend Developer
- Senior DevOps Engineer
- Senior Tester
- Senior gRPC/Proto Specialist
- Senior Git/Workspace Specialist
- Senior Plugin Integration Developer
- Senior Documentation Engineer
- Senior Agent Swarm Orchestrator
- Senior Security/Sandbox Engineer
- Senior Performance Engineer
- Senior Analysis Storage Architect
- Senior Joern/CPG Specialist

The orchestrator coordinates dependencies and file ownership. Multiple workers may run in parallel only when shared contracts are fixed and write scopes do not overlap.
