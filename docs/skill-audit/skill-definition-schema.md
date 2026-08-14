# Canonical `.agents` Skill Definition Schema

## Status and authority

Schema identifier: `skill-definition-v1`

S02 review status: `S02_REVIEW_PENDING`

This file is the one canonical schema authority for the structural definition
of project skills at `.agents/skills/**/SKILL.md`. It does not replace the
following higher or adjacent authorities:

1. Root `AGENTS.md` remains authoritative for repository behavior, safety,
   architecture, evidence integrity and stop conditions.
2. `QUALITY.md` remains authoritative for verification commands and quality
   gates.
3. `.agents/AGENTS.md` remains authoritative for Codex discoverability and
   requires a directory entry point with YAML frontmatter fields `name` and
   `description`.
4. Accepted ADR-0015 and the active GOV-02/GOV-04 workflow define the
   governance requirement for ownership, conflict auditing and migration.
5. Each individual `SKILL.md` remains authoritative for that skill's actual
   meaning. This schema must not rewrite responsibility-specific semantics.
6. `docs/skill-audit/skill-inventory.md`, `skill-registry.md` and
   `skill-registry.json` are derived audit or routing caches. They are never
   schema or skill-behavior authority.

`.codex/skills/**` is a separate reusable-skill namespace. A same-named
project skill and reusable skill is not a duplicate within this schema; the
active Forensic Analytics executor is resolved by the repository workflow
rules.

## Scope and ownership

Schema ownership is singular: Senior Documentation Engineer is the primary
and sole owner. Skill Registry Conflict Auditor is the independent reviewer,
not a co-owner.

| Concern | Owner or authority | Evidence |
|---|---|---|
| Structural schema | Senior Documentation Engineer | This file; S02 workflow owner and ADR-0015; independent review by Skill Registry Conflict Auditor |
| Skill meaning and semantic rules | The owning `.agents/skills/**/SKILL.md` | Individual skill file |
| Architecture authority | Senior System Architect | Root `AGENTS.md`, ADRs and routing rules |
| Requirement and workflow traceability | Senior Requirement Engineer / active workflow owner | `.agents/roles/senior-requirement-engineer/SKILL.md`, `docs/workflow/workflow.md` |
| Quality authority | Senior Tester / Quality Gate Orchestrator | `QUALITY.md` and quality skills |
| Registry and inventory cache | Skill Registry Conflict Auditor / Senior Documentation Engineer | `docs/skill-audit/**` |
| S03 validator and migration | S03 owner: Senior Documentation Engineer / Skill Registry Conflict Auditor | S03 metadata in `docs/workflow/workflow.md` |

`owner`, `strand`, and `scope` are not accepted frontmatter fields in
`skill-definition-v1`. Decision ownership is expressed semantically in the
`Authority` section and operational ownership is recorded by the registry or
workflow. A missing or ambiguous owner is a governance finding, not a value to
infer from the directory name.

## Entry point and machine-checkable contract

Every project skill must be exactly one regular file at:

```text
.agents/skills/<directory-name>/SKILL.md
```

The following checks are machine-checkable and must be deterministic:

| Requirement | Rule | Current result |
|---|---|---:|
| Entry point | The directory exists and contains regular `SKILL.md`. | 77/77 |
| Frontmatter boundaries | YAML frontmatter is delimited by `---` before the body. | 77/77 |
| `name` | Required, non-empty YAML scalar; unique within `.agents/skills`; equal to the directory basename. | 77/77; 0 duplicates |
| `description` | Required, non-empty YAML scalar. Quoted and YAML block scalar forms are allowed. | 77/77 |
| Frontmatter keys | Only `name` and `description` are currently defined by this schema. Unknown keys are a validation finding. | 77/77 |
| Body presence | A body follows the frontmatter. | 77/77 |

There are no frontmatter aliases. `title`, `summary`, a filename, or a
heading cannot substitute for `name` or `description`. Frontmatter validity
does not prove that the body is semantically complete.

These checks establish only machine-checkable structure. They must not be used
to claim that a skill's mission, responsibilities, authority, boundaries,
inputs, outputs, collaboration or STOP behavior is semantically complete.

## Semantic body contract

