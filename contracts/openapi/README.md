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

FA-MVP-0001 adds current-verified repository checkout workspace routes under
the `Workspaces` tag for metadata preview, create/reuse, get and branch
refresh. These routes belong to `query-report-api-service` as a sanitized
public facade only. The repository checkout workspace, branch state, source
snapshot references, private checkout paths and H2 persistence remain owned by
`repository-source-service`. Reports, replay, LLM and broader query/list
routes remain planned until later service slices implement and test them.

Slice S18 moves executable OpenAPI contract-test ownership to
`query-report-api-service`. The service-local executable OpenAPI contract test
`GatewayOpenApiContractTest` reads `gateway-api.yaml` directly and verifies the
current repository-to-BTM submission/status contract without changing the
public API shape. FA-MVP-0001 S02 added workspace contract coverage, S07
implemented the public facade and S10 closed leakage, idempotency, restart and
refresh regression coverage. The legacy
`forensic-analytics-rest` contract test is
historical predecessor evidence after S05 source-tree removal; compatibility
wording remains provenance only, not an active rollback runtime claim.
