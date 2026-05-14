# Workspace and gRPC Integration Workplan

This workplan defines the next platform step for Forensic Analytics:

```text
Plugin
  -> gRPC request
    -> forensic_analytics ingestion
      -> workspace preparation
        -> repository clone or checkout
          -> analysis job registration
            -> later parser or analyzer execution
```

The plan deliberately prepares workspace, checkout, ingestion and session boundaries before parser work. A parser can only produce trustworthy forensic evidence when the platform can already prove which repository, branch, commit, workspace path, source roots and analysis session were used. This workplan therefore treats repository acquisition and session registration as the next evidence-preserving foundation.

Parser implementation, Joern execution, BTM generation, replay, LLM diagnosis and report generation are outside this workplan. The plugin remains a producer and gRPC client. `forensic_analytics` remains the consumer and analysis platform.

## Verified Repository Baseline

The repository currently contains these relevant modules:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-cli`
- `forensic-analytics-testbed`
- `forensic-analytics-persistence`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-bootstrap`

The future implementation slices in this directory must verify existing contracts before changing them. The current gRPC module already contains `forensic_ingestion.proto` with an `AnalyzeRepository` RPC and the planned request and response model names. The implementation work must preserve compatibility intentionally and must not silently replace verified names.

## How To Use This Workplan

Work through the files in order:

1. Start with [00-overview.md](00-overview.md) and [01-architecture-target.md](01-architecture-target.md).
2. Execute the implementation slices from [02-slices.md](02-slices.md) in dependency order.
3. Use [03-subagents.md](03-subagents.md) and [04-parallelization-plan.md](04-parallelization-plan.md) to coordinate parallel work and reviews.
4. Keep the contract, workspace, Git, plugin and test plans aligned through [05-grpc-contract.md](05-grpc-contract.md) to [10-wildfly-hardening-test.md](10-wildfly-hardening-test.md).
5. Run the quality gates described in [11-quality-gates.md](11-quality-gates.md).
6. Finish with the Git workflow in [12-commit-and-push-plan.md](12-commit-and-push-plan.md).

Every slice must keep the domain free of framework dependencies, keep application services behind ports, and preserve uncertainty explicitly. WildFly is a later hardening target, not the first functional test.