The body contract has eight canonical concepts: Mission, Responsibilities,
Authority, Forbidden Scope, Inputs, Outputs, Collaboration Rules and STOP
Rules. Every new or migrated skill must define all eight concepts. An existing
legacy skill may remain temporarily incomplete only through an explicit typed
exception that is semantically justified; omission is never silent. Body
sections are semantic contracts. A machine check can find a heading or alias,
but human review must confirm that its content really fulfills the concept.
The canonical headings below are preferred for new or migrated skills.

| Concept | Canonical heading | Requirement | Accepted legacy aliases | Review requirement |
|---|---|---|---|---|
| Mission | `Mission` | Required for every skill. | `Purpose`, `Description`, `Goal`, `Role` | Confirm that the section states the skill's mission, not only a topic label. |
| Responsibilities | `Responsibilities` | Required for every skill. | `Instructions`, `Practices`, `Core Rules`, `Mandatory Behavior`, `Review Tasks` | Confirm that the section defines the skill's work and does not silently acquire another owner's decisions. |
| Authority | `Authority` | Required for every new or migrated skill. An existing legacy skill may use a typed `NOT_APPLICABLE` exception only when semantic review proves that authority is not applicable. | None | The section must state decisions owned, limits and escalation owner. `Role` is not an authority alias. |
| Forbidden scope | `Forbidden Scope` | Required for every skill. | `Forbidden`, `Boundaries`, `Forbidden Actions` | Confirm that forbidden behavior is explicit and does not weaken root `AGENTS.md`, ADRs or microservice rules. |
| Inputs | `Inputs` | Required for every skill. | `Expected Inputs`, `Required Inputs`, `Required References`, `Reference Files` | Confirm exact source paths, contracts or evidence required by the skill. |
| Outputs | `Outputs` | Required for every skill. | `Expected Outputs`, `Expected Output`, `Output Format`, `Decision Contract`, `Result Contract` | Confirm artifacts, decisions or reports produced. `Verification` alone is not an output alias. |
| Collaboration | `Collaboration Rules` | Required for every new or migrated skill. An existing legacy skill may use a typed `NOT_APPLICABLE` exception only when semantic review proves that collaboration is not applicable. | `Related Skills`, `Required Skills` | Confirm direction, consultation, non-overlap and handoff ownership. A self-contained advisory skill requires a typed exception. |
| STOP behavior | `STOP Rules` | Required for every skill. | `Stop Conditions`, `Stop Rules` | Confirm exact blocking conditions, escalation owner and no-guessing behavior. |

Supplementary sections such as `Core Rule`, `Practices`, `Verification`,
profiles, decision records, platform guidance and phase details are optional.
They may refine a canonical concept, but they may not silently replace a
required concept unless the accepted alias is semantically verified or a typed
exception is recorded.

### Alias and semantic review rule

Aliases are machine-recognizable candidates, not automatic equivalences. A
human reviewer must inspect the section content when an alias is ambiguous,
can describe more than one concept, or is repeated. For example, `Description`
may be a mission or only metadata, `Role` does not establish authority,
`Boundaries` must be checked for forbidden scope, and `Verification` is not an
Outputs alias. `Related Skills` and `Required Skills` must be checked for
actual collaboration or dependency semantics. Ambiguous forms remain typed
exceptions until the reviewer records the interpretation and next action; they
must not be normalized silently.

The canonical body contract is intentionally stricter than the currently
implemented frontmatter contract. S02 documents the draft contract; S03 owns
the validator and migration. No current skill body is to be mechanically
rewritten as part of S02.

## Typed exceptions

An exception preserves an observed existing legacy skill until S03 either
migrates it or records an accepted non-applicability decision. Exceptions are
not aliases, and they do not make a missing required section disappear. New or
migrated skills must define all eight concepts and cannot use an exception to
waive that requirement.

Each exception record must contain all fields below:

```yaml
type: LEGACY_ALIAS # enum: LEGACY_ALIAS | LEGACY_MISSING_SECTION | NOT_APPLICABLE | DUPLICATE_SECTION | UNRESOLVED
skill: "<exact frontmatter name>"
path: "<exact repository path>"
concept: "<canonical concept affected>"
observed: "<exact heading, shape or missing condition>"
justification: "<evidence-based reason>"
owner: "<exact role or skill responsible for resolution>"
reviewer: "<exact independent reviewer role or skill>"
remediation: "<migration, review or decision required>"
next_action: "<concrete follow-up or next action>"
status: OPEN # enum: OPEN | RESOLVED
trace: "<workflow slice, ADR, issue or review reference>"
```

