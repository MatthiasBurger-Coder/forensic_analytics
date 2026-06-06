# Architecture Source Maps

## Root Classification

`docs/architecture/` is a historical and current architecture source-map root.
It contains service-boundary maps, migration maps, current-state records and
legacy-reference classifications that workflows and arc42 governance still use
as source evidence.

Authoritative arc42 architecture outputs belong under
[`../arc42/`](../arc42/). This root must not be treated as newer arc42 output
when an equivalent checked document exists under `docs/arc42/**`.

Do not move this directory wholesale or replace it with pointer stubs without a
dedicated architecture-document consolidation workflow. Such a workflow must
update references, classify historical evidence, preserve ADR traceability and
avoid claiming implementation or deployment readiness from documentation moves
alone.
