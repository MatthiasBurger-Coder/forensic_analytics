# arc42 Placement Assessment

## Status

Created by workflow Slice S01 on branch
`docs/workflow-architecture-assessment-20260606`.

This document classifies existing architecture entries into the verified
Forensic Analytics arc42 structure. It is a source assessment and placement
matrix. It is not an arc42 chapter and not an ADR.

## Purpose

The workflow no longer needs the complete project progress assessment text to
decide where architecture entries belong. The official arc42 template and
section guidance provide the placement rules. Existing repository architecture
documents provide the evidence to classify entries.

The complete architecture progress assessment source is not available in the
repository at the time of this slice. It must not be invented. If the complete
text is later provided, store it separately as:

```text
docs/arc42/08-crosscutting-concepts/architecture-source-maps/assessments/2026-06-architecture-progress-assessment.md
```

## Verified Inputs

- `docs/workflow/workflow.md`
- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/README.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-coupling-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/legacy-reference-classification.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/microservice-governance.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-caller-retirement-plan.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-runtime-isolation.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monorepo-service-build-strategy.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-communication-matrix.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-migration-map.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`
- `docs/arc42/README.md`
- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0009-no-shared-common-modules.md`
- `docs/arc42/11-risks-and-technical-debt.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0010-contract-first-rest-and-grpc.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0013-data-ownership-per-service.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0017-target-microservices-service-landscape.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0018-initial-logical-contracts.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0022-final-modular-monolith-source-tree-retirement.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0025-consolidated-architecture-baseline-without-migration.md`
- `.github/workflows/sonar_check.yml`
- Official arc42 sources listed in `docs/workflow/workflow.md` and
  `docs/workflow/context-pack.md`, including the plain Markdown template at
  `https://github.com/arc42/arc42-template/raw/master/dist/arc42-template-EN-plain-markdownMP.zip`

## Placement Rules

| Entry type | Target | Placement rule |
|---|---|---|
| Fundamental solution approach, top-level target strategy, migration strategy | `docs/arc42/04-solution-strategy.md` | Use for strategic direction and short rationale. Link to details elsewhere. |
| Static structure, modules, services, component responsibility, source-code location | `docs/arc42/05-building-block-view.md` | Use for current and target building blocks, responsibilities and interfaces. |
| Runtime interaction, scenario, request flow or behavior over time | `docs/arc42/06-runtime-view.md` | Use for verified or explicitly planned runtime scenarios. |
| Infrastructure, container, deployment topology or runtime environment | `docs/arc42/07-deployment-view.md` | Use only for verified deployment material or explicitly planned deployment gaps. |
| Crosscutting rule, concept, governance model, data ownership, persistence, contract, evidence integrity, security, observability, quality-gate concept | `docs/arc42/08-crosscutting-concepts.md` | Use for concepts that apply across several building blocks. |
| Accepted decision, major tradeoff, selected technology, ADR summary | `docs/arc42/09-architecture-decisions.md` or numbered ADR | Reference existing ADRs before adding or changing decisions. |
| Known risk, maturity gap, unresolved readiness, technical debt, migration blocker | `docs/arc42/11-risks-and-technical-debt.md` | Use as the primary chapter for architecture risk and debt. |
| Term definition | `docs/arc42/12-glossary.md` | Use only for terminology that needs stable meaning. |

## Requirement Categories

The placement matrix uses these requirement categories when classifying
architecture entries:

- Functional behavior
- Non-functional quality
- Architecture and service boundary
- Resilience
- Scalability
- UX
- Observability
- Security and data protection
- Data ownership and persistence
- Quality gate
- Assumption
- Open question

## Non-Goals And Forbidden Claims

This assessment does not authorize backend, frontend, runtime, contract,
build-logic or deployment changes. It does not extract services, rewrite ADR
history, invent maturity scores, invent schema or endpoint details, invent
graph or runtime facts, or claim production readiness from target service
names. Target service names are architecture direction and migration evidence,
not production-readiness evidence.

## Placement Matrix

