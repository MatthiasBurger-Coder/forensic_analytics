# Level 1 Governance Overview

This overview shows the global command and governance flow. It intentionally
keeps S1, S2 and S3 separate and shows only the high-level paths that decide
whether work can continue, stop, publish or escalate.

```mermaid
flowchart TD
  ROOT["ROOT: Forensic Analytics Governance"]
  CMD["Commands"]
  S1["S1: skills-agents"]
  S2["S2: workflow create"]
  S3["S3: workflow execute"]
  HB["Hard Boundaries R1-R11"]
  DOCROOT["DOCROOT: Global Documentation Governance"]
  QG["QG: Quality Gates"]
  CP["CP: Commit / Checkpoint / Rollback"]
  PUB["PUB: Publication Modes"]
  RA["ROOT_ARCHITECT: Escalation"]
  STOP["STOP and report"]
  DONE["DONE: Verified outcome"]

  ROOT --> CMD
  ROOT --> HB
  ROOT --> DOCROOT

  CMD -->|"skills update"| S1
  CMD -->|"workflow create"| S2
  CMD -->|"workflow execute"| S3
  CMD -->|"push or push auto"| PUB

  HB --> S1
  HB --> S2
  HB --> S3

  DOCROOT --> S1
  DOCROOT --> S2
  DOCROOT --> S3

  S1 -->|"eligible publication path"| PUB
  S2 -->|"checked workflow release"| S3
  S3 --> QG
  QG --> CP
  CP --> PUB
  PUB --> DONE

  S1 -->|"blocked"| STOP
  S2 -->|"blocked"| STOP
  S3 -->|"blocked"| STOP
  QG -->|"failed after routing"| STOP
  CP -->|"rollback decision required"| RA
  STOP --> RA
```

## Level 1 Checks

- Flowchart integrity is audited through
  `.agents/skills/flowchart-integrity-auditor/SKILL.md`.
- ROOT, commands, S1, S2, S3, hard boundaries, publication modes and global
  governance nodes are visible.
- S1, S2 and S3 remain separate process strands.
- `workflow execute` does not jump back to `workflow create`.
- Publication is a governed outcome path, not a hidden side effect.
- STOP paths end in report or Root Architect escalation.
