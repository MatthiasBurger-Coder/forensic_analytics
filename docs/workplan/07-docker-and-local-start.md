# Docker And Local Start Plan

## Frontend Container

Create:

```text
forensic-ui/Dockerfile
forensic-ui/nginx.conf
forensic-ui/.dockerignore
```

Use a multi-stage build:

```text
Node build stage
  -> npm install or npm ci
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

The nginx config may proxy `/api` to the backend only after the backend service name and port are verified. If no compose service exists, document the expected proxy target instead of inventing one.

## Compose Integration

No root compose file was verified. If implementation adds one, create it deliberately and document why.

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

Document:

```bash
cd forensic-ui
npm install
npm run dev
```

and the backend command selected by the REST runtime slice.

If the REST backend runs on a different port than the Vite dev server, configure Vite proxy or `VITE_API_BASE_URL` explicitly.
