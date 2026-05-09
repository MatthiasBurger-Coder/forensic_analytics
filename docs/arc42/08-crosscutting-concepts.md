# 8. Crosscutting Concepts

## 8.1 Canonical IDs

The platform uses stable IDs to correlate static facts, semantic facts, Byteman rules and runtime events.

Important IDs:

- `projectId`
- `moduleId`
- `sourceFileId`
- `classKey`
- `methodKey`
- `callsiteKey`
- `branchKey`
- `ruleId`
- `analysisRunId`
- `runtimeSessionId`
- `correlationId`
- `traceId`
- `spanId`
- `parentSpanId`
- `incidentId`

## 8.2 Runtime Data Sensitivity

Runtime data must be treated as sensitive by default.

Supported mechanisms:

- Allowlisting
- Redaction
- Hashing
- Masking
- Length limits
- Sampling
- Retention
- Encryption
- Auditing

## 8.3 Evidence-Based LLM Usage

LLM analysis must be based on curated evidence packages. The LLM must not invent missing facts. If evidence is insufficient, the diagnosis must state the limitation.

## 8.4 Graph and Vector Projections

Graph DB and Vector DB are projections from the canonical analysis model. They are optimized views, not the source of truth.

## 8.5 Ambiguity Handling

Ambiguous mappings between JavaParser, Joern, Byteman rules and runtime events must be marked with confidence levels. Unclear mappings must not be silently accepted.

## 8.6 Replay Uncertainty

The replay must explicitly show missing, incomplete or uncertain event chains.
