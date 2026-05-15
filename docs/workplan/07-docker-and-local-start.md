# Docker And Local Start Plan

## Frontend Container

Implemented:

```text
forensic-ui/Dockerfile
forensic-ui/nginx.conf
forensic-ui/.dockerignore
```

The Dockerfile uses a multi-stage build:

```text
Node build stage
  -> npm ci
  -> npm run build

nginx runtime stage
  -> serve Vite static build
  -> support SPA route fallback
  -> expose port 80
```

## API Base URL

The frontend uses:

```text
VITE_API_BASE_URL
```

Default local value:

```text
/api
```

The nginx config serves the Vite build with SPA fallback. It returns a JSON `502 BACKEND_UNAVAILABLE` response for `/api` because no root compose file or backend service name exists in this repository slice. Configure `VITE_API_BASE_URL` at frontend build time when the REST API is not reachable at `/api`.

## Compose Integration

No root compose file exists. This slice did not add one.

Suggested service shape after backend service names are verified:

```yaml
forensic-ui:
  build:
    context: ./forensic-ui
  ports:
    - "3000:80"
  depends_on:
    - verified-backend-service-name
```

Do not use `forensic-api-gateway` unless that service actually exists or is created in the same slice.

## Local Development

Local frontend development:

```bash
cd forensic-ui
npm install
npm run dev
```

The Vite dev server is available at:

```text
http://127.0.0.1:5173/
```

Local backend runtime:

```bash
./gradlew :forensic-analytics-bootstrap:run --dependency-verification strict --console=plain --stacktrace
```

By default the REST API is enabled on `127.0.0.1:8080` and the gRPC ingestion server is enabled on port `9090`.

REST settings:

```text
FORENSICS_ANALYTICS_REST_ENABLED=true
FORENSICS_ANALYTICS_REST_HOST=127.0.0.1
FORENSICS_ANALYTICS_REST_PORT=8080
```

Equivalent system properties are:

```text
forensics.analytics.rest.enabled
forensics.analytics.rest.host
forensics.analytics.rest.port
```

If the REST backend runs on a different port than the Vite dev server, configure Vite proxy or `VITE_API_BASE_URL` explicitly.

For local Vite development against the default REST port:

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080/api npm run dev
```

`VITE_API_BASE_URL` is compiled into the Vite bundle. Passing it to `docker run -e` does not reconfigure an already built image.

The frontend container can be built with:

```bash
docker build -t forensic-ui:local ./forensic-ui
```
