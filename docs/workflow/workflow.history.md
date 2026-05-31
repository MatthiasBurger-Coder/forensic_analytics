# Workflow History

## 2026-05-29

Initial workflow version for moving repository-source workspace metadata from
the Docker-local H2 MVP adapter to PostgreSQL while keeping checkout bytes on
the repository-source workspace volume.

Slices S01 through S06 were executed against this version.

## 2026-05-31

Scope update accepted from user clarification:

- H2 may remain for tests and deterministic fixtures.
- PostgreSQL is the runtime and production persistence path.
- Missing or unreachable PostgreSQL must be reported by startup failure or
  storage health/readiness `DOWN`; it must not silently fall back to H2.
- Database configuration must be available through operator Settings in the
  existing UI.

This version replaces the previous S07/S08 tail with:

- S07 PostgreSQL Runtime Default and H2 Test Boundary.
- S08 Database Settings Contract and Backend Handoff.
- S09 React Database Settings UI.
- S10 End-to-End Verification and Release Readiness.

Previously completed S01-S06 checkpoint commits remain valid historical
execution evidence. S07 and later slices must rerun S3D against this workflow
version before modifying files.
