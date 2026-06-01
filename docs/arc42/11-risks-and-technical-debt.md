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
| unclassified workflow slice executes | A slice without a verified owner might change files outside the checked workflow | Route `none of the above` to `S3_UNCLASSIFIED` and Root Architect escalation |
| generic quality failure retry | A build, test, architecture, documentation or lock failure might be retried by the wrong role | Route failures through the Typed Error Router before retry |
| rollback path is unclear | Failed quality gates or push failures might lead to unsafe history rewriting | Use `CP_ROLLBACK` as a decision node and forbid blind `git reset --hard` |
| flowchart becomes unreviewable | Large diagrams can hide dead nodes, missing paths and wrong backward jumps | Maintain Level 1 overview and Level 2 subgraphs in `docs/governance/workflow/` and audit them through `flowchart-integrity-auditor` |
| governance role remains bootstrap-only | Root Architect is mapped but lacks a dedicated role artifact | Keep bootstrap owners documented and create dedicated artifacts only through a future governance slice |
| legacy source-tree rollback is misunderstood | Retired `forensic-analytics-*` source trees might be treated as current rollback/runtime units | Use ADR-0022 and the S05 checkpoint as the rollback boundary; restore by reverting the checkpoint commit only when verified dependency evidence requires it |
| FA-MVP-0001 requirement source is separated from `docs/epics` | A later reader may miss that the user-provided requirement is the accepted source for this workflow | Keep the Three Amigos decision record and execution report linked to FA-MVP-0001 and add a dedicated EPIC artifact only through a later requirement-governance slice |
| Repository-source PostgreSQL is mistaken for shared Analytics persistence | Operators or later slices might treat the repository-source PostgreSQL schema as a cross-service database or canonical evidence store | ADR-0024 bounds PostgreSQL to repository-source workspace metadata only; ADR-0013 still forbids direct cross-service database access and OD-001 remains open for broader Analytics persistence |
| Docker-local Compose config is mistaken for runtime readiness | A valid Compose model might be read as proof of image startup, health checks, Swarm or Kubernetes readiness | S09 records Compose model validation only; later deployment slices must verify image builds, startup, health probes, cleanup, Swarm stacks or Kubernetes manifests before claiming readiness |

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
| `push auto` used too broadly | Product implementation may be merged accidentally | `S1_PUSH_ELIGIBILITY_GUARD` restricts it to `skills-agents` |
| `workflow execute` rewrites workflow scope | Execution could hide requirement drift by changing the workflow during execution | R10 forbids automatic backward jumps to `workflow create`; unresolved scope conflicts stop and escalate |
