# FA-MSA-001 Testbed

`services:testbed` is the non-production integration and system-test boundary
for the FA-MSA-001 workflow.

It preserves the current `forensic-analytics-testbed` coverage in a
service-root location while the legacy module remains active as rollback and
current-quality-gate evidence. The testbed owns deterministic repository
fixtures, local E2E tests and optional hardening scenarios. It is not a
productive backend service and must not become a shared Java implementation
module or runtime dependency for production services.

S15 keeps WildFly hardening here as default-skipped non-production evidence,
but productive logging and Spring architecture ownership lives in the
service-local `*ArchitectureTest` suites for the target services.

S16 changes `RepositoryAnalysisTestbedTest` into deprecation evidence for the
legacy in-process repository-analysis runtime scenario. It verifies target CLI
and service contracts without running the legacy engine path and is not
completed local analysis parity.

## Verification

Run the service-local testbed gate with:

```bash
./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace
```

The root quality gate still includes `forensic-analytics-testbed` until a later
retirement slice proves caller migration, parity and rollback evidence.

## Deployment Scope

The testbed may reference verified local deployment descriptors such as
`deployment/docker-compose/repository-to-btm.local.yml` for integration
environment evidence. It does not add a Dockerfile, Docker Compose service,
Docker Swarm stack or Kubernetes manifest in this slice.