| Architecture entry | Source evidence | Target arc42 section | Rationale | Gaps or stop notes |
|---|---|---|---|---|
| Documentation root classification: `docs/arc42/08-crosscutting-concepts/architecture-source-maps/**` is source-map input and `docs/arc42/**` is authoritative arc42 output. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/README.md`, `docs/arc42/README.md` | `08-crosscutting-concepts.md` | This is a documentation-governance concept that affects all architecture writing. | Do not create `docs/arc42/**`. |
| EPIC v0.2 is the product and requirement baseline for arc42. | `docs/arc42/README.md`, `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md` | `01-introduction-and-goals.md` and existing arc42 README | Requirement source and stakeholder-facing goals belong to chapter 1; the README can keep source-baseline navigation. | No current slice changes chapter 1. |
| Producer-side plugins trigger server-side analysis, while Analytics owns normalization, replay, graph projection and LLM evidence packages. | `docs/arc42/04-solution-strategy.md`, EPIC v0.2, ADR-0025 | `04-solution-strategy.md` | This is high-level solution strategy. | Preserve planned versus implemented distinctions. |
| FA-MSA-001 target microservice service landscape. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`, `docs/arc42/09-architecture-decisions/adr/ADR-0017-target-microservices-service-landscape.md`, `docs/arc42/05-building-block-view.md` | `04-solution-strategy.md`, `05-building-block-view.md`, `09-architecture-decisions.md` | The target landscape is strategic, static structure and an accepted ADR consequence. | Do not claim production readiness from target naming. |
| Current Gradle module and service-root inventory. | `settings.gradle.kts`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md` | `05-building-block-view.md` | Static module and service-root structure belongs to the building block view. | Mark predecessor, current, target, optional and planned roots distinctly. |
| Current service roots are migration evidence, not completed production microservices. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/microservice-governance.md` | `11-risks-and-technical-debt.md` | This is a maturity and readiness risk. | No service-readiness claim without build, start, health, container and deployment evidence. |
| Service boundary rules: business responsibility, owned data, service-local models, no shared Java implementation. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/microservice-governance.md`, ADR-0009, ADR-0013, ADR-0017 | `08-crosscutting-concepts.md`, `09-architecture-decisions.md` | Boundary rules are crosscutting concepts and ADR consequences. | If a boundary is changed, route through architecture and ADR review. |
| Current-to-target service decomposition. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-migration-map.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md` | `05-building-block-view.md` | Current and target building blocks plus migration mapping belong to static structure. | Keep historical predecessor names as provenance, not aliases. |
| Strangler-first migration approach. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-migration-map.md` | `04-solution-strategy.md` | Migration approach is a strategy. | Detailed blockers stay in chapter 11. |
| Historical monolith coupling and caller-retirement evidence. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-coupling-map.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-caller-retirement-plan.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monolith-runtime-isolation.md`, ADR-0022 | `11-risks-and-technical-debt.md`, supporting references in `05-building-block-view.md` | Coupling, caller-retirement and legacy runtime isolation are risk/debt and historical structure topics. | Do not treat retired source trees as current runtime units. |
| Contract-first REST and gRPC communication. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-communication-matrix.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/contract-versioning.md`, ADR-0010, ADR-0018 | `08-crosscutting-concepts.md`, `09-architecture-decisions.md` | Contract-first communication is a crosscutting integration concept and an accepted decision. | Contract field, endpoint or compatibility claims require contract governance. Planned contracts are not runtime implementation evidence. |
| gRPC ingestion boundary. | `docs/arc42/05-building-block-view.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-communication-matrix.md`, ADR-0010 | `05-building-block-view.md`, `08-crosscutting-concepts.md` | The owning service is a building block; generated-code and DTO rules are crosscutting. | Do not turn generated transport code into shared DTO/domain modules. |
| Per-service data ownership and one-writer persistence rule. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`, ADR-0013 | `08-crosscutting-concepts.md`, `09-architecture-decisions.md` | Ownership rules are crosscutting; the accepted one-writer rule is an ADR consequence. | Unclear owner or write path stops later slices. |
| PostgreSQL for repository-source workspace metadata. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`, ADR-0024 | `09-architecture-decisions.md`, supporting notes in `08-crosscutting-concepts.md` and `11-risks-and-technical-debt.md` | This is an accepted bounded persistence decision with crosscutting data-ownership consequences and risk of over-interpretation. | Keep PostgreSQL bounded to repository-source workspace metadata. |
| H2 scope after PostgreSQL cutover. | ADR-0023, ADR-0024, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md` | `09-architecture-decisions.md`, `11-risks-and-technical-debt.md` | H2 is an accepted test-only/historical decision and a risk if misread as runtime fallback. | Do not describe H2 as runtime storage or Docker fallback. |
| Broader analytics persistence remains open. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`, `docs/arc42/09-architecture-decisions.md`, ADR-0024, ADR-0025 | `09-architecture-decisions.md`, `11-risks-and-technical-debt.md` | Open decisions belong in chapter 9, while resulting uncertainty is risk/debt. | Do not infer canonical analytics database, graph, vector, report or LLM storage decisions. |
| Build strategy for service roots in a monorepo. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/monorepo-service-build-strategy.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md` | `04-solution-strategy.md`, `08-crosscutting-concepts.md`, `10-quality-requirements.md` | Build strategy is strategic and crosscutting; quality commands belong to quality requirements. | Do not document service-specific commands for unregistered roots. |
| Quality gates and dependency verification. | `QUALITY.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md`, `docs/arc42/10-quality-requirements.md`, `.github/workflows/sonar_check.yml` | `10-quality-requirements.md`, supporting governance in `08-crosscutting-concepts.md` | Quality scenarios and commands belong to chapter 10; process governance belongs to chapter 8. | This workflow is documentation-only and requires `git diff --check`. `current-build-and-test-map.md` says no `.github/workflows` directory was verified, while `.github/workflows/sonar_check.yml` exists in this revision; refresh CI source-map evidence before making CI-readiness claims. |
| Docker-local evidence and deployment readiness gaps. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md`, `docs/arc42/07-deployment-view.md` | `07-deployment-view.md`, `11-risks-and-technical-debt.md` | Deployment material belongs to chapter 7; missing readiness is chapter 11 risk. | Do not claim Swarm or Kubernetes readiness without verified manifests and commands. |
| Observability-stack as deployment-oriented material, not shared Java runtime. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-state.md`, `docs/arc42/08-crosscutting-concepts.md`, ADR-0025 | `05-building-block-view.md`, `08-crosscutting-concepts.md` | The stack is a building block plus operational crosscutting concept. | Logs remain diagnostics, not forensic evidence. |
| CLI client as public API client boundary, not backend service. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-communication-matrix.md` | `05-building-block-view.md`, `08-crosscutting-concepts.md` | Client boundary is static structure; public API consumption rules are crosscutting. | Do not assign evidence ownership to CLI. |
| Testbed as non-production integration and system-test boundary. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-roots.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md` | `05-building-block-view.md`, `10-quality-requirements.md` | Testbed is a building block and quality/test environment. | Do not treat testbed fixtures as production shared modules. |
| Legacy reference classification and documentation cleanup rules. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/legacy-reference-classification.md`, `docs/arc42/08-crosscutting-concepts/documentation-governance/*` | `08-crosscutting-concepts.md`, `11-risks-and-technical-debt.md` | Documentation governance is crosscutting; stale executable references are risk/debt. | Contract compatibility vocabulary must be preserved unless contract governance approves changes. |
| Distributed monolith risk. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/microservice-governance.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-coupling-map.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/target-microservices-architecture.md` | `11-risks-and-technical-debt.md` | This is a known architecture risk when service names exist without autonomous runtime evidence. | State as risk, not as proven defect, unless later evidence proves active coupling. |
| Wrong service-cut risk. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/microservice-governance.md`, ADR-0017 | `11-risks-and-technical-debt.md` | Incorrect decomposition is a service-boundary risk. | Future cuts need business responsibility, contract, data ownership and test impact. |
| Unstable or early gRPC contract stabilization risk. | `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-communication-matrix.md`, `docs/arc42/08-crosscutting-concepts/architecture-source-maps/contract-versioning.md`, ADR-0010 | `11-risks-and-technical-debt.md` | Contract instability is an explicit technical and migration risk. | Do not change proto fields, endpoints or compatibility semantics in this workflow. |
| SCA migration concept. | User instruction and workflow only; no verified expansion found in source-map docs during S01 | Candidate for `08-crosscutting-concepts.md`; unresolved term may also need `12-glossary.md` | The placement is crosscutting if SCA is verified as a migration concept, but the meaning is not verified. | Stop before authoritative SCA definition. Record as unresolved. |

## Extraction Plan

1. Update chapter 11 first with risk and debt entries from the matrix:
   distributed monolith risk, service-readiness gaps, contract stabilization
   risk, wrong service-cut risk, persistence boundary risk and predecessor or
   transitional module migration debt.
2. Update chapters 4 and 5 with target strategy and current-to-target
   building-block mapping. Keep strategy short and structure detailed.
3. Update chapters 8 and 9 with crosscutting governance, contract-first,
   data-ownership and decision-reference entries. Reference ADRs instead of
   rewriting decision history.
4. Do not update chapter 6 or 7 in this workflow unless a verified runtime or
   deployment entry is explicitly extracted from the matrix.
5. Do not update chapter 12 unless an unresolved term becomes a verified term.

## Unresolved Gaps

- The complete architecture progress assessment source text is not available.
- `SCA` is not expanded by verified repository evidence in this slice.
- Runtime readiness for the full target service landscape remains unverified.
- Docker Swarm and Kubernetes readiness remain unverified.
- Broader canonical Analytics persistence remains open outside the
  repository-source PostgreSQL decision.
- CI source-map evidence needs refresh because
  `docs/arc42/08-crosscutting-concepts/architecture-source-maps/current-build-and-test-map.md` still says no
  `.github/workflows` directory was verified, while
  `.github/workflows/sonar_check.yml` exists in this revision.

## Slice S01 Review Notes

Senior Documentation Engineer:

- The placement assessment stays under `docs/arc42/08-crosscutting-concepts/architecture-source-maps/assessments/`.
- Authoritative arc42 extracts remain planned for later slices under
  `docs/arc42/**`.
- The complete assessment source is explicitly unavailable and not invented.

Senior Requirement Engineer:

- The matrix traces entries to repository source-map files, arc42 files, ADRs
  and EPIC baseline material where applicable.
- Missing source text and unresolved terminology are recorded as gaps, not
  requirements.

Senior System Architect:

- Current modules, predecessor roots, target services and optional planned
  services remain separate evidence categories.
- Production microservice readiness is not claimed without verified runtime,
  health, container and deployment evidence.
- `graph-replay-service` and `report-generation-service` are registered
  placeholder roots, not implemented runtime services.
- CI source-map evidence must be refreshed before CI-readiness claims.

## Verification Commands

Required for this slice:

```bash
test -f docs/arc42/08-crosscutting-concepts/architecture-source-maps/assessments/2026-06-arc42-placement-assessment.md
git diff --check
```
