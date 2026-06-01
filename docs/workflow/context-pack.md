# Workflow Context Pack

## Workflow

- Name: Repository Workspace Branch Selection And Refresh
- Version: `2026-06-01`
- Branch: `feature/workflow-workspace-branch-selection-20260601`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`

## Affected Areas

- `repository-source-service`
- `forensic-ui`
- `contracts/grpc/repository-analysis.proto`
- `contracts/openapi/gateway-api.yaml`
- repository workspace metadata and branch refresh semantics

## Forbidden Areas

- Runtime replay, graph, LLM and report evidence semantics.
- Direct UI database or Git access.
- Deleting analysis results without verified owner contracts.
- Docker volume deletion outside explicit final-delete requirements.

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester
- Contract-First API Steward
- Data Ownership & Persistence Steward

## Required Quality Commands

- `./gradlew test --dependency-verification strict --console=plain --stacktrace`
- `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`
- `npm test -- --run src/pages/workspaces/CreateWorkspacePage.test.tsx src/pages/workspaces/WorkspaceListPage.test.tsx src/adapters/api/mappers.test.ts`
- `npm run build`

## Source Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `a1bd05f38d8602a5c67dcfbf3628c811a4c0de33004fdf553afadf4b5c614f5d` |
| `QUALITY.md` | `95f9aa2ba5dd7f67057864d52321e5716acb46b166bc74c12b154ec271bb3596` |
| `.agents/orchestrator/routing-rules.md` | `64d7b815bb9f5d1da72b42822a3e532fa79d08c8391cf2fbb7da3de390a8d740` |
| `.agents/orchestrator/swarm-orchestrator.md` | `860d2ad867b08838d8155ffaa580bec50f708b40eacef91920e65e76040eded8` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md` | `bd155271d13945677d998468a7c41df4f98e71ebb7e9913d2c7d08f931f4b472` |
| `docs/arc42/05-building-block-view.md` | `4a61879a69c7979c871b5e0b6b97ff4de4ceeef11adde77f8c9bf3dad7795e4d` |
| `contracts/grpc/repository-analysis.proto` | `8f34e24f8a1c0c5c24dcda882f72ce225c8218b123b9629f35bbf99a2c7d8727` |
| `contracts/openapi/gateway-api.yaml` | `eeccf2c8ab7085f9144d8908b92d90cc9f20917df7d80121f1102d5f14ee1967` |
