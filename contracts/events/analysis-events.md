# Analysis Event Contracts

## Status

Slice 03 initial planned event contract.

The user approved logical initial contracts for service communication on
2026-05-16. These events are design artifacts for future service slices. They
do not prove that a broker, topic, queue, publisher or consumer is implemented.

## Broker Assumption

The initial event model targets a RabbitMQ-compatible topic exchange because
repository governance allows RabbitMQ or message contracts for service
integration. Concrete broker deployment remains a later DevOps slice.

Planned exchange:

```text
forensic.analysis.v1
```

Delivery model:

- At-least-once delivery.
- Consumers must deduplicate by `eventId`.
- Producers must include stable `idempotencyKey` when an event is caused by a
  command or retryable request.
- Ordering is guaranteed only per `analysisRunId` when the broker topology can
  preserve a partition or routing key.
- Missing or delayed events must be represented as incomplete or unavailable
  state, not repaired silently.

## Envelope

Every event uses this envelope:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `eventId` | string | yes | Globally unique event id for deduplication. |
| `eventType` | string | yes | One of the event names in this file. |
| `schemaVersion` | string | yes | Event schema version, initially `1.0.0`. |
| `occurredAt` | string | yes | RFC 3339 UTC timestamp from the producer. |
| `producerService` | string | yes | Service that produced the event. |
| `correlationId` | string | yes | Request or workflow correlation id. |
| `causationId` | string | no | Command, request or event id that caused this event. |
| `idempotencyKey` | string | no | Stable retry and duplicate-detection key. |
| `analysisRunId` | string | yes | Analysis run affected by the event. |
| `jobId` | string | no | Worker job id when the event is job-scoped. |
| `payload` | object | yes | Event-specific payload. |

Envelope fields must not contain secrets, credentials, raw source content,
runtime method parameters, method return values or LLM prompt content.

## Events

### `analysis.run.requested`

Producer: `forensic-gateway-service`

Consumers: `forensic-ingestion-service`, `analysis-store-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `repositoryUrl` | string | yes | Requested repository URL. |
| `branch` | string | no | Requested branch. |
| `commit` | string | no | Requested commit. |
| `schemaVersion` | string | yes | Request schema version. |
| `buildTool` | string | yes | Build tool named by the requester. |

Either `branch` or `commit` must be present.

### `analysis.job.accepted`

Producer: `analysis-store-service`

Consumers: worker services, `forensic-gateway-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Planned worker kind. |
| `sourceSnapshotId` | string | no | Source snapshot for source-based jobs. |
| `inputArtifacts` | array | yes | Artifact references used as job input. |
| `completeness` | enum | yes | `COMPLETE`, `INCOMPLETE` or `UNKNOWN`. |

### `analysis.job.dispatchable`

Producer: `analysis-store-service`

Consumers: worker services

Payload fields match `analysis.job.accepted` and add:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `attempt` | integer | yes | Current attempt number. |
| `leaseAfter` | string | no | RFC 3339 timestamp after which a worker may lease the job. |

### `analysis.job.running`

Producer: worker service that leased the job

Consumers: `analysis-store-service`, `forensic-gateway-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `workerId` | string | yes | Worker instance id. |
| `attempt` | integer | yes | Running attempt. |
| `leaseExpiresAt` | string | yes | RFC 3339 lease expiry. |

### `analysis.job.progressed`

Producer: worker service

Consumers: `analysis-store-service`, `forensic-gateway-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `attempt` | integer | yes | Attempt that reported progress. |
| `percentComplete` | integer | no | Approximate progress from 0 to 100. |
| `diagnostics` | array | yes | Sanitized diagnostic messages. |

### `analysis.job.completed`

Producer: worker service

Consumers: `analysis-store-service`, `forensic-gateway-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `attempt` | integer | yes | Completed attempt. |
| `outputArtifacts` | array | yes | Produced artifact references. |
| `completeness` | enum | yes | Completeness of produced output. |
| `diagnostics` | array | yes | Sanitized diagnostic messages. |

### `analysis.job.failed`

Producer: worker service

Consumers: `analysis-store-service`, `forensic-gateway-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `attempt` | integer | yes | Failed attempt. |
| `reason` | string | yes | Sanitized failure reason. |
| `retryable` | boolean | yes | Whether the same job may be retried. |
| `completeness` | enum | yes | Output completeness at failure time. |
| `diagnostics` | array | yes | Sanitized diagnostic messages. |

### `analysis.job.dead-lettered`

Producer: `analysis-store-service`

Consumers: `forensic-gateway-service`, operations tooling

Payload fields match `analysis.job.failed` and add:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `deadLetterReason` | string | yes | Sanitized reason the job left retry flow. |
| `failureCount` | integer | yes | Number of recorded failures. |

### `analysis.artifact.registered`

Producer: service that owns produced artifact bytes

Consumers: `analysis-store-service`, downstream worker services

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `artifacts` | array | yes | Artifact references with path, type, checksum and size. |
| `producerService` | string | yes | Service that owns artifact bytes until accepted. |
| `completeness` | enum | yes | Completeness of artifact set. |

### `analysis.report.requested`

Producer: `forensic-gateway-service`

Consumers: `report-generation-service`, `analysis-store-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `reportType` | enum | yes | `SUMMARY`, `INCIDENT_CONTEXT` or `LLM_CONTEXT_PACKAGE`. |
| `includeGeneratedHypotheses` | boolean | yes | Whether generated text may be included. |

### `analysis.report.completed`

Producer: `report-generation-service`

Consumers: `forensic-gateway-service`, `analysis-store-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `reportId` | string | yes | Report identifier. |
| `reportType` | enum | yes | Completed report type. |
| `artifact` | object | yes | Report artifact reference. |
| `containsGeneratedText` | boolean | yes | Whether generated content exists. |

## Enumerations

`workerKind` values:

- `REPOSITORY_ANALYSIS`
- `AST_ANALYSIS`
- `JOERN_ANALYSIS`
- `BTM_GENERATION`
- `GRAPH_ANALYSIS`
- `REPORT`
- `LLM_PROJECTION`

`completeness` values:

- `COMPLETE`
- `INCOMPLETE`
- `UNKNOWN`

## Compatibility Rules

- New optional payload fields may be added in minor versions.
- Required fields, event names and enum values require a major version.
- Event names are never reused for different semantics.
- Consumers must ignore unknown optional fields.
- Producers must not remove fields from an active major version.
- Events do not replace owner APIs for strongly consistent reads.
