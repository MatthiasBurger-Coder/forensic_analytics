# Forensic UI

Standalone React, TypeScript and Vite operator UI for the Forensic Analytics REST MVP.

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

The UI registers and prepares repository analysis sessions through HTTP/REST. It does not call gRPC, WebSocket, SSE or gRPC-Web from the browser, and it does not claim that the full repository analysis pipeline has completed.
