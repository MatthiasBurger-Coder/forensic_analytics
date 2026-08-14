# Agent Documentation

This directory documents the repository agent, skill and workflow governance model.

## Root Classification

`docs/agents/` is a process-governance root, not a product-architecture
chapter. Agent organigramm, governance and skill-registry material stays here
unless a `skills-agents` governance workflow changes process ownership. The
arc42 documentation may reference this model, but it must not absorb these
process-control files by default.

## Documents

- `organigramm.md` - current agent and process-strand organigramm
- `agent-governance.md` - explanation of the three-strand governance model
- `skill-registry.md` - registry of skills, agents, roles and process ownership

The canonical two-level Governance Flowchart V2 diagrams live in
[`../governance/workflow/`](../governance/workflow/).

## `.codex/agents` Portability

GOV-05 classified all 34 callable definitions under `.codex/agents/*.toml`
from their actual descriptions and developer instructions:

| Classification | Count | Copy policy |
|---|---:|---|
| Reusable | 5 | Eligible for a portable `.codex` template |
| Project-specific | 29 | Keep repository-local unless generalized and re-audited |
| Manual review | 0 | No unresolved mixed-reference definition |

The reusable definitions are `implementation_worker`, `quality_reviewer`,
`replay_graph_llm_reviewer`, `repository_explorer` and
`source_analysis_reviewer`. Project-specific definitions contain explicit
Forensic Analytics references, `.agents` skill paths, or repository-specific
workflow, module, contract or deployment boundaries. The JSON registry is the
machine-readable audit record; it does not override the TOML files or root
`AGENTS.md`.
