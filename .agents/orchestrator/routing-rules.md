# Routing Rules

Use these rules to select roles for a slice. Prefer the smallest set that covers the actual files and risks.

## Role Routing

- Backend domain, application, persistence, static analysis, runtime ingestion, gRPC or Protobuf work routes to `roles/senior-java-backend.md`.
- React, frontend state, API client integration or UI component work routes to `roles/senior-react-frontend.md`.
- Information architecture, accessibility, visualization UX or user-flow work routes to `roles/senior-ux-designer.md`.
- Cross-module design, package boundaries, architecture tests or module restructuring routes to `roles/senior-system-architect.md`.
- Microservice boundaries, service autonomy, independent deployability or no-shared-code service reviews route to `roles/microservice-senior-expert.md`.
- Bounded-context service decomposition and "technical module is not a microservice" decisions route to `skills/service-decomposition-bounded-context/SKILL.md`.
- Cross-service REST/OpenAPI, gRPC/protobuf or event contract governance routes to `skills/contract-governance-expert/SKILL.md`.
- Production microservice migration safety, rollback, strangler strategy or multi-service risk gates route to `skills/microservice-migration-safety-gate/SKILL.md`.
- Microservice runtime independence, healthcheck, observability or container-readiness evidence routes to `skills/microservice-runtime-readiness-expert/SKILL.md`.
- Test strategy, regression coverage, ArchUnit, coverage gates or quality failures route to `roles/senior-tester.md`.
- Gradle, Docker, Kubernetes, CI, observability or deployment work routes to `roles/senior-devops.md`.
- New workflow creation, checked `docs/workflow/workflow.md`, arc42 review, slice dependency planning or planning-risk review routes to `roles/senior-workflow-architect/SKILL.md`.
- EPIC consistency, requirement drift, requirement classification, assumption tracking or requirement-to-architecture synchronization routes to `roles/senior-requirement-engineer/SKILL.md`.
- Incoming requirement gatekeeping before workflow authoring, Three Amigos review, acceptance-criteria validation, dependency/deadlock checks or `READY_FOR_WORKFLOW` versus `REQUIRES_REFINEMENT` decisions route to `skills/three-amigos-requirement-gatekeeper/SKILL.md`.
- Skills, roles, prompts, Codex agent definitions, organigramm, skill registry, process documentation or `push auto` guard changes route to `skills/skill-registry-conflict-auditor/SKILL.md` and `roles/senior-documentation-engineer.md`.
- `workflow create` must route backend impact to `roles/senior-java-backend.md`, frontend impact to `roles/senior-react-frontend.md`, architecture and arc42 impact to `roles/senior-system-architect.md`, and test strategy to `roles/senior-tester.md`.
- `workflow execute` backend slices must include Senior Java Backend Developer, Microservice Senior Expert when service boundaries are affected, hexagonal architecture review, Spring review when Spring wiring is affected, JUnit 6 testing review and Docker/container review when container readiness is affected.
- `workflow execute` frontend slices must include Senior React Frontend Developer, Senior UX Designer and Docker/container review when container readiness is affected.
- Multi-role coordination, conflict resolution or slice planning routes to `roles/senior-swarm-orchestrator.md`.
- Protobuf contracts, streaming RPC design, request validation or gRPC compatibility route to `roles/senior-grpc-proto-specialist.md`.
- Repository checkout, workspace lifecycle, source-root preparation or large Git repositories route to `roles/senior-git-workspace-specialist.md`.
- Plugin producer handoff, plugin-side request construction or plugin-to-server communication routes to `roles/senior-plugin-integration-developer.md`.
- Documentation, skill-audit material, existing workflow updates or ADR alignment notes route to `roles/senior-documentation-engineer.md`.
- Untrusted repository handling, sandboxing, safe Git operations or secret leakage risks route to `roles/senior-security-sandbox-engineer.md`.
- Performance budgets, large repository metrics, timeouts, quotas or scalability testing route to `roles/senior-performance-engineer.md`.
- Analysis-session storage, raw ingestion storage, artifact storage or projection boundaries route to `roles/senior-analysis-storage-architect.md`.
- Joern, Code Property Graph, semantic artifact handling or CPG large-project planning route to `roles/senior-joern-cpg-specialist.md`.

## Escalation

Stop and report when a required file, task, symbol, schema, command or contract cannot be verified exactly.

Use `QUALITY.md` for verification commands. Optional external checks such as Sonar require documented credentials and must be reported when skipped.
