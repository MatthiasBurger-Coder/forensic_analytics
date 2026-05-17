# Skill And Agent Registry

This registry assigns skills, roles and Codex agents to the three governed
process strands.

The existing detailed historical inventory remains in
[`docs/skill-audit/skill-inventory.md`](../skill-audit/skill-inventory.md). This
registry is the process-governance view used for strand ownership, allowed file
scope, documentation duty and release readiness.

## Registry Field Model

Each entry is interpreted as the row in the tables below plus the strand default
matrix.

Required registry fields:

- Name
- Purpose
- Process strand
- Parent / Owner
- Inputs
- Outputs
- May change
- Must not change
- Quality gate
- Documentation duty
- Linked files / processes
- Status

## Strand Default Matrix

| Process strand | Inputs | Outputs | May change | Must not change | Quality gate | Documentation duty |
|---|---|---|---|---|---|---|
| `skills-agents` | Root rules, existing skills, roles, prompts, Codex agents, registry, organigramm, process docs | Approved skill/agent governance change | `.agents/**`, `.codex/agents/**`, `.codex/skills/**`, `.codex/subagents/**`, `docs/agents/**`, `docs/process/**`, `docs/skill-audit/**`, governance-limited `AGENTS.md`, `docs/governance/**`, `docs/arc42/**`, `docs/adr/**` | Product backend, frontend, Docker/runtime, analytics engine, Gradle/build and contract implementation files unless explicitly scoped for governance tooling | Integrity review, registry review, organigramm review, documentation review, `git diff --check`, documented docs checks | Registry, organigramm, process docs and AGENTS impact must be updated or explicitly checked |
| `workflow create` | User requirement, Requirement Clarification Loop, root rules, `QUALITY.md`, arc42, ADRs, EPIC, routing rules, existing workflows | Checked `docs/workflow/workflow.md`; checked or updated arc42 docs | Dedicated workflow planning artifacts and architecture/process documentation needed for planning | Product implementation, backend code, frontend code, Docker/runtime code, analytics implementation | Five-role Three Amigos gate, workflow.md validation, arc42 validation, Documentation Governance, Final Gate, `git diff --check` | `docs/workflow/workflow.md` and arc42 review are mandatory end artifacts; release requires no blocking questions |
| `workflow execute` | Checked `docs/workflow/workflow.md`, checked arc42 docs, workflow branch, slice plan, role assignments | Slice changes, quality evidence, slice checkpoint commits and pushes, execution report, synchronized docs | Only files explicitly allowed by the active workflow slice | Any out-of-scope files and any scope expansion not returned to `workflow create` | Slice quality gate, staged diff check, checkpoint push, final workflow execute gate, `QUALITY.md` commands, diff checks | Execution report, arc42 consistency, testing docs, checkpoint commit SHA and push result |

Shared governance roles are strand-scoped support roles. They may be invoked in
all three strands, but their effective allowed files and gates are the active
strand defaults above.

## Process Governance Skills And Roles

