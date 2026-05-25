# Forensic UI

Standalone React, TypeScript and Vite operator UI for the Forensic Analytics query-report REST API.

## Local Development

```bash
cd forensic-ui
npm install
npm run dev
```

The dev server runs at `http://127.0.0.1:5173/`.

The UI reads `VITE_API_BASE_URL`; the default is `/api`.

For local development against the default REST backend port:

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080/api npm run dev
```

## Verification

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

No lint script is configured in this slice.

## Docker

```bash
docker build -t forensic-ui:local ./forensic-ui
```

The nginx image serves the static Vite build with SPA fallback. It returns a JSON `502 BACKEND_UNAVAILABLE` response for `/api` because this repository has no root compose service name for the REST backend.

`VITE_API_BASE_URL` is compiled into the Vite bundle. Passing a different value to `docker run -e` does not reconfigure an already built image.

## Current Scope

The UI creates repository checkout workspaces through the verified public routes `/api/workspace-metadata`, `/api/workspaces`, `/api/workspaces/{workspaceId}` and `/api/workspaces/{workspaceId}/branches/{workspaceBranchId}/refresh`. Metadata is rendered only from the public preview response, `workspaceTitle` is read-only, and workspace save/branch refresh operations send `X-Correlation-Id` and `Idempotency-Key`.

The UI still starts repository-to-BTM sessions through `/api/repository-analyses`, including the required `X-Correlation-Id` and `Idempotency-Key` headers. It does not call internal worker services, repository-source-service, Git remotes, gRPC, WebSocket, SSE or gRPC-Web from the browser.

Dashboard and aggregate diagnostics views stay isolated until query-report-api-service exposes verified public list/query routes.
