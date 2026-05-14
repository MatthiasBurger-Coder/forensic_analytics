# 03 - Subagents

| Subagent | Responsibility | Assigned slices | Coordination | Delivered artifacts | Review responsibility |
|---|---|---|---|---|---|
| Senior System Architect | Preserve hexagonal boundaries, module direction, ports/adapters and target architecture. | 1, 3, 12 | Coordinates with Java Backend, gRPC/Proto and Git/Workspace specialists. | Architecture notes, boundary decisions, stop reports for unverifiable symbols. | Reviews architecture-sensitive slices. |
| Senior Java Backend Developer | Implement domain models, application services, ports and adapter integration in Java 25. | 2, 3, 4, 5, 7, 8, 9 | Works with Tester, Storage Architect and Git/Workspace Specialist. | Minimal backend changes, unit tests, integration wiring. | Reviews application/domain implementation quality. |
| Senior DevOps Engineer | Verify Gradle, wrapper, CI-equivalent commands, runtime environment and optional infrastructure. | 10, 11, 12 | Works with Performance and Security for WildFly hardening. | Environment notes, Gradle command verification, CI risks. | Reviews quality-command feasibility. |
| Senior Tester | Define regression, contract, in-process gRPC and integration tests. | 1, 2, 3, 7, 8, 9, 12 | Works with all implementation roles before broad quality gates. | Test plan, targeted tests, quality-gate evidence. | Reviews acceptance criteria and residual risk. |
| Senior gRPC/Proto Specialist | Own Protobuf evolution, gRPC mapping, validation, streaming extensions and compatibility. | 1, 2, 6 | Coordinates with Plugin Integration and Java Backend. | Proto contract, mapping checklist, compatibility notes. | Reviews gRPC and Protobuf slices. |
| Senior Git/Workspace Specialist | Own workspace lifecycle, safe Git operations, checkout result, source-root metadata and cleanup. | 3, 4, 5, 7, 9, 10, 11 | Coordinates with Security, Performance and Storage. | Git port plan, workspace policy, checkout hardening notes. | Reviews Git/workspace slices. |
| Senior Plugin Integration Developer | Own plugin-side request construction, client behavior, response handling and producer boundary. | 6, 7 | Coordinates with gRPC/Proto and Documentation. | Plugin handoff checklist, client integration plan. | Reviews plugin producer boundary. |
| Senior Documentation Engineer | Keep workplan, skill audit, handoff docs and ADR follow-up notes consistent. | 1, 6, 10, 12 | Coordinates with Orchestrator and reviewers. | Updated docs and manual-review records. | Reviews documentation consistency. |
| Senior Agent Swarm Orchestrator | Coordinate dependencies, file ownership, parallel groups, reviews and commit readiness. | all | Coordinates all roles and routes blockers. | Slice board, owner map, conflict log, handoff summary. | Reviews workflow completeness. |
| Senior Security/Sandbox Engineer | Own untrusted repository handling, sandbox boundaries, safe Git, path validation and secret safety. | 4, 5, 7, 10, 11 | Coordinates with Git/Workspace, DevOps and Performance. | Sandbox checklist, safe cleanup rules, security findings. | Reviews security-sensitive slices. |
| Senior Performance Engineer | Own checkout metrics, resource quotas, timeout behavior, large repository baseline and scalability risks. | 4, 9, 10, 11 | Coordinates with Git/Workspace and DevOps. | Metrics plan, WildFly hardening report, quota notes. | Reviews performance-sensitive slices. |
| Senior Analysis Storage Architect | Own session persistence, raw request storage, artifact references, indexing and projection boundaries. | 5, 8, 12 | Coordinates with Java Backend and Persistence Reviewer. | Storage model notes, analysis-session persistence criteria. | Reviews storage and provenance slices. |
| Senior Joern/CPG Specialist | Keep Joern/CPG planning analytics-side and out of the current workspace/gRPC implementation. | 9, 10, 12 | Coordinates with Source Analysis Reviewer and Documentation. | Joern/CPG deferral notes and future prerequisites. | Reviews that Joern is not accidentally executed. |

## Coordination Rules

- The orchestrator fixes shared contract ownership before parallel implementation.
- Write-capable workers must have disjoint file scopes.
- Reviewers may run in parallel with implementation when they do not block the immediate next local step.
- Any conflict between plugin and analytics responsibilities is documented before implementation continues.
