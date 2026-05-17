# Microservices Ecosystem Conversion Prompt

This prompt is retained only as workflow context for future service-split planning.

It is not active implementation authorization for this branch.

For this branch, the active target is agent workflow governance reconstruction:

- `skills update`
- `workflow create`
- `workflow execute`
- slice checkpoint push
- `push auto` only for `skills-agents`

If a future workflow revisits microservices ecosystem conversion, it must run `workflow create` first and must prove:

- service boundary
- data ownership
- contract impact
- test impact
- rollback or strangler strategy
- no shared Java implementation modules
- checked `docs/workflow/workflow.md`
- checked or updated arc42 documentation

This file must not be used to justify product implementation changes in the current branch.
