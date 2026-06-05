# arc42 Check Status

## Status

Checked and updated for the FA-MVP-0001 PostgreSQL correction on
`2026-06-04`.

## Checked Sections

| Section | Result |
|---|---|
| `docs/arc42/04-solution-strategy.md` | Updated so FA-MVP-0001 uses repository-source PostgreSQL metadata persistence and no longer describes H2 MVP runtime persistence. |
| `docs/arc42/05-building-block-view.md` | Updated so ADR-0023 H2 scope is tests/direct fixtures only and ADR-0024 is runtime metadata authority. |
| `docs/arc42/07-deployment-view.md` | Updated so the current Docker-local view uses `forensic-postgres` plus the repository-source workspace volume, with no active H2 data mount. |
| `docs/arc42/08-crosscutting-concepts.md` | Updated so historical H2 files are private historical data/test fixtures only and not active fallback storage. |
| `docs/arc42/09-architecture-decisions.md` | Updated to summarize revised ADR-0023 and ADR-0024 consequences. |
| `docs/arc42/10-quality-requirements.md` | Updated quality scenarios from H2 runtime persistence to PostgreSQL runtime persistence and storage readiness visibility. |
| `docs/arc42/11-risks-and-technical-debt.md` | Already records the risk that repository-source PostgreSQL could be mistaken for shared analytics persistence. |

## Architecture Consequences

- `repository-source-service` is the only writer for repository checkout
  workspace metadata.
- PostgreSQL is private repository-source metadata storage.
- Repository checkout bytes stay on private repository-source workspace storage.
- H2 is deterministic adapter test/direct fixture scope only.
- No public DTO may leak database, H2, private filesystem, raw Git or credential
  details.

## Remaining Open Decisions

- Broader canonical analytics persistence remains outside ADR-0024.
- Historical H2 file preservation requires an explicit migration slice if the
  user later requires it.
- Docker Compose model validation does not prove image startup, health, Swarm
  or Kubernetes readiness.
