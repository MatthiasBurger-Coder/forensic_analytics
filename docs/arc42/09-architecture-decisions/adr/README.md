# arc42 Architecture Decision Records

This directory is the authoritative arc42 ADR chapter for the ADR Baseline
Consolidation workflow.

## S05 Placement Result

During S05, `ADR-0001` through `ADR-0024` were mirrored byte-for-byte from
`docs/adr/` into this directory. Their status, numbering, title and body were
not rewritten.

`ADR-0025` was created directly in this directory as the consolidated
architecture baseline for the verified ADR set.

## Governance Notes

- New authoritative ADR output for this workflow belongs in this directory.
- `AD-*` rows in `docs/arc42/09-architecture-decisions.md` are arc42 decision
  index entries, not numbered ADR files.
- Existing `docs/adr/**` files remain repository history and compatibility
  input until a separate approved slice changes that location.
- Pointer stubs must not duplicate architecture content.
