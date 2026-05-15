# arc42 Synchronization

## Purpose

The arc42 governance skill keeps architecture documentation aligned with verified requirements, implementation behavior and architecture decisions.

## Update Triggers

Review arc42 when:

- EPIC assumptions change
- service boundaries change
- plugin versus server responsibilities change
- runtime behavior changes
- deployment topology changes
- persistence ownership changes
- UI communication strategy changes
- resilience decisions change
- scalability constraints change
- observability or audit requirements change
- architecture decisions are added, superseded or reinterpreted

## Section Mapping

Use the existing arc42 structure:

- `01-introduction-and-goals.md` for goals and stakeholder-facing quality needs
- `02-architecture-constraints.md` for non-negotiable constraints
- `03-system-scope-and-context.md` for boundaries and external systems
- `04-solution-strategy.md` for high-level architecture strategy
- `05-building-block-view.md` for components and responsibility ownership
- `06-runtime-view.md` for runtime behavior and communication paths
- `07-deployment-view.md` for deployment topology
- `08-crosscutting-concepts.md` for shared concerns such as evidence integrity, security, observability and resilience
- `09-architecture-decisions.md` for ADR references
- `10-quality-requirements.md` for quality scenarios and verification expectations
- `11-risks-and-technical-debt.md` for known gaps and unresolved risk
- `12-glossary.md` for terminology

## ADR Propagation

Architecture decisions must be reflected consistently:

- ADRs record decisions and context.
- arc42 summarizes active architectural consequences.
- workplans route implementation slices according to those consequences.
- skills and roles apply the rules without redefining the decision.

Do not edit ADR intent or historical records without an explicit architecture decision.

## Runtime And Deployment Changes

Runtime and deployment changes must be documented when they alter:

- communication protocol
- service ownership
- process boundary
- storage boundary
- retry, timeout or degradation behavior
- health-check semantics
- deployment unit
- data retention or evidence handling

The documentation must distinguish planned behavior from implemented behavior.

## Stop Conditions

Stop and report if:

- arc42 conflicts with the EPIC and the current source of truth is unclear
- an ADR contradicts current implementation and the intended decision is unclear
- a service boundary moved without a documented architecture decision
- resilience behavior changed but no documentation owner is clear
- deployment assumptions changed but the deployment view cannot be updated from verified evidence
