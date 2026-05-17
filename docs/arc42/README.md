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

The three-strand agent and workflow governance model was checked for Governance Flowchart V2 workflow creation on branch `architecture/workflow-governance-flowchart-v2-20260517`.

Checked sections:

- Introduction and Goals: no product goal change required.
- Architecture Constraints: checked for agent and workflow governance constraints.
- System Scope and Context: no runtime system boundary change required.
- Solution Strategy: checked for repository governance strategy.
- Building Block View: checked for repository governance building blocks.
- Runtime View: checked for repository governance runtime flows.
- Deployment View: no deployment topology change required.
- Crosscutting Concepts: checked for engineering governance and documentation synchronization.
- Architecture Decisions: checked for active workflow and agent governance decisions.
- Quality Requirements: checked for governance quality scenarios.
- Risks and Technical Debt: checked for governance drift, checkpoint and push automation risks.
- Glossary: checked for governance terms.

The checked workflow create end state is:

1. no blocking requirement questions remain,
2. complete checked docs/workflow/workflow.md,
3. checked or updated arc42 documentation,
4. Documentation Governance passed,
5. explicit release for workflow execute.

Reviewer role: Senior System Architect / arc42 Architecture Governance.

## Agent Governance Documentation Status

The agent organigramm and process-strand model are documented in:

- `docs/agents/organigramm.md`
- `docs/agents/agent-governance.md`
- `docs/agents/skill-registry.md`
- `docs/process/skills-update.md`
- `docs/process/workflow-create.md`
- `docs/process/workflow-execute.md`
- `docs/process/push-auto.md`

arc42 sections updated for agent governance:

- Architecture Constraints
- Solution Strategy
- Building Block View
- Runtime View
- Crosscutting Concepts
- Architecture Decisions
- Quality Requirements
- Risks and Technical Debt
- Glossary

Current workflow branch: `architecture/workflow-governance-flowchart-v2-20260517`.
