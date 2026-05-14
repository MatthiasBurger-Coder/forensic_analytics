# 9. Architecture Decisions

## 9.1 Accepted Decisions

| ID | Decision | Status | Rationale |
|---|---|---|---|
| AD-001 | Plugins are producers, not the platform | Accepted | Keeps build integration separate from central analysis |
| AD-002 | Use a canonical analysis model | Accepted | Enables correlation across JavaParser, Joern, Byteman and runtime events |
| AD-003 | Graph DB and Vector DB are projections | Accepted | Prevents storage-specific models from becoming the source of truth |
| AD-004 | Runtime data is sensitive by default | Accepted | Protects secrets, personal data and business-critical values |
| AD-005 | LLM diagnosis must be evidence-based | Accepted | Reduces hallucination risk and improves reviewability |
| AD-006 | Automated repair is gated | Accepted | Prevents unsafe autonomous changes |

## 9.2 Open Decisions

| ID | Open Decision | Notes |
|---|---|---|
| OD-001 | Initial relational database | Not selected in EPIC v0.1 |
| OD-002 | Initial Graph DB | Not selected in EPIC v0.1 |
| OD-003 | Initial Vector DB | Not selected in EPIC v0.1 |
| OD-004 | Runtime ingestion mode | JSONL likely for MVP, HTTP collector later |
| OD-005 | Runtime value storage policy | Needs redaction rule model |
| OD-006 | Initial LLM provider | Must remain replaceable |
| OD-007 | Source-code loading and versioning in UI | Needs later design |
| OD-008 | Multi-repo and multi-service trace model | Needs later design |
