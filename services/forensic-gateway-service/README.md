# Forensic Gateway Service

## Status

Slice 04 runtime shell.

This service is the external API and UI/CLI facade shell. It currently exposes
Gateway-local health and status endpoints only:

- `GET /health`
- `GET /api/health`
- `GET /api/status`

Repository submission, worker orchestration, Analysis Store queries, BTM byte
delivery, replay, reporting and frontend integration are later workflow slices.
The Gateway must not contain AST, Joern, BTM, storage, replay or reporting
business logic and must not depend on worker service implementation classes.
