# Deployment Description

This workflow affects local runtime only when slices modify
`repository-source-service` or `forensic-ui` behavior.

Expected changed services:

- `repository-source-service`
- `forensic-ui`

Restart guidance after implementation:

```bash
./gradlew --no-daemon --max-workers=1 :repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker compose --env-file docker/postgres/.env -p forensic-analytics-local \
  -f deployment/docker-compose/services/repository-source-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  build repository-source-service forensic-ui
docker compose --env-file docker/postgres/.env -p forensic-analytics-local \
  -f deployment/docker-compose/services/repository-source-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  up -d --no-deps repository-source-service forensic-ui
```

Do not remove volumes during this workflow unless a final-delete slice
explicitly verifies the operator intent and affected data ownership.
