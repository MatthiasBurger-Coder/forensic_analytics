# Workflow Context Pack

## Active Workflow

- Workflow: Remote Branch Metadata Listing And Persistence
- Branch: `feature/workflow-remote-branches-gui-persistence-20260602`
- Version: `2026-06-02`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`

## Affected Areas

- `repository-source-service` remote metadata resolution and workspace branch persistence
- `query-report-api-service` gateway forwarding and REST serialization
- `forensic-ui` workspace metadata mapper and branch selector
- `contracts/grpc/repository-analysis.proto`
- `contracts/openapi/gateway-api.yaml`
- ADR-0024 governed repository-source persistence

## Forbidden Areas

- Direct UI or query-report database access
- Live GitHub dependency in required tests
- Branch-name path usage
- Runtime evidence inference from branch selection
- New shared Java DTO or implementation modules

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester
- Senior Analysis Storage Architect
- Senior DevOps Engineer
- Senior Documentation Engineer

## Quality Commands

- Minimum: `./gradlew test --dependency-verification strict --console=plain --stacktrace`
- Full: `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`
- Frontend targeted: `cd forensic-ui && npm test -- --run src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx`

## Governing File Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `a1bd05f38d8602a5c67dcfbf3628c811a4c0de33004fdf553afadf4b5c614f5d` |
| `QUALITY.md` | `95f9aa2ba5dd7f67057864d52321e5716acb46b166bc74c12b154ec271bb3596` |
| `.agents/orchestrator/routing-rules.md` | `64d7b815bb9f5d1da72b42822a3e532fa79d08c8391cf2fbb7da3de390a8d740` |
| `.agents/orchestrator/swarm-orchestrator.md` | `860d2ad867b08838d8155ffaa580bec50f708b40eacef91920e65e76040eded8` |
| `docs/arc42/README.md` | `b95949d2ced866e71ecd73f20cb7f305a093964cf2fffd778ed9a26322be01e0` |
| `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md` | `54ff246b4359e1eb92c7e80058db42faa079ff5ffd3db0d71170cfaa3dbb68fe` |
| `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md` | `bd155271d13945677d998468a7c41df4f98e71ebb7e9913d2c7d08f931f4b472` |
| `contracts/openapi/gateway-api.yaml` | `de311319e3af45b87982bae4fdef2a1850ac932fd800f117b78919154fedfac7` |
| `contracts/grpc/repository-analysis.proto` | `8f34e24f8a1c0c5c24dcda882f72ce225c8218b123b9629f35bbf99a2c7d8727` |
| `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryMetadataAdapter.java` | `89a2ac16acbb95d2bbd244abf2d6291196b80b115f303d04bc3ce7568e2b38a3` |
| `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpHandler.java` | `e133df60b2d2b1cb991c93dbca9649f05ee245b3d4c17a29a207c1021f14c552` |
| `forensic-ui/src/domain/workspace.ts` | `29256791bd8626b3c573ea7a9200d4e6e5477d6a8cd948009cae6d2b3f0a22a9` |

## Staleness Rules

This context pack is stale if any recorded hash changes, if the workflow branch changes, or if implementation discovers a missing contract, field, service owner, persistence path or quality command.
