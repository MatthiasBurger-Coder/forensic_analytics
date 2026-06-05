# ADR-0020: Agent Governance Process Strands

## Status

Accepted

## Context

Forensic Analytics uses Codex and repository-specific agents, skills and prompts to perform architecture-sensitive work.

Without a strict process model, agent work can mix skill maintenance, requirement clarification, implementation, documentation and publication behavior.

## Decision

We define exactly three agent process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

We define one explicit command for skills and agents:

```text
skills update
```

We define two workflow commands:

```text
workflow create
workflow execute
```

We separate publication behavior into:

1. Slice checkpoint push
2. `push`
3. `push auto`

`push auto` is restricted to the `skills-agents` strand.

`workflow execute` performs a slice checkpoint commit and pushes the workflow branch after every successful slice.

Documentation Governance runs inside every active strand. It is mandatory but not a fourth strand.

## Consequences

- Agent work is easier to route.
- Requirements are clarified before workflow authoring.
- arc42 synchronization becomes mandatory for workflow creation.
- Implementation is slice-based and recoverable.
- `push auto` cannot accidentally publish product implementation.
- Documentation Governance becomes part of every strand.
