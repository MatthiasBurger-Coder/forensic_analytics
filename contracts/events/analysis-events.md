# Analysis Event Contracts

## Status

FA-MSA-001 Slice 03 initial planned event contract.

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

Producer: `query-report-api-service`

Consumers: `ingestion-service`, `analysis-orchestrator-service`

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

Producer: `analysis-orchestrator-service`

Consumers: `repository-source-service`, `java-parser-analysis-service`,
`joern-analysis-service`, optional later worker services and
`query-report-api-service` status projections

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Planned worker kind. |
| `sourceSnapshotId` | string | no | Source snapshot for source-based jobs. |
| `inputArtifacts` | array | yes | Artifact references used as job input. |
| `completeness` | enum | yes | `COMPLETE`, `INCOMPLETE` or `UNKNOWN`. |

### `analysis.job.dispatchable`

Producer: `analysis-orchestrator-service`

Consumers: `repository-source-service`, `java-parser-analysis-service`,
`joern-analysis-service` and optional later worker services

Payload fields match `analysis.job.accepted` and add:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `attempt` | integer | yes | Current attempt number. |
| `leaseAfter` | string | no | RFC 3339 timestamp after which a worker may lease the job. |

### `analysis.job.running`

Producer: worker service that leased the job

Consumers: `analysis-orchestrator-service`, `query-report-api-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `workerId` | string | yes | Worker instance id. |
| `attempt` | integer | yes | Running attempt. |
| `leaseExpiresAt` | string | yes | RFC 3339 lease expiry. |

### `analysis.job.progressed`

Producer: worker service

Consumers: `analysis-orchestrator-service`, `query-report-api-service`

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `workerKind` | enum | yes | Worker kind. |
| `attempt` | integer | yes | Attempt that reported progress. |
| `percentComplete` | integer | no | Approximate progress from 0 to 100. |
| `diagnostics` | array | yes | Sanitized diagnostic messages. |

### `analysis.job.completed`

Producer: worker service

Consumers: `analysis-orchestrator-service`, `query-report-api-service`

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

Consumers: `analysis-orchestrator-service`, `query-report-api-service`

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

Producer: `analysis-orchestrator-service`

Consumers: `query-report-api-service`, operations tooling

Payload fields match `analysis.job.failed` and add:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `deadLetterReason` | string | yes | Sanitized reason the job left retry flow. |
| `failureCount` | integer | yes | Number of recorded failures. |

### `analysis.artifact.registered`

Producer: service that owns produced artifact bytes

Consumers: S04-approved canonical artifact metadata owner and downstream
worker services

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `artifacts` | array | yes | Artifact references with path, type, checksum and size. |
| `producerService` | string | yes | Service that produced the artifact metadata. |
| `byteOwnerService` | string | yes | Service that owns artifact bytes. |
| `metadataOwnerService` | string | yes | Service that owns accepted artifact metadata after registration. |
| `byteCustody` | enum | yes | `PRODUCER_RETAINED`, `SCOPED_OBJECT_ACCESS` or `EXPLICIT_HANDOFF`. |
| `completeness` | enum | yes | Completeness of artifact set. |

The S04-approved canonical artifact metadata owner registers accepted artifact
metadata only. It does not transfer byte custody unless a later explicit
handoff or object-access contract records that transfer.

### `analysis.report.requested`

Producer: `query-report-api-service`

Consumers: `query-report-api-service` service-local report adapter, optional
later `report-generation-service` only if a later requirement approves it, and
the S04-approved report artifact owner

Payload:

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `reportType` | enum | yes | `SUMMARY`, `INCIDENT_CONTEXT` or `LLM_CONTEXT_PACKAGE`. |
| `includeGeneratedHypotheses` | boolean | yes | Whether generated text may be included. |

### `analysis.report.completed`

Producer: `query-report-api-service` service-local report adapter or optional
later `report-generation-service` only if a later requirement approves it

Consumers: `query-report-api-service` and the S04-approved report artifact owner

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
