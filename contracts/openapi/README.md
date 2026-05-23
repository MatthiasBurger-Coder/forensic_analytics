# OpenAPI Contracts

## Status

Planned OpenAPI contract root.

`gateway-api.yaml` is a transitional predecessor filename for the public REST
contract. Under FA-MSA-001 its target authority is
`query-report-api-service`. Existing Gateway wording in command names, tests or
file names remains compatibility evidence only; it does not make
`forensic-gateway-service` the target service.

Operations marked `current-verified` are based on the current REST adapter,
predecessor Gateway evidence and S08 `query-report-api-service` target-service
tests. Operations marked `planned-initial` are logical target contracts and are
not implementation evidence until later service slices implement and test them.

Slice S18 moves executable OpenAPI contract-test ownership to
`query-report-api-service`. The service-local executable OpenAPI contract test
`GatewayOpenApiContractTest` reads `gateway-api.yaml` directly and verifies the
current repository-to-BTM submission/status contract without changing the
public API shape. The legacy `forensic-analytics-rest` contract test remains
rollback evidence until a later removal slice proves caller-free retirement.
