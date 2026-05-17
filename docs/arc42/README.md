# arc42 Architecture Documentation – Forensics Platform

This directory contains the arc42-based architecture documentation for the Forensics Platform.

## Source Baseline

This documentation is based on the EPIC:

- Name: Forensics Platform - Exception-Centered Runtime Replay and LLM-Assisted Failure Analysis
- Version: 0.1
- Date: 2026-05-03
- Status: Draft

## Sections

1. [Introduction and Goals](01-introduction-and-goals.md)
2. [Architecture Constraints](02-architecture-constraints.md)
3. [System Scope and Context](03-system-scope-and-context.md)
4. [Solution Strategy](04-solution-strategy.md)
5. [Building Block View](05-building-block-view.md)
6. [Runtime View](06-runtime-view.md)
7. [Deployment View](07-deployment-view.md)
8. [Crosscutting Concepts](08-crosscutting-concepts.md)
9. [Architecture Decisions](09-architecture-decisions.md)
10. [Quality Requirements](10-quality-requirements.md)
11. [Risks and Technical Debt](11-risks-and-technical-debt.md)
12. [Glossary](12-glossary.md)

## Documentation Principle

The EPIC remains the product and requirement baseline. The arc42 documentation transforms this baseline into an architectural structure.

## Governance Check Status

The three-strand agent and workflow governance model was checked on branch `architecture/workflow-align-agent-workflow-strands-20260517`.

Checked sections:

- Introduction and Goals: no product goal change required.
- Architecture Constraints: updated with agent and workflow governance constraints.
- System Scope and Context: no runtime system boundary change required.
- Solution Strategy: updated with repository governance strategy.
- Building Block View: updated with repository governance building blocks.
- Runtime View: updated with repository governance runtime flows.
- Deployment View: no deployment topology change required.
- Crosscutting Concepts: updated with engineering governance and documentation synchronization.
- Architecture Decisions: updated with active workflow and agent governance decisions.
- Quality Requirements: updated with governance quality scenarios.
- Risks and Technical Debt: updated with governance drift, checkpoint and push automation risks.
- Glossary: updated with governance terms.

The checked workflow create end state is:

1. no blocking requirement questions remain,
2. complete checked docs/workflow/workflow.md,
3. checked or updated arc42 documentation,
4. Documentation Governance passed,
5. explicit release for workflow execute.

Reviewer role: Senior System Architect / arc42 Architecture Governance.
