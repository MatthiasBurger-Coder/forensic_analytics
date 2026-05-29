# Quality And Leakage Gates

## Authority

`QUALITY.md` is the authoritative quality contract.

Minimum repository command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Slice-Level Checks

For Java application services with Dockerfiles:

```bash
./gradlew :<module>:test --dependency-verification strict --console=plain --stacktrace
./gradlew :<module>:bootJar --dependency-verification strict --console=plain --stacktrace
docker compose -f deployment/docker-compose/services/<module>.compose.yml config
git diff --check
```

Before claiming Docker image-build readiness for root-context service
Dockerfiles, verify that `.dockerignore` re-includes the affected service boot
jar path:

```bash
git diff -- .dockerignore
./gradlew --no-daemon --max-workers=1 :<module>:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f <module>/Dockerfile --build-arg SERVICE_JAR=<module>/build/libs/<module>-0.1.0-SNAPSHOT.jar -t forensic-analytics/<module>:local .
```

For `cli-client`:

```bash
./gradlew :cli-client:test --dependency-verification strict --console=plain --stacktrace
./gradlew :cli-client:build --dependency-verification strict --console=plain --stacktrace
docker compose -f deployment/docker-compose/services/cli-client.compose.yml config
git diff --check
```

For planned or non-production roots:

```bash
./gradlew :<module>:tasks --dependency-verification strict --console=plain --stacktrace
docker compose -f deployment/docker-compose/services/<module>.compose.yml config
git diff --check
```

For the UI:

```bash
cd forensic-ui && npm ci
cd forensic-ui && npm run test
cd forensic-ui && npm run build
docker compose -f deployment/docker-compose/services/forensic-ui.compose.yml config
git diff --check
```

## Runtime Smoke Checks

When Docker is available and image builds succeed:

```bash
docker compose -f deployment/docker-compose/forensic-analytics.local.yml build
docker compose -f deployment/docker-compose/forensic-analytics.local.yml up -d
docker compose -f deployment/docker-compose/forensic-analytics.local.yml ps
curl -fsS http://127.0.0.1:<query-report-host-port>/api/health
curl -fsS http://127.0.0.1:<ui-host-port>/
docker compose -f deployment/docker-compose/forensic-analytics.local.yml logs --no-color --tail=200
docker compose -f deployment/docker-compose/forensic-analytics.local.yml down
```

The execution report must record the exact host ports used.

## Leakage Gates

Stop a slice if:

- repository-source private workspace or H2 volumes are mounted into non-owner
  services;
- UI or public API responses expose private paths, stack traces, credentials,
  tokens, raw Git output, or internal worker diagnostics;
- `forensic-ui` still returns the hardcoded nginx `502 BACKEND_UNAVAILABLE` for
  `/api` while the deployment claims GUI/API integration;
- browser access depends on unverified CORS behavior instead of a verified
  same-origin proxy or an explicitly tested browser-safe route;
- `.dockerignore` excludes a service boot jar needed by a Dockerfile while the
  slice claims image-build readiness;
- static analysis, Joern, graph, replay, report, or generated LLM output is
  described as observed runtime evidence;
- planned roots are represented as running services without verified runtime;
- logs or traces are treated as canonical forensic evidence;
- Docker runtime checks are skipped but the docs claim they passed.

## Optional Checks

Docker image builds, Compose startup, and Joern image pulls are external
runtime checks. They should be run when available. If skipped because Docker,
network access, or external images are unavailable, the result must be reported
without claiming runtime readiness.
