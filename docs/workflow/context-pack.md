# Workflow Context Pack

## Identity

- Workflow: FA-MVP-0001 PostgreSQL Repository Workspace Checkout Alignment
- Version: `2026-06-04`
- Branch: `feature/workflow-repository-workspace-checkout-20260604`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`

## Affected Areas

- FA-MVP-0001 workflow documentation
- Repository-source persistence documentation
- Data ownership and service-boundary references
- arc42 deployment, building-block, crosscutting, decision and quality notes
- Quality-gate planning

## Forbidden Areas

- Product source implementation
- OpenAPI or gRPC contract mutation
- Build logic mutation
- Docker runtime mutation
- Frontend code mutation
- ADR reopening or weakening
- H2 runtime, Docker or readiness fallback behavior

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester
- Senior Documentation Engineer
- Senior DevOps Engineer

Callable subagents were not used because the user did not explicitly request
delegated or parallel agent work. The role files were used as local review
checklists.

## Governing Decisions

- ADR-0013: Data ownership per service
- ADR-0016: Branch-first workflow creation
- ADR-0023: H2 accepted for repository-source tests only
- ADR-0024: PostgreSQL for repository-source workspace metadata

## Quality Commands

```bash
git diff --check
./gradlew :repository-source-service:test --tests "*RepositorySourceServiceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourcePostgresPersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourceH2PersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceServiceTest" --dependency-verification strict --console=plain --stacktrace
cd forensic-ui && npm run test -- src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `a1bd05f38d8602a5c67dcfbf3628c811a4c0de33004fdf553afadf4b5c614f5d` |
| `QUALITY.md` | `95f9aa2ba5dd7f67057864d52321e5716acb46b166bc74c12b154ec271bb3596` |
| `.codex/AGENTS.md` | `d43bc1ea9ca10deb8a6553c7c21208e8020d751f674a8cb824959b666bd01aec` |
| `.codex/workflow/workflow-execution-rules.md` | `6d2ea74943f8ff4187a371d6e263fcf7f179d7250868086f4ba294e6f5fa122a` |
| `.agents/orchestrator/routing-rules.md` | `64d7b815bb9f5d1da72b42822a3e532fa79d08c8391cf2fbb7da3de390a8d740` |
| `.agents/orchestrator/swarm-orchestrator.md` | `860d2ad867b08838d8155ffaa580bec50f708b40eacef91920e65e76040eded8` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md` | `45f80d99219c4b8fc225b59b318d999c552f2d5807d7070546cae88023c49179` |
| `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md` | `bd155271d13945677d998468a7c41df4f98e71ebb7e9913d2c7d08f931f4b472` |
| `docs/architecture/data-ownership.md` | `d11c735558a9e9428cb36e644d3308c393378c806484da39a09010a297ce6c7e` |
| `docs/architecture/service-boundaries.md` | `df4467d8075f9712f3cae29a817af1f42c60cb27a8b1d72cb3c832a08f0c0d15` |
| `docs/architecture/current-state.md` | `a5dc69c1b9ebd78dcd19d93be4e890837dd7468d49e8e8b693446d414312ddf0` |
| `docs/architecture/service-roots.md` | `e1ac463e931fe4ef521508a26e2db021d0f6c8f2e0148fad71bb43c94cc2325a` |
| `docs/architecture/current-build-and-test-map.md` | `f63a9cede27df1ac5408f9975d18bcaa9d782f307cf343bfdca0c70be39a494f` |
| `docs/arc42/04-solution-strategy.md` | `4e0036cd9c0a1c39d1ce39d4278368c13ad92114dbbc388cad88bb3b29ef1246` |
| `docs/arc42/05-building-block-view.md` | `e2aeff134f596529013db9182da24e5b7697581ce269a57ce9a656deeecacbfb` |
| `docs/arc42/07-deployment-view.md` | `68b91cd3173a915de8d0d26f66dca9bf20706af45de0db4528ed1bbd12d24552` |
| `docs/arc42/08-crosscutting-concepts.md` | `b2d00de837696f080ab7c865901ee2bbcf88deb360cf33ab10bcf2db68182fe3` |
| `docs/arc42/09-architecture-decisions.md` | `bb9b358e3a120d8a8f3ed4ea9e1fc38e3aa2cefac4a273b8db042d865d545c4a` |
| `docs/arc42/10-quality-requirements.md` | `79c3093edf86840babb2ab084e3dce4dc711ec532205648f46520d2e6bcbe7bf` |
| `docs/arc42/README.md` | `f4855e8a7c580be7532450dd5cc2ccfb9b5d851db7772cae4228d285084f7b4d` |
| `repository-source-service/README.md` | `b86f915273221ad04a6a1885a530cd05e60558098708093d0e5e44a21b049738` |
| `query-report-api-service/README.md` | `669c33c105e4e2deabbdd9dacbb1267b61a1b3fce6ec7d25bfe80ee85016c015` |
| `repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java` | `35cda0a2aa1b7a360a19fa21f15c6a71369aa3287dccc5f6f74448a5e004d8ca` |

This context pack is a navigation aid only. Root `AGENTS.md`, `QUALITY.md`,
ADRs, arc42, routing rules and skill files remain authoritative.
