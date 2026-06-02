# Deployment Description

This workflow may require rebuilding and restarting affected local services during `workflow execute` if the observed UI still drops `repositoryBranches` after code-level tests pass.

Affected runtime components:

- `repository-source-service`
- `query-report-api-service`
- `forensic-ui`
- `forensic-postgres` only as repository-source metadata storage governed by ADR-0024

Operational diagnostic target:

```http
POST /api/workspace-metadata
Content-Type: application/json

{"repositoryUrl":"https://github.com/wildfly/wildfly.git"}
```

Expected diagnostic condition: response contains a non-empty `repositoryBranches` array when the remote metadata lookup succeeds. The workflow must not assert a fixed branch count from the GitHub UI.
