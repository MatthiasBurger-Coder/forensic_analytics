# ADR Conflict Analysis - 2026-06-04

## Purpose

This analysis records verified documentation conflicts and consolidation risks
found after the ADR inventory. It does not rewrite ADR history, supersede ADRs
or create a new architecture decision. Resolution belongs to later approved
workflow slices.

## Verified Inputs

- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`
- `docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md`
- `docs/adr/ADR-0005-adapter-logging-observability-boundary.md`
- `docs/adr/ADR-0017-target-microservices-service-landscape.md`
- `docs/adr/ADR-0022-final-modular-monolith-source-tree-retirement.md`
- `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md`
- `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md`

## Conflict Summary

| ID | Area | Verified conflict or risk | Required handling |
|---|---|---|---|
| C-001 | Decision index shape | `docs/arc42/09-architecture-decisions.md` mixes older `AD-*` rows with `ADR-*` rows. The older `AD-*` entries are not represented as files in `docs/adr/` and are not part of the verified ADR-0001 through ADR-0024 sequence. | Later consolidation must either classify `AD-*` entries as arc42 summary decisions, backlog/open decisions or historical notes. It must not renumber them into ADRs without an explicit ADR decision. |
| C-002 | Documentation roots | The workflow target makes `docs/arc42/` authoritative for architecture, ADR, requirement, conflict-analysis and report outputs. The repository still contains historical or authoritative-looking architecture documents under `docs/arc42/08-crosscutting-concepts/architecture-source-maps/` and ADR files under `docs/adr/`. | Later slices must move, mirror or replace authoritative content under arc42 and leave only verified pointer stubs when compatibility requires them. |
| C-003 | Flat arc42 chapters versus target subdirectories | Current arc42 navigation uses flat files such as `docs/arc42/05-building-block-view.md` and `docs/arc42/09-architecture-decisions.md`. The workflow target uses chapter subdirectories such as `docs/arc42/09-architecture-decisions/adr/`. | Later slices must preserve current flat files until a verified layout transition exists. They may add chapter subdirectories for workflow outputs but must not silently orphan existing flat chapter content. |
| C-004 | Historical predecessor names | ADR-0017 and architecture documents preserve predecessor/current-state names while ADR-0022 retires legacy modular-monolith source trees as active implementation source. | Later consolidation must label predecessor names as historical, current-state, migration or rollback evidence. It must not treat them as active target aliases. |
| C-005 | Runtime persistence baseline | ADR-0023 is accepted for tests only and superseded for runtime by ADR-0024. Some related documents still mention historical H2 context and repository-source persistence boundaries. | Later consolidation must keep H2 as deterministic adapter test/direct fixture scope only and PostgreSQL as repository-source runtime metadata persistence. |
| C-006 | Runtime readiness claims | Target microservice documents describe service roots and implementation evidence but also state that production readiness, Docker Swarm and Kubernetes readiness are not proven. | Later consolidation must keep planned or target architecture separate from implemented runtime readiness evidence. |
| C-007 | Logging and observability source-tree history | ADR-0005, ADR-0008 and ADR-0022 together preserve historical logging/observability decisions while forbidding reintroduction of shared Java logging/runtime modules. | Later consolidation must keep diagnostics-as-not-evidence and service-local diagnostics active while marking retired source trees as historical. |

## Detailed Findings

### C-001 - Mixed Decision Index

`docs/arc42/09-architecture-decisions.md` contains accepted `AD-001` through
`AD-006` entries and `ADR-0005` through `ADR-0024` entries in the same accepted
decision table.

The verified ADR inventory found concrete ADR files only for `ADR-0001` through
`ADR-0024`. Therefore the `AD-*` entries must not be treated as numbered ADR
files, and they must not be silently converted into ADR numbers.

Required later action:

- classify `AD-*` entries as arc42 summary decisions, open-decision history or
  backlog candidates;
- keep ADR numbering stable;
- use `ADR-0025` only if a later reviewed architecture decision is approved.

### C-002 - Multiple Documentation Roots

The ADR Baseline Consolidation workflow requires future architecture output to
live under `docs/arc42/`. The repository still has:

```text
docs/adr/
docs/arc42/08-crosscutting-concepts/architecture-source-maps/
```

`docs/adr/` contains the verified ADR source records. `docs/arc42/08-crosscutting-concepts/architecture-source-maps/`
contains service-boundary and target architecture material that reads as active
architecture guidance.

Required later action:

- treat existing `docs/adr/**` and `docs/arc42/08-crosscutting-concepts/architecture-source-maps/**` as verified source
  inputs during consolidation;
- move or mirror authoritative content into arc42 only in approved slices;
- leave compatibility stubs only when a verified repository rule requires
  them;
- never duplicate architecture content in pointer stubs.

### C-003 - Flat arc42 Files Versus Target Subdirectories

Current arc42 documentation is organized as flat chapter files, for example:

```text
docs/arc42/05-building-block-view.md
docs/arc42/09-architecture-decisions.md
```

The workflow target adds output subdirectories, for example:

```text
docs/arc42/09-architecture-decisions/inventory/
docs/arc42/09-architecture-decisions/conflicts/
docs/arc42/09-architecture-decisions/adr/
docs/arc42/09-architecture-decisions/reports/
```

These shapes can coexist during consolidation, but the workflow must not imply
that current flat chapter files are obsolete unless a later slice explicitly
updates navigation and compatibility.

Required later action:

- preserve flat chapter files as current chapter entrypoints until navigation
  is updated;
- add subdirectory outputs under those chapters;
- update `docs/arc42/README.md` only after the chapter layout is verified.

### C-004 - Historical And Target Service Names

ADR-0017 defines the FA-MSA-001 target service landscape and includes a table
of superseded current or predecessor names. ADR-0022 retires legacy
modular-monolith source trees as implementation source. Architecture documents
still preserve predecessor names as current-state, migration, rollback or
historical evidence.

Required later action:

- keep target services and predecessor names separate;
- avoid compatibility-alias language unless a contract-governance decision
  explicitly requires it;
- preserve rollback/provenance vocabulary as historical evidence only.

### C-005 - H2 And PostgreSQL Boundary

ADR-0023 states that H2 is accepted for tests only and superseded for runtime
by ADR-0024. ADR-0024 states that PostgreSQL is service-owned metadata storage
for repository-source workspace state and that Liquibase owns schema creation
and evolution for that repository-source schema.

Required later action:

- state PostgreSQL as runtime persistence only for the bounded
  repository-source workspace metadata scope;
- state H2 as deterministic adapter test and direct fixture scope only;
- keep broader canonical Analytics persistence open unless another ADR closes
  it;
- do not introduce runtime fallback language.

### C-006 - Target Architecture Versus Runtime Readiness

`docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md` states that the
target service landscape is not a production-readiness claim. Service-boundary
documentation similarly distinguishes implementation evidence and migration
inputs from completed independent deployability.

Required later action:

- label target and planned behavior explicitly;
- do not claim independent build, start, healthcheck, container, Docker Swarm
  or Kubernetes readiness unless verified by repository evidence and recorded
  commands;
- keep implementation evidence separate from production-readiness claims.

### C-007 - Logging And Observability History

ADR-0005 and ADR-0008 preserve logging and observability decisions from the
predecessor modular-monolith context. ADR-0022 retires the related source trees
as active implementation source. `docs/arc42/05-building-block-view.md`
already states that active services must use service-local diagnostics or
deployment/configuration material rather than shared Java logging modules.

Required later action:

- keep diagnostics-as-not-evidence active;
- keep service-local diagnostics and `observability-stack` deployment material
  separate;
- do not reintroduce shared Java logging or observability runtime modules
  between independently deployable services.

## S04/S05 Input Notes

The consolidated ADR slice must not invent a resolved baseline for the
conflicts above. It may reference this analysis and then decide only the
verified consolidation baseline approved by that slice.

The ADR placement slice must preserve historical ADR intent. If an ADR is
moved or mirrored into arc42, the source history and status must remain
traceable.

## Stop Conditions For Later Slices

Stop if a later slice would:

- renumber an existing ADR;
- convert an `AD-*` row into an ADR without explicit ADR approval;
- treat predecessor names as active target aliases;
- present target architecture as production-ready runtime evidence;
- describe H2 as runtime, Docker or readiness fallback;
- duplicate authoritative architecture content in pointer stubs;
- orphan existing flat arc42 chapter content without verified navigation
  updates.
