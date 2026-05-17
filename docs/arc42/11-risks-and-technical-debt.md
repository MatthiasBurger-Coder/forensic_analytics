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
| Governance drift | AGENTS.md, process docs, workflow docs, arc42, ADRs and skill registry may diverge | Documentation Governance and explicit process-strand ownership |
| push auto too broad | `push auto` might be interpreted as permission to publish implementation changes | Restrict `push auto` to `skills-agents` and block product implementation scopes |
| checkpoint push confused with push auto | Slice checkpoint push might be mistaken for PR merge and cleanup authority | Document slice checkpoint push as workflow-execute-only branch push with no PR merge or cleanup |
| unbounded governance loop | Automatic clarification or correction loops may keep cycling without a decision | Cap automatic governance loops at `maxRetries = 3` and escalate to Root Architect |

## 11.1 Technical Debt Candidates

- Initial event import may be JSONL-only.
- First graph projection may be simplified.
- Vector DB integration may be postponed.
- UI may start as minimal read-only analysis view.
- Joern mapping may initially support only selected node types.
- Governance diagrams and skill registry need periodic review as agent definitions evolve.

## 11.2 Agent Governance Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Agent mixes process strands | Wrong files changed or unsafe publication | Explicit command routing and strand-scoped file permissions |
| Requirement is ambiguous | Wrong workflow or implementation | Requirement Clarification Loop and blocking questions |
| Clarification loop is exhausted | Workflow create may remain unresolved | Stop after `maxRetries = 3` and escalate to Root Architect |
| arc42 is not updated | Architecture drift | `workflow create` requires checked or updated arc42 |
| Slice work is lost locally | Rework after machine failure | Slice checkpoint commit and push after every successful slice |
| Quality failure has generic ownership | Wrong role fixes the wrong cause or retries indefinitely | Typed Error Router assigns `ARCH_VIOLATION`, `BUILD_FAILURE`, `TEST_FAILURE`, `DOC_GOVERNANCE_FAILURE`, `LOCK_CONFLICT` or `UNKNOWN_FAILURE` and caps retries at `maxRetries = 3` |
| `push auto` used too broadly | Product implementation may be merged accidentally | Push Auto Guard restricts it to `skills-agents` |
