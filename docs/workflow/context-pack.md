# Workflow Context Pack

## Identity

- Workflow: Move Repository Workspace Metadata to PostgreSQL
- Version: 2026-05-29
- Branch: `feature/workflow-workspace-postgres-20260529`
- Process strand: `workflow execute`
- Execution profile: `FULL_PATH`
- Last S3_DOC refresh: 2026-05-31 after S05 checkpoint verification and
  `main` merge governance hash refresh.

## Affected Areas

- `repository-source-service`
- Repository-source persistence ports and outbound adapters
- Repository-source Spring bootstrap wiring
- Liquibase changelog resources
- Local Docker Compose runtime with `forensic-postgres`
- ADR, arc42 and architecture ownership documentation

## Forbidden Areas

- Platform workspace membership, project, asset, audit or retention features
- Public REST/gRPC contract changes without contract governance
- Cross-service database access
- Checkout byte storage in PostgreSQL
- Graph, vector, replay, report or LLM persistence decisions
- Hidden H2 compatibility fallback

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

## Conditional Roles

- Data Ownership And Persistence Steward
- Senior Analysis Storage Architect
- Senior DevOps Engineer
- Security And Threat Modeling
- Observability And Runtime Diagnostics
- ADR Steward
- Quality Gate Orchestrator

## Required Quality Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Docker Compose model checks are required for slices that change Compose files.

## Governing File Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `a1bd05f38d8602a5c67dcfbf3628c811a4c0de33004fdf553afadf4b5c614f5d` |
| `QUALITY.md` | `95f9aa2ba5dd7f67057864d52321e5716acb46b166bc74c12b154ec271bb3596` |
| `.agents/orchestrator/routing-rules.md` | `64d7b815bb9f5d1da72b42822a3e532fa79d08c8391cf2fbb7da3de390a8d740` |
| `.agents/orchestrator/swarm-orchestrator.md` | `860d2ad867b08838d8155ffaa580bec50f708b40eacef91920e65e76040eded8` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `.agents/skills/execution-profile-router/SKILL.md` | `dfd2fc367bb9ab856e9482d3088690795ee1805b7c84ad462281b2350ca2f62b` |
| `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md` | `54ff246b4359e1eb92c7e80058db42faa079ff5ffd3db0d71170cfaa3dbb68fe` |
| `docs/adr/ADR-0013-data-ownership-per-service.md` | `4114bd8f39a60539bba18bcc32de481aff5a91cec34d556ca1149747b04879ab` |
| `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md` | `57055ae1b371d5c229dd8d64450a23869dc89849808fa68bdff45901a5123a69` |
| `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md` | `bd155271d13945677d998468a7c41df4f98e71ebb7e9913d2c7d08f931f4b472` |
| `docs/adr/README.md` | `92fb221b584b6128e3fa0fa272213c850041c975747dc29d9c2a2151c7802575` |
| `docs/arc42/05-building-block-view.md` | `4a61879a69c7979c871b5e0b6b97ff4de4ceeef11adde77f8c9bf3dad7795e4d` |
| `docs/arc42/07-deployment-view.md` | `90ee12b31a438fec32ec65bd91d8988ee199c9de39cc40b46909a79dfa90f56b` |
| `docs/arc42/08-crosscutting-concepts.md` | `3ff16ad9deea8c65d3aeb282563d3fecaa60d77e88f7e98e6821397e79502939` |
| `docs/arc42/09-architecture-decisions.md` | `55459bc3daf15ab72e6f20b7836f4866916b9653c6231fee3610250c9af8a3f5` |
| `docs/arc42/11-risks-and-technical-debt.md` | `e2a0a47d6a78b863d0dc85fba52f448f7fe7c830c508c38e3adf5e2d9fd4da28` |
| `docs/architecture/data-ownership.md` | `3c814b69375b3ff70df736926e18a5688b42744f44f88fe55347376243b1078e` |
| `docs/architecture/service-boundaries.md` | `d27af63c11a48de4c7498b677f434545c30facbde1c908647e75a5bad5db7db7` |

The context pack is stale when any recorded hash changes before workflow
execution.