| Name | Purpose | Process strand | Parent / Owner | Linked files / processes | Status |
|---|---|---|---|---|---|
| Senior System Architect | Top-level architecture and process governance authority | `skills-agents`; `workflow create`; `workflow execute` | Agent Workflow Orchestrator | `.agents/roles/senior-system-architect.md` | active |
| Documentation Governance | Mandatory documentation review and synchronization | `skills-agents`; `workflow create`; `workflow execute` | Senior Documentation Engineer | `.agents/roles/senior-documentation-engineer.md`, `.agents/skills/documentation-sync/SKILL.md` | active |
| Skill / Agent Creator | Creates or updates skill, role, prompt and agent material | `skills-agents` | Senior System Architect | system `skill-creator`, `.agents/AGENTS.md` | active |
| Skill Integrity Reviewer | Reviews skill responsibility, STOP rules, frontmatter and conflicts | `skills-agents` | Senior System Architect | `.agents/skills/skill-registry-conflict-auditor/SKILL.md` | active |
| Skill Registry Maintainer | Maintains this registry and historical inventory links | `skills-agents` | Documentation Governance | `docs/agents/skill-registry.md`, `docs/skill-audit/skill-inventory.md` | active |
| Organigramm Maintainer | Maintains role hierarchy and strand diagrams | `skills-agents` | Documentation Governance | `docs/agents/organigramm.md` | active |
| AGENTS.md Maintainer | Keeps root agent rules aligned with verified governance | `skills-agents` | Senior System Architect | `AGENTS.md` | active |
| Process Governance Maintainer | Maintains process docs for the three strands | `skills-agents` | Senior System Architect | `docs/process/**`, `docs/governance/README.md` | active |
| Push Auto Guard | Blocks `push auto` outside `skills-agents` | `skills-agents` | git-commit-preparation | `.agents/skills/git-commit-preparation/SKILL.md`, `.codex/agents/git_commit_reviewer.toml`, `.codex/agents/git_commit_operator.toml` | active |
| Slice Checkpoint Push | Commits a completed `workflow execute` slice and pushes the current workflow branch to origin | `workflow execute` | Workflow Executor | `docs/process/workflow-execute.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/git-commit-preparation/SKILL.md` | active |
| docs/workflow/workflow.md Maintainer | Owns checked workflow artifact completeness | `workflow create`; `workflow execute` | Senior Workflow Architect | `.agents/skills/workflow-authoring/SKILL.md`, `docs/workflow/workflow.md` | active |
| arc42 Architecture Documentation Maintainer | Checks and updates arc42 consequences | `workflow create`; `workflow execute` | Senior System Architect | `.agents/skills/arc42-architecture-governance/SKILL.md`, `docs/arc42/**` | active |
| Testing Documentation Maintainer | Keeps test strategy and quality evidence traceable | `workflow create`; `workflow execute` | Senior Tester | `QUALITY.md`, workflow quality logs | active |
| Execution Report Maintainer | Records execution evidence after slices | `workflow execute` | Workflow Executor | `docs/workflow/execution-summary.md` or active workflow report | active |

## Mandatory Workflow Create Gate Roles

| Role | Mandatory focus |
|---|---|
| Senior Requirement Engineer | Goal, scope, non-goals, acceptance criteria, assumptions and open questions |
| Senior System Architect | Architecture boundaries, arc42, service boundaries, plugin-vs-analytics boundary and risks |
| Senior Java Backend Developer | Backend impact, ports, adapters, domain, JUnit 6 testability, Spring and microservice consequences |
| Senior React Frontend Developer | Frontend impact, UX flows, React components, state, API adapters and build/test consequences |
| Senior Tester | Testability, regression, quality gates, acceptance criteria and slice acceptance |

## Discoverable Skill Assignments

