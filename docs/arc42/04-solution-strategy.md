# 4. Solution Strategy

## 4.1 Strategy Overview

The Forensics Platform separates producer-side triggers from central server-side analysis. Build plugins provide repository, branch, commit, build and runtime-launch context. The central platform checks out repositories, runs analysis, normalizes evidence, persists facts, correlates runtime data and produces generated artifacts such as BTM files.

## 4.2 Core Strategy

1. Use Gradle and Maven plugins only as request producers and runtime-binding adapters.
2. Normalize all inputs into a canonical analysis model.
3. Use stable IDs for classes, methods, callsites, branches, rules and runtime events.
4. Generate Byteman/BTM files server-side from an explicit instrumentation plan.
5. Let the plugin bind server-generated BTM files through the runtime agent when debugging requires instrumentation.
6. Collect runtime events with stable `ruleId` and `methodKey` references.
7. Build incidents from exception events.
8. Reconstruct replay timelines by correlation ID, trace ID, thread ID and sequence.
9. Build graph and vector projections from the canonical model.
10. Provide curated evidence packages to the LLM.
11. Keep repair automation gated by tests, quality gates and review.

## 4.3 MVP Strategy

The MVP focuses on read-only analysis:

- Static fact import
- Canonical model persistence
- Joern result import or attachment
- Server-side Byteman/BTM generation with stable rule IDs
- JSONL runtime event import
- Exception incident creation
- CorrelationID-based replay
- Simple graph projection
- LLM root-cause explanation without code modification

## 4.4 Non-MVP Scope

The following items are explicitly postponed:

- Automated patch generation
- Automated pull request creation
- Automated staging or production deployment
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers
