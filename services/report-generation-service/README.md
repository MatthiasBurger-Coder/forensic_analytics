# Report Generation Service

## Status

Planned service root. No implementation exists yet.

Slice 16 explicitly defers this service from repository-to-BTM pipeline
acceptance. The current accepted BTM path ends at generated BTM artifact
delivery through Gateway/Analysis Store/BTM Generation owner APIs. Reports,
incident packages, LLM-ready packages and live LLM output remain future
generated artifacts.

This service will own reports, incident context packages and LLM-ready or
generated analysis packages. Generated LLM output must remain labeled as
generated analysis or hypothesis, never verified evidence.

Before implementation, a future slice must define report contracts, evidence
package inputs, Graph Replay dependency behavior, deterministic rendering
rules, generated-output labeling, redaction policy and tests proving reports do
not overwrite or relabel evidence.
