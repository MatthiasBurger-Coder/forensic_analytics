# FA-MSA-001 Testbed

`services:testbed` is the non-production integration and system-test boundary
for the FA-MSA-001 workflow.

It preserves predecessor `forensic-analytics-testbed` coverage in a
service-root location after S05 source-tree removal. The retired source tree is
historical predecessor evidence only. The testbed owns deterministic
repository fixtures, local E2E tests and optional hardening scenarios. It is
not a productive backend service and must not become a shared Java
implementation module or runtime dependency for production services.

S15 keeps WildFly hardening here as default-skipped non-production evidence,
but productive logging and Spring architecture ownership lives in the
service-local `*ArchitectureTest` suites for the target services.

S16 changes `RepositoryAnalysisTestbedTest` into deprecation evidence for the
legacy in-process repository-analysis runtime scenario. It verifies target CLI
and service contracts without running the legacy engine path and is not
completed local analysis parity.

S03 confirms `RepositoryAnalysisMiniEndToEndTest` and
`RepositoryAnalysisRealRepositoryEndToEndTest` into repository checkout and
ingestion boundary evidence. The former mini and real repository fixture
behavior remains legacy rollback evidence only: `AnalyzeRepository`, local or
file repository checkout, monolith analysis-session registration and direct
workspace cleanup are deprecated target behavior, and target services do not
accept local or file repository input. Replacement evidence is split across
`repository-source-service` checkout/workspace/source-snapshot tests,
`ingestion-service` `AnalyzeRepository` `UNIMPLEMENTED` tests and
`analysis-orchestrator-service` pending `StartRepositoryToBtm` tests. This is
not session-registration parity and does not claim completed local repository
analysis parity.

## Verification

Run the service-local testbed gate with:

```bash
./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace
```

The root quality gate runs the registered service projects, including
`services:testbed`, and does not run a legacy `:forensic-analytics-testbed`
Gradle task in the current service-only build.

## Deployment Scope

The testbed may reference verified local deployment descriptors such as
`deployment/docker-compose/repository-to-btm.local.yml` for integration
environment evidence. It does not add a Dockerfile, Docker Compose service,
Docker Swarm stack or Kubernetes manifest in this slice.
