# Quality Gate Plan

`QUALITY.md` is the authoritative quality contract for this repository.

## Workflow Creation Verification

Workflow creation is documentation-only. Required verification after authoring:

```bash
git diff --check
```

Diff inspection is also required:

```bash
git status --short
git diff -- docs/workflow
```

## Minimum Repository Verification

Use before broader validation when production code, tests, build logic,
contracts or runtime behavior change:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Quality Gate

Use as the final repository quality gate for production, build, contract,
adapter, persistence, runtime, replay, graph, reporting, LLM or architecture
changes:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Diagnostic Build Health

This command is useful but incomplete unless `checkPackageCoverage` is known to
be wired into `check`:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

## Frontend Verification

The verified current frontend root is `forensic-ui`.

Use inside `forensic-ui` until a later slice verifies a move to
`frontend/frontend-web-app`:

```bash
npm test
npm run build
```

## Service-Specific Verification

Service-specific commands such as:

```bash
./gradlew :services:forensic-ingestion-service:test
```

are planned targets only until the service exists in `settings.gradle.kts` or an
independent service-local build is verified. Do not claim these commands pass
before they are executable in the repository.

## Docker Verification

Docker Compose, Docker Swarm and Kubernetes checks are required only after the
corresponding manifests are created and verified.

Planned local compose checks:

```bash
docker compose -f deployment/docker-compose/docker-compose.yml config
docker compose -f deployment/docker-compose/docker-compose.yml build
docker compose -f deployment/docker-compose/docker-compose.yml up -d
docker compose -f deployment/docker-compose/docker-compose.yml ps
docker compose -f deployment/docker-compose/docker-compose.yml down
```

Planned Kubernetes dry run when `kubectl` is locally available:

```bash
kubectl apply --dry-run=client -f deployment/kubernetes/
```