Field typing is part of the exception contract: `type` and `status` are
enumerations; every other field, including `reviewer` and `next_action`, is a
required non-empty string. `reviewer` identifies the independent review
owner, while `next_action` records the concrete follow-up needed to resolve
or re-review the exception.

Exception rules:

- `LEGACY_ALIAS` is allowed only when human review confirms that the alias
  preserves the concept.
- `LEGACY_MISSING_SECTION` is required when a required concept is absent from
  an existing legacy skill; it does not make the absence valid for a new or
  migrated skill.
- `NOT_APPLICABLE` is allowed only for an existing legacy skill when semantic
  review proves that the concept does not apply. It is never a waiver for a
  new or migrated skill and must still record the operational owner and
  escalation path.
- `DUPLICATE_SECTION` records repeated or ambiguous headings; it is never
  silently collapsed.
- `UNRESOLVED` is blocking and cannot be reported as ready.
- An exception without a verified `reviewer` or `next_action` remains
  `UNRESOLVED`.
- No exception may remove, weaken or replace a STOP rule or forbidden scope.

## Current repository compatibility snapshot

### Machine-checkable scan evidence

The following structural and heading-presence results are implemented and
verified now. Heading or alias presence is scan evidence only; it is not
semantic conformance.

- 77/77 project skill entry points satisfy `.agents/AGENTS.md` discovery.
- 77/77 contain required `name` and `description` frontmatter.
- All 77 names are unique, match their directory basenames and use only the
  defined frontmatter keys.

Coverage counts use exact canonical headings and accepted aliases at Markdown
heading levels 1 through 6, matched by `^#{1,6}[[:space:]]+...$`; therefore
the `Forbidden scope` count is reproducibly 33/77, while a level-2-only scan
produces 32.

| Recognized semantic heading | Skills containing one | Evidence interpretation |
|---|---:|---|
| Mission or accepted alias | 77/77 | Machine-detectable presence; semantic confirmation remains required. |
| Responsibilities or accepted alias | 66/77 | 11 skills need an explicit migration or exception record. |
| Authority | 13/77 | Presence only; every new or migrated skill must define it, while legacy gaps require migration or a typed exception. |
| Forbidden scope or accepted alias | 33/77 | 44 skills need an explicit migration or exception record. |
| Inputs or accepted alias | 47/77 | 30 skills need an explicit migration or exception record. |
| Outputs or accepted alias | 43/77 | `Verification` was not counted as an output. 34 skills need an explicit migration or exception record. |
| Collaboration or accepted alias | 18/77 | Presence only; every new or migrated skill must define it, while legacy gaps require migration or a typed exception. |
| STOP rules or accepted alias | 52/77 | 25 skills need an explicit migration or exception record. |

### Semantic human review required

S03 owns semantic validation and migration. It must confirm whether each
recognized heading or alias fulfills its canonical concept, preserve missing
or ambiguous meaning as typed exceptions, and never infer runtime or
responsibility semantics from heading presence alone.

S03 owns any body-shape classification. S02 claims only the verified duplicate
`Required Skills` finding below; no other body-shape classification is claimed.

| Duplicate heading | Count | Path | Required handling |
|---|---:|---|---|
| `Required Skills` | 1 | `.agents/skills/microservice-senior-expert/SKILL.md` | Record `DUPLICATE_SECTION`; do not merge or reinterpret automatically. |

## Workflow and ADR alignment

S02 is the `GOV-04: Canonical Skill Definition Schema` slice in
`docs/workflow/workflow.md`. Its verified primary owner is Senior
Documentation Engineer. Skill Registry Conflict Auditor is the independent
schema reviewer; Senior System Architect, Senior Requirement Engineer and
Senior Tester remain the workflow's secondary reviewers. Its only contract
lock is `skill-definition-schema`; its required local quality command is the
minimum command from `QUALITY.md`.

ADR-0015 requires mission, responsibilities, authority, forbidden scope,
inputs, outputs, collaboration rules and STOP rules for every new skill. The
canonical schema applies the same all-eight-concepts requirement to migrated
skills. Existing legacy skills may use an explicit typed `NOT_APPLICABLE`
exception only when semantic review justifies it; otherwise the missing
concept must be recorded as a typed exception, never silently omitted. No
product architecture, runtime, deployment or evidence behavior changes; arc42
is checked and no new ADR is required. The JSON registry/cache is not
refreshed in S02: S03 supplies validator and migration evidence, and S04 owns
the source-derived registry/cache refresh.