| Name | Purpose | Process strand | Parent / Owner | Linked files / processes | Status |
|---|---|---|---|---|---|
| agent-swarm-coordination-specialist | Dependency graph planning and multi-agent coordination | `workflow execute` | Senior Swarm Orchestrator | `.agents/skills/agent-swarm-coordination-specialist/SKILL.md` | active |
| adr-steward | ADR lifecycle and governance-decision alignment | `workflow create`; `workflow execute`; `skills-agents` | Senior System Architect | `.agents/skills/adr-steward/SKILL.md` | active |
| agent-handoff-protocol | Explicit role and subagent handoff contracts | `workflow execute` | Senior Swarm Orchestrator | `.agents/skills/agent-handoff-protocol/SKILL.md` | active |
| analysis-storage-architect | Storage architecture for raw ingestion, normalized stores and projections | `workflow execute` | Senior Analysis Storage Architect | `.agents/skills/analysis-storage-architect/SKILL.md` | active |
| analytics-persistence-review | Persistence and deterministic artifact review | `workflow execute` | Analytics Persistence Reviewer | `.agents/skills/analytics-persistence-review/SKILL.md` | active |
| analytics-slice-workflow | Analytics implementation slice planning | `workflow create` | Senior Workflow Architect | `.agents/skills/analytics-slice-workflow/SKILL.md` | active |
| arc42-architecture-governance | arc42 synchronization | `workflow create`; `workflow execute` | Senior System Architect | `.agents/skills/arc42-architecture-governance/SKILL.md` | active |
| architecture-archunit-hexagonal | ArchUnit hexagonal boundary rules | `workflow execute` | Senior Tester | `.agents/skills/architecture-archunit-hexagonal/SKILL.md` | active |
| architecture-hexagonal | Hexagonal boundary preservation | `workflow execute` | Senior System Architect | `.agents/skills/architecture-hexagonal/SKILL.md` | active |
| architecture-modular-monorepo | Module-boundary and Gradle project responsibility | `workflow execute` | Senior System Architect | `.agents/skills/architecture-modular-monorepo/SKILL.md` | active |
| build-gradle | Gradle build logic and quality-gate alignment | `workflow execute` | Senior DevOps Engineer | `.agents/skills/build-gradle/SKILL.md` | active |
| code-property-graph-joern-specialist | Joern and CPG planning | `workflow execute` | Senior Joern CPG Specialist | `.agents/skills/code-property-graph-joern-specialist/SKILL.md` | active |
| contract-first-api-steward | Contract-first REST/gRPC governance | `workflow execute` | Contract Governance | `.agents/skills/contract-first-api-steward/SKILL.md` | active |
| contract-governance-expert | Cross-service contract governance | `workflow execute` | Contract Governance | `.agents/skills/contract-governance-expert/SKILL.md` | active |
| data-ownership-persistence-steward | Service data ownership and persistence governance | `workflow execute` | Senior System Architect | `.agents/skills/data-ownership-persistence-steward/SKILL.md` | active |
| devops-ci-cd | CI/CD and local CI equivalents | `workflow execute` | Senior DevOps Engineer | `.agents/skills/devops-ci-cd/SKILL.md` | active |
| devops-docker | Docker and container workflow | `workflow execute` | Senior DevOps Engineer | `.agents/skills/devops-docker/SKILL.md` | active |
| devops-kubernetes | Kubernetes material after manifest verification | `workflow execute` | Senior DevOps Engineer | `.agents/skills/devops-kubernetes/SKILL.md` | active |
| distributed-systems-architect | Distributed jobs and worker lifecycle | `workflow execute` | Senior System Architect | `.agents/skills/distributed-systems-architect/SKILL.md` | active |
| documentation-sync | Documentation synchronization | `skills-agents`; `workflow create`; `workflow execute` | Senior Documentation Engineer | `.agents/skills/documentation-sync/SKILL.md` | active |
| engineering-governance | Umbrella governance synchronization | `skills-agents`; `workflow create`; `workflow execute` | Senior System Architect | `.agents/skills/engineering-governance/SKILL.md` | active |
| frontend-hexagonal | Frontend boundary design | `workflow execute` | Senior React Frontend Developer | `.agents/skills/frontend-hexagonal/SKILL.md` | active |
| frontend-react | React frontend work after module verification | `workflow execute` | Senior React Frontend Developer | `.agents/skills/frontend-react/SKILL.md` | active |
| frontend-ux-guidelines | UX and accessibility review | `workflow execute` | Senior UX Designer | `.agents/skills/frontend-ux-guidelines/SKILL.md` | active |
| git-branch-strategy | Branch isolation and line-ending checks | `workflow create`; `skills-agents` | Release Governance | `.agents/skills/git-branch-strategy/SKILL.md` | active |
| git-clean | Post-merge cleanup | `skills-agents` | Push Auto Guard | `.agents/skills/git-clean/SKILL.md` | active |
| git-commit-message-preparation | Commit message preparation | `skills-agents` | Push Auto Guard | `.agents/skills/git-commit-message-preparation/SKILL.md` | active |
| git-commit-preparation | Commit, push, slice checkpoint push and `push auto` readiness workflow | `skills-agents`; `workflow execute` | Push Auto Guard / Workflow Executor | `.agents/skills/git-commit-preparation/SKILL.md` | active |
| git-large-repository-specialist | Large repository checkout hardening | `workflow execute` | Senior Git Workspace Specialist | `.agents/skills/git-large-repository-specialist/SKILL.md` | active |
| grpc-ingestion | gRPC ingestion adapter work and review | `workflow execute` | Senior Java Backend Developer | `.agents/skills/grpc-ingestion/SKILL.md` | active |
| grpc-streaming-specialist | gRPC and Protobuf streaming governance | `workflow execute` | Senior gRPC Proto Specialist | `.agents/skills/grpc-streaming-specialist/SKILL.md` | active |
| ingestion-handoff-review | Engine-request and gRPC handoff review | `workflow execute` | Ingestion Handoff Reviewer | `.agents/skills/ingestion-handoff-review/SKILL.md` | active |
| java-25-backend | Java 25 backend implementation style | `workflow execute` | Senior Java Backend Developer | `.agents/skills/java-25-backend/SKILL.md` | active |
| joern-semantic-analysis | Optional Joern/CPG semantic enrichment review | `workflow execute` | Senior Joern CPG Specialist | `.agents/skills/joern-semantic-analysis/SKILL.md` | active |
| microservice-migration-safety-gate | Production service extraction safety gate | `workflow create`; `workflow execute` | Microservice Senior Expert | `.agents/skills/microservice-migration-safety-gate/SKILL.md` | active |
| microservice-runtime-readiness-expert | Service runtime independence readiness | `workflow execute` | Microservice Senior Expert | `.agents/skills/microservice-runtime-readiness-expert/SKILL.md` | active |
| microservice-senior-expert | Microservice autonomy and no shared Java modules | `workflow execute` | Microservice Senior Expert | `.agents/skills/microservice-senior-expert/SKILL.md` | active |
| migration-workflow | Migration planning in verified slices | `workflow create` | Senior Workflow Architect | `.agents/skills/migration-workflow/SKILL.md` | active |
| observability-diagnostics | Logging, metrics and redaction diagnostics | `workflow execute` | Senior DevOps Engineer | `.agents/skills/observability-diagnostics/SKILL.md` | active |
| observability-runtime-diagnostics | Runtime trace context and diagnostics governance | `workflow execute` | Senior DevOps Engineer | `.agents/skills/observability-runtime-diagnostics/SKILL.md` | active |
| performance-scalability-engineer | Scalability, quotas and instrumentation planning | `workflow execute` | Senior Performance Engineer | `.agents/skills/performance-scalability-engineer/SKILL.md` | active |
| protobuf-contracts | Protobuf compatibility review | `workflow execute` | Senior gRPC Proto Specialist | `.agents/skills/protobuf-contracts/SKILL.md` | active |
| quality-architecture-validation | Architecture validation and dependency checks | `workflow execute` | Senior Tester | `.agents/skills/quality-architecture-validation/SKILL.md` | active |
| quality-archunit-review | JUnit, ArchUnit and coverage review | `workflow execute` | Senior Tester | `.agents/skills/quality-archunit-review/SKILL.md` | active |
| quality-gate | Repository quality-gate execution | `workflow execute` | Senior Tester | `.agents/skills/quality-gate/SKILL.md` | active |
| quality-gate-governance | Quality-gate selection and reporting | `workflow create`; `workflow execute` | Senior Tester | `.agents/skills/quality-gate-governance/SKILL.md` | active |
| quality-gate-orchestrator | Slice quality-gate planning, execution and reporting | `workflow execute` | Senior Tester | `.agents/skills/quality-gate-orchestrator/SKILL.md` | active |
| quality-mutation-testing | Mutation-testing guidance after tool verification | `workflow execute` | Senior Tester | `.agents/skills/quality-mutation-testing/SKILL.md` | active |
| quality-testing-strategy | Test planning and deterministic fixtures | `workflow create`; `workflow execute` | Senior Tester | `.agents/skills/quality-testing-strategy/SKILL.md` | active |
| replay-graph-llm-review | Replay, graph, report and LLM evidence review | `workflow execute` | Replay Graph LLM Reviewer | `.agents/skills/replay-graph-llm-review/SKILL.md` | active |
| replay-runtime-correlation-specialist | Runtime replay and trace stitching planning | `workflow execute` | Replay Runtime Specialist | `.agents/skills/replay-runtime-correlation-specialist/SKILL.md` | active |
| requirement-engineering | Requirement drift and traceability | `workflow create` | Senior Requirement Engineer | `.agents/skills/requirement-engineering/SKILL.md` | active |
| release-branch-governance | Branch, commit, push and release-readiness governance | `skills-agents`; `workflow execute` | Release Governance | `.agents/skills/release-branch-governance/SKILL.md` | active |
| resilience-engineering | Resilience decisions and failure handling | `workflow create`; `workflow execute` | Senior System Architect | `.agents/skills/resilience-engineering/SKILL.md` | active |
| security-sandbox-specialist | Untrusted repository and safe Git handling | `workflow execute` | Senior Security Sandbox Engineer | `.agents/skills/security-sandbox-specialist/SKILL.md` | active |
| security-threat-modeling | Security and supply-chain threat modeling | `workflow create`; `workflow execute` | Senior System Architect | `.agents/skills/security-threat-modeling/SKILL.md` | active |
| service-decomposition-bounded-context | Bounded-context service decomposition | `workflow create`; `workflow execute` | Microservice Senior Expert | `.agents/skills/service-decomposition-bounded-context/SKILL.md` | active |
| skill-registry-conflict-auditor | Skill, role and workflow responsibility conflicts | `skills-agents` | Senior System Architect | `.agents/skills/skill-registry-conflict-auditor/SKILL.md` | active |
| source-analysis-pipeline | Static source ingestion and semantic artifacts | `workflow execute` | Source Analysis Reviewer | `.agents/skills/source-analysis-pipeline/SKILL.md` | active |
| spring-core | Spring wiring after verified module usage | `workflow execute` | Senior Java Backend Developer | `.agents/skills/spring-core/SKILL.md` | active |
| swarm-coordination | Bounded parallel subagent coordination | `workflow execute` | Senior Swarm Orchestrator | `.agents/skills/swarm-coordination/SKILL.md` | active |
| swarm-orchestration | Multi-agent workflow planning | `workflow execute` | Senior Swarm Orchestrator | `.agents/skills/swarm-orchestration/SKILL.md` | active |
| testing-junit6 | JUnit 6 tests and deterministic fixtures | `workflow execute` | Senior Tester | `.agents/skills/testing-junit6/SKILL.md` | active |
| three-amigos-requirement-gatekeeper | Requirement gate before workflow authoring | `workflow create` | Senior Requirement Engineer | `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | active |
| workflow-authoring | Checked `docs/workflow/workflow.md` creation and arc42 review | `workflow create` | Senior Workflow Architect | `.agents/skills/workflow-authoring/SKILL.md` | active |
| workflow-conflict-resolution | Overlapping local change handling | `workflow create`; `workflow execute` | Senior Swarm Orchestrator | `.agents/skills/workflow-conflict-resolution/SKILL.md` | active |
| workflow-executor | Active workflow execution | `workflow execute` | Workflow Executor | `.agents/skills/workflow-executor/SKILL.md` | active |
| workflow-slice | Workflow slice planning | `workflow create` | Senior Workflow Architect | `.agents/skills/workflow-slice/SKILL.md` | active |
| workflow-slice-execution | Slice read-only verification and execution protocol | `workflow execute` | Workflow Executor | `.agents/skills/workflow-slice-execution/SKILL.md` | active |
| workspace-lifecycle-specialist | Workspace lifecycle and checkout preparation | `workflow execute` | Senior Git Workspace Specialist | `.agents/skills/workspace-lifecycle-specialist/SKILL.md` | active |

## Project Role Assignments

These role files use the same strand default matrix for inputs, outputs,
allowed changes, forbidden changes, quality gates and documentation duty.

| Name | Purpose | Process strand | Parent / Owner | Linked files / processes | Status |
|---|---|---|---|---|---|
| Microservice Senior Expert | Service autonomy, deployability and no shared Java modules | `workflow execute` | Senior System Architect | `.agents/roles/microservice-senior-expert.md` | active |
| Senior Analysis Storage Architect | Analysis storage, projections and trace correlation | `workflow execute` | Senior System Architect | `.agents/roles/senior-analysis-storage-architect.md` | active |
| Senior DevOps | Gradle, Docker, CI/CD, observability and deployment | `workflow execute` | Senior Swarm Orchestrator | `.agents/roles/senior-devops.md` | active |
| Senior Documentation Engineer | Documentation consistency, workflow handoff and ADR alignment | `skills-agents`; `workflow create`; `workflow execute` | Documentation Governance | `.agents/roles/senior-documentation-engineer.md` | active |
| Senior Git/Workspace Specialist | Checkout, source-root preparation and workspace lifecycle | `workflow execute` | Senior Swarm Orchestrator | `.agents/roles/senior-git-workspace-specialist.md` | active |
| Senior gRPC/Proto Specialist | gRPC, Protobuf and compatibility review | `workflow execute` | Senior Java Backend Developer | `.agents/roles/senior-grpc-proto-specialist.md` | active |
| Senior Java Backend Developer | Backend domain, application, ports, adapters and tests | `workflow execute` | Workflow Executor | `.agents/roles/senior-java-backend.md` | active |
| Senior Joern/CPG Specialist | Joern and Code Property Graph planning | `workflow execute` | Senior Java Backend Developer | `.agents/roles/senior-joern-cpg-specialist.md` | active |
| Senior Performance Engineer | Scalability, quotas and instrumentation planning | `workflow execute` | Senior System Architect | `.agents/roles/senior-performance-engineer.md` | active |
| Senior Plugin Integration Developer | Plugin producer handoff and gRPC client integration review | `workflow execute` | Senior Java Backend Developer | `.agents/roles/senior-plugin-integration-developer.md` | active |
| Senior React Frontend Developer | React UI slices, state, accessibility and API integration | `workflow execute` | Workflow Executor | `.agents/roles/senior-react-frontend.md` | active |
| Senior Requirement Engineer | Requirement integrity, EPIC drift and traceability | `workflow create` | Senior System Architect | `.agents/roles/senior-requirement-engineer/SKILL.md` | active |
| Senior Security/Sandbox Engineer | Untrusted repository, filesystem and safe Git security | `workflow execute` | Senior System Architect | `.agents/roles/senior-security-sandbox-engineer.md` | active |
| Senior Swarm Orchestrator | Slice routing, branch coordination and quality handoff | `workflow execute` | Agent Workflow Orchestrator | `.agents/roles/senior-swarm-orchestrator.md` | active |
| Senior System Architect | Architecture boundaries and governance escalation | `skills-agents`; `workflow create`; `workflow execute` | Agent Workflow Orchestrator | `.agents/roles/senior-system-architect.md` | active |
| Senior Tester | Regression strategy, JUnit 6, ArchUnit and quality gates | `workflow create`; `workflow execute` | Workflow Executor | `.agents/roles/senior-tester.md` | active |
| Senior UX Designer | UX, accessibility and operational evidence workflows | `workflow execute` | Senior React Frontend Developer | `.agents/roles/senior-ux-designer.md` | active |
| Senior Workflow Architect | Executable workflow authoring and slice planning | `workflow create` | Senior System Architect | `.agents/roles/senior-workflow-architect/SKILL.md` | active |

## Reusable Codex Skills

Reusable `.codex/skills` entries remain general-purpose helpers. They must not
be used to override project-specific `.agents` rules.

| Name | Purpose | Process strand | Parent / Owner | Linked files / processes | Status |
|---|---|---|---|---|---|
| archunit-expert | ArchUnit architecture-rule support | `workflow execute` | Senior Tester | `.codex/skills/archunit-expert/SKILL.md` | active |
| hexagonal-architecture-expert | Hexagonal architecture support | `workflow execute` | Senior System Architect | `.codex/skills/hexagonal-architecture-expert/SKILL.md` | active |
| junit6-expert | JUnit 6 testing support | `workflow execute` | Senior Tester | `.codex/skills/junit6-expert/SKILL.md` | active |
| microservice-architecture-expert | Microservice autonomy support | `workflow execute` | Microservice Senior Expert | `.codex/skills/microservice-architecture-expert/SKILL.md` | active |
| protobuf-grpc-expert | Protobuf and gRPC support | `workflow execute` | Senior gRPC/Proto Specialist | `.codex/skills/protobuf-grpc-expert/SKILL.md` | active |
| workflow-executor | Reusable workflow execution entrypoint | `workflow execute` | Workflow Executor | `.codex/skills/workflow-executor/SKILL.md` | active |

## Callable Codex Agent Assignments

Callable agents use the same field model as skills. Their concrete inputs,
outputs, allowed files and gates come from the active strand default matrix and
their linked role or skill instructions.

| Name | Purpose | Process strand | Parent / Owner | Linked files / processes | Status |
|---|---|---|---|---|---|
| analytics_persistence_reviewer | Reviews analytics persistence, evidence and provenance | `workflow execute` | Senior Analysis Storage Architect | `.codex/agents/analytics_persistence_reviewer.toml` | active |
| architecture_forensic_analytics_architect | Reviews Forensic Analytics architecture, replay, graph and LLM risks | `workflow execute` | Senior System Architect | `.codex/agents/architecture_forensic_analytics_architect.toml` | active |
| architecture_reviewer | Reviews architecture boundaries and migration risks | `workflow execute` | Senior System Architect | `.codex/agents/architecture_reviewer.toml` | active |
| documentation_reviewer | Reviews documentation consistency | `skills-agents`; `workflow create`; `workflow execute` | Senior Documentation Engineer | `.codex/agents/documentation_reviewer.toml` | active |
| git_commit_operator | Performs staged commit, slice checkpoint push, push, PR and guarded `push auto` operations | `skills-agents`; `workflow execute` | Push Auto Guard / Workflow Executor | `.codex/agents/git_commit_operator.toml` | active |
| git_commit_reviewer | Reviews commit, slice checkpoint push, push and `push auto` readiness | `skills-agents`; `workflow execute` | Push Auto Guard / Workflow Executor | `.codex/agents/git_commit_reviewer.toml` | active |
| implementation_worker | Implements one approved workflow slice at a time | `workflow execute` | Workflow Executor | `.codex/agents/implementation_worker.toml` | active |
| ingestion_handoff_reviewer | Reviews engine-request and gRPC handoff contracts | `workflow execute` | Senior gRPC/Proto Specialist | `.codex/agents/ingestion_handoff_reviewer.toml` | active |
| joern_semantics_reviewer | Reviews Joern Docker and CPG semantic enrichment | `workflow execute` | Senior Joern/CPG Specialist | `.codex/agents/joern_semantics_reviewer.toml` | active |
| microservice_senior_expert | Reviews service autonomy and deployability | `workflow execute` | Microservice Senior Expert | `.codex/agents/microservice_senior_expert.toml` | active |
| quality_archunit_reviewer | Reviews JUnit 6, ArchUnit and coverage quality | `workflow execute` | Senior Tester | `.codex/agents/quality_archunit_reviewer.toml` | active |
| quality_reviewer | Reviews tests, coverage and build verification | `workflow execute` | Senior Tester | `.codex/agents/quality_reviewer.toml` | active |
| replay_graph_llm_reviewer | Reviews replay, graph, reporting and LLM packages | `workflow execute` | Senior System Architect | `.codex/agents/replay_graph_llm_reviewer.toml` | active |
| repository_explorer | Performs read-only repository exploration | `skills-agents`; `workflow create`; `workflow execute` | Active strand owner | `.codex/agents/repository_explorer.toml` | active |
| security_reviewer | Reviews security-sensitive changes and test isolation | `workflow execute` | Senior Security/Sandbox Engineer | `.codex/agents/security_reviewer.toml` | active |
| senior_analysis_storage_architect | Reviews analysis storage architecture | `workflow execute` | Senior System Architect | `.codex/agents/senior_analysis_storage_architect.toml` | active |
| senior_devops | Reviews Gradle, Docker, CI/CD and deployment slices | `workflow execute` | Senior Swarm Orchestrator | `.codex/agents/senior_devops.toml` | active |
| senior_documentation_engineer | Reviews documentation, skill audits and workflow handoff | `skills-agents`; `workflow create`; `workflow execute` | Documentation Governance | `.codex/agents/senior_documentation_engineer.toml` | active |
| senior_git_workspace_specialist | Reviews workspace lifecycle and checkout preparation | `workflow execute` | Senior Swarm Orchestrator | `.codex/agents/senior_git_workspace_specialist.toml` | active |
| senior_grpc_proto_specialist | Reviews gRPC, Protobuf and compatibility | `workflow execute` | Senior Java Backend Developer | `.codex/agents/senior_grpc_proto_specialist.toml` | active |
| senior_java_backend | Implements and reviews backend slices | `workflow execute` | Workflow Executor | `.codex/agents/senior_java_backend.toml` | active |
| senior_joern_cpg_specialist | Reviews Joern CPG planning | `workflow execute` | Senior Java Backend Developer | `.codex/agents/senior_joern_cpg_specialist.toml` | active |
| senior_performance_engineer | Reviews scalability and performance risks | `workflow execute` | Senior System Architect | `.codex/agents/senior_performance_engineer.toml` | active |
| senior_plugin_integration_developer | Reviews plugin producer handoff | `workflow execute` | Senior Java Backend Developer | `.codex/agents/senior_plugin_integration_developer.toml` | active |
| senior_react_frontend | Implements and reviews React frontend slices | `workflow execute` | Workflow Executor | `.codex/agents/senior_react_frontend.toml` | active |
| senior_requirement_engineer | Reviews EPIC and requirement consistency | `workflow create` | Senior System Architect | `.codex/agents/senior_requirement_engineer.toml` | active |
| senior_security_sandbox_engineer | Reviews sandboxing, filesystem and secret leakage risks | `workflow execute` | Senior System Architect | `.codex/agents/senior_security_sandbox_engineer.toml` | active |
| senior_swarm_orchestrator | Coordinates slice routing and conflict management | `workflow execute` | Agent Workflow Orchestrator | `.codex/agents/senior_swarm_orchestrator.toml` | active |
| senior_system_architect | Reviews architecture and microservice boundaries | `skills-agents`; `workflow create`; `workflow execute` | Agent Workflow Orchestrator | `.codex/agents/senior_system_architect.toml` | active |
| senior_tester | Reviews regression tests and quality gates | `workflow create`; `workflow execute` | Workflow Executor | `.codex/agents/senior_tester.toml` | active |
| senior_ux_designer | Reviews UX and accessibility | `workflow execute` | Senior React Frontend Developer | `.codex/agents/senior_ux_designer.toml` | active |
| senior_workflow_architect | Creates checked workflow structure and slice plans | `workflow create` | Senior System Architect | `.codex/agents/senior_workflow_architect.toml` | active |
| source_analysis_reviewer | Reviews static source analysis and unresolved symbols | `workflow execute` | Senior Java Backend Developer | `.codex/agents/source_analysis_reviewer.toml` | active |
| swarm_orchestrator | Coordinates read-only findings into an implementation plan | `workflow execute` | Senior Swarm Orchestrator | `.codex/agents/swarm_orchestrator.toml` | active |

No skill, role or agent is deprecated by this registry update. Existing overlaps
are kept as explicit collaboration links unless a later `skills-agents` strand
review can prove with high confidence that a role should be merged or
deprecated.
