# 2. Architecture Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java 25 | The platform baseline is Java 25. |
| Gradle 9.4.0 | Gradle integration must be compatible with Gradle 9.4.0. |
| JUnit 6 | Automated tests use the JUnit 6 baseline. |
| Maven support | Maven must be supported as a separate plugin adapter. |
| Hexagonal Architecture | Core domain logic must be independent from frameworks and external tools. |
| Plugins as adapters | Gradle and Maven plugins are producers of facts, not the central platform. |
| Joern as adapter | Joern integration must be encapsulated behind a port. |
| Byteman integration | Byteman rules are generated from the analysis model and runtime planning. |

## 2.2 Product Constraints

- The MVP does not include autonomous code changes.
- The MVP does not include automatic pull request creation.
- The MVP does not include production deployment automation.
- The first platform version focuses on JSONL runtime event import.
- Graph DB and Vector DB products are not finally selected.

## 2.3 Security Constraints

- Runtime values must be treated as sensitive by default.
- Redaction must happen before persistence into graph or vector projections.
- Secrets must not be indexed in a Vector DB.
- Runtime data access must be auditable.

## 2.4 Architectural Guardrails

- Canonical model first.
- Graph DB and Vector DB are projections, not the source of truth.
- Ambiguous mappings must be reported, not silently accepted.
- LLM output must be evidence-based.
- Automated repair must be gated by tests, quality gates and human review.
