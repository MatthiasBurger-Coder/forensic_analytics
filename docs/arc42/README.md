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

The three-strand agent and workflow governance model was checked on branch
`architecture/workflow-align-agent-workflow-strands-20260517` on 2026-05-17.

Checked sections:

- 1. Introduction and Goals: no product goal change required.
- 2. Architecture Constraints: updated with agent and workflow governance
  constraints.
- 3. System Scope and Context: no runtime system boundary change required.
- 4. Solution Strategy: updated with repository governance strategy.
- 5. Building Block View: updated with repository governance building blocks.
- 6. Runtime View: updated with repository governance runtime flows.
- 7. Deployment View: no deployment topology change required.
- 8. Crosscutting Concepts: updated with engineering governance and
  documentation synchronization.
- 9. Architecture Decisions: updated with the active workflow and agent
  governance decision set.
- 10. Quality Requirements: updated with governance quality scenarios.
- 11. Risks and Technical Debt: updated with governance drift and push
  automation risks.
- 12. Glossary: updated with governance terms.

Reviewer role: Senior System Architect / arc42 Architecture Governance.
Unresolved drift: existing `docs/workflow/**` sidecars remain historical
supporting material until a later workflow explicitly migrates or archives them.
