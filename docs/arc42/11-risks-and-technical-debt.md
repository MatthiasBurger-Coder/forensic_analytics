# 11. Risks and Technical Debt

| Risk | Description | Mitigation |
|---|---|---|
| Runtime overhead | Too many Byteman rules can slow down the target application | Rule planner, sampling, profiles, selective instrumentation |
| Sensitive trace data | Parameters may contain personal data or secrets | Redaction, allowlisting, hashing, retention |
| Wrong graph correlation | JavaParser and Joern data may be mapped incorrectly | Confidence levels, validation, ambiguity reporting |
| LLM hallucination | LLM may suggest wrong causes or fixes | Evidence-only prompting, tests, review gates |
| Large graphs | UI may become overloaded for large codebases | Layering, filtering, slicing, lazy loading |
| Unsafe automatic fixes | Patches may have unexpected side effects | Regression tests, risk classifier, human review |
| Toolchain complexity | Joern, Byteman, Graph DB, Vector DB and LLM increase complexity | Hexagonal ports, modular adapters, MVP slicing |
| Governance drift | Skills, roles, prompts and workflow rules can diverge over time | `skills-agents` integrity checks, registry maintenance and documentation governance |
| Premature workflow authoring | A workflow could be finalized before blocking requirement questions are answered | Requirement Clarification Loop blocks final checked `docs/workflow/workflow.md` and release for `workflow execute` |
| Stale workflow artifacts | Historical workflow sidecars can be mistaken for checked execution inputs | `workflow execute` starts only from checked `docs/workflow/workflow.md` and checked arc42 documentation |
| Unsafe push automation | `push auto` could publish unrelated or product implementation changes if not guarded | `push auto` is restricted to `skills-agents` and blocks product implementation files |
| Portable `.codex` leakage | Project-specific rules can accidentally enter reusable `.codex` templates | Keep project-specific governance in root `AGENTS.md`, `.agents/**` and `docs/**`; document exceptions |
| Governance-only task touches product code | Process changes could accidentally modify backend, frontend, Docker/runtime or analytics implementation | Changed-file guards and final diff review block product implementation files |

## 11.1 Technical Debt Candidates

- Initial event import may be JSONL-only.
- First graph projection may be simplified.
- Vector DB integration may be postponed.
- UI may start as minimal read-only analysis view.
- Joern mapping may initially support only selected node types.
