# CLI Client

`cli-client` is the FA-MSA-001 target public API client boundary.

S09 implements only the transitional `gateway-submit` compatibility command for
repository-to-BTM submission through `query-report-api-service` HTTP/OpenAPI.
It does not implement analysis execution, JavaParser or Joern control,
persistence access, local workspace handling, status reads or report reads.

S16 records local `analyze` and `ingest-request` as deprecated target behavior.
`cli-client` rejects those legacy command names and does not implement
analysis execution, does not implement engine-request import and does not route
local paths or engine-request files to the public API.

## Build And Test

```bash
./gradlew :services:cli-client:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:cli-client:build --dependency-verification strict --console=plain --stacktrace
```

## Run

```bash
./gradlew :services:cli-client:run --args="gateway-submit --gateway http://localhost:8080/api --repo-url https://example.com/acme/demo.git --branch main --request-id request-1 --schema-version gateway.v1 --requested-outputs BTM_RULES --provider github --build-tool gradle --build-id build-1 --root-project demo --declared-modules :app,:lib --correlation-id correlation-1 --idempotency-key idem-1 --timeout-seconds 60 --max-workspace-bytes 100000 --allow-shallow-clone true"
```
