# Slice Dependency Map

## Dependencies

```mermaid
flowchart TD
  S01["Slice 01: AGENTS.md process strands"]
  S02["Slice 02: docs/process skills update"]
  S03["Slice 03: workflow create gate"]
  S04["Slice 04: workflow execute checkpoint"]
  S05["Slice 05: Git publication governance"]
  S06["Slice 06: organigramm and skill registry"]
  S07["Slice 07: arc42 and ADR governance"]
  S08["Slice 08: active workflow consistency"]
  S09["Slice 09: Codex agents and prompts"]
  S10["Slice 10: final validation"]

  S01 --> S02
  S02 --> S03
  S03 --> S04
  S04 --> S05
  S05 --> S06
  S06 --> S07
  S07 --> S08
  S08 --> S09
  S09 --> S10
```

## Execution Rule

Each slice must complete its diff review, checkpoint commit and push before the next slice starts.

The checkpoint push target is:

```text
origin/architecture/workflow-align-agent-workflow-strands-20260517
```

No slice may push to `main`, create or merge a PR, run `push auto`, clean up branches or force-push.
