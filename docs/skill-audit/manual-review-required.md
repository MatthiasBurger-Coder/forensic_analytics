# Manual Review Required

## Summary

The following items were not changed automatically because they are outside the requested skill/workplan consolidation surface, appear to be historical or ADR-adjacent documentation, or would require a dedicated architecture/documentation decision. They should be reviewed before the next broad documentation alignment slice.

| Item | Affected file | What was found | Why automatic correction was not made | Suggested decision |
|---|---|---|---|---|
| ADR-0001 still allows plugins to collect raw analysis facts. | `docs/adr/ADR-0001-plugins-are-producers.md` | The context says plugins can collect build context, source roots, dependencies and raw analysis facts. | The current platform direction narrows the plugin to repository, branch, commit, build and context data. Changing an accepted ADR should be done by a new ADR or an explicit ADR amendment. | Create a new ADR clarifying that current plugin-to-analytics handoff is request/context only and parser/Joern/BTM execution belongs in Analytics. |
| arc42 technical context says the Gradle plugin provides AST facts and rule bindings. | `docs/arc42/03-system-scope-and-context.md` | The technical context assigns AST facts and rule bindings to the Gradle plugin. | This conflicts with the current phase, but may describe a prior target architecture. It should be updated together with ADR-0001 and the ingestion docs. | Update the context model so plugins provide repository/build context and Analytics owns parser, Joern and BTM capabilities. |
| Existing `docs/README.md` describes local repository analysis with JavaParser and Joern. | `docs/README.md` | The file documents current local analysis modules and gRPC ingestion as implemented surfaces. | The codebase currently contains JavaParser, Joern and local CLI modules, so this is not clearly false. The new workplan only states these are not part of the next workspace/gRPC implementation phase. | Keep implemented-module documentation, but add a future slice clarifying the transition from local analysis to server-side workspace/job orchestration. |
| Byteman/BTM appears in older architecture docs. | `docs/arc42/*`, `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md` | Several docs mention Byteman rule generation. | The current user request forbids BTM generation in this phase, but does not necessarily remove BTM from the long-term platform vision. | Add phase labels so BTM remains future analytics-side work and is never planned as current plugin work. |

## Missing Paths

The following requested inspection paths were absent:

- root `README.md`
- root `workflow.md`
- `docs/workflow/`

No substitute content was invented for these paths.
