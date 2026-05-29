# Report Generation Service

## Status

Planned service root. No implementation exists yet.

Slice 16 explicitly defers this service from repository-to-BTM pipeline
acceptance. The current accepted BTM path ends at generated BTM artifact
delivery through predecessor Gateway/Analysis Store integration and BTM
Generation owner APIs. Reports, incident packages, LLM-ready packages and live
LLM output remain future generated artifacts.

This optional later service may own standalone report-generation behavior if a
future requirement promotes it. FA-MSA-001 mandatory query/report ownership
currently belongs to `query-report-api-service` for public read models,
generated report packages, LLM-ready packages and stored generated LLM output
as labeled generated analysis or hypotheses, never verified evidence.

Before implementation, a future slice must define report contracts, evidence
package inputs, Graph Replay dependency behavior, deterministic rendering
rules, generated-output labeling, redaction policy and tests proving reports do
not overwrite or relabel evidence.

## Deployment Readiness

`deployment/docker-compose/services/report-generation-service.compose.yml` is a
profile-gated deployment marker for the planned service root. It verifies local
deployment documentation references without starting a runnable report
generation service. It does not define a Dockerfile, publish ports, claim a
health endpoint, generate report data or label hypotheses or generated output
as verified forensic evidence.
