# Conflicts Resolved

## Summary

No existing `.agents/skills/*/SKILL.md` file contained a confirmed outdated Java, Gradle, JUnit, Spring Boot, plugin-as-platform, parser-in-plugin or local-analysis-first rule. Because the 95-percent rule only permits automatic correction when the target is clear, existing skill bodies were kept.

The following gaps and stale workflow surfaces were resolved because the current repository rules, `QUALITY.md`, build files and user-provided platform direction made the target state clear.

| Conflict or gap | Affected files | Change made | Why confidence was at least 95 percent |
|---|---|---|---|
| Missing distributed-platform skill for worker/job lifecycle, retry, leasing, backpressure and failure recovery. | `.agents/skills/distributed-systems-architect/SKILL.md` | Added a dedicated skill. | The new platform phase explicitly requires worker/workspace-capable architecture and long-running analysis jobs. |
| Missing workspace lifecycle skill for server-side repository checkout before parser execution. | `.agents/skills/workspace-lifecycle-specialist/SKILL.md` | Added a dedicated skill. | The requested flow puts workspace creation and Git checkout before parsers. Existing skills did not own this lifecycle directly. |
| Missing large Git repository skill for WildFly-scale checkout hardening. | `.agents/skills/git-large-repository-specialist/SKILL.md` | Added a dedicated skill. | WildFly hardening is explicitly requested and limited to clone, checkout, source-root detection and cleanup. |
| Missing gRPC streaming and Protobuf evolution skill for the next plugin-to-server contract. | `.agents/skills/grpc-streaming-specialist/SKILL.md` | Added a dedicated skill. | The repository already contains gRPC/Protobuf modules and the requested target flow is gRPC-based. |
| Missing analysis storage skill for raw ingestion, normalized stores, artifacts and projections. | `.agents/skills/analysis-storage-architect/SKILL.md` | Added a dedicated skill. | Existing persistence review skills are review-oriented; the new workplan needs a storage architecture owner. |
| Missing replay/runtime correlation planning skill. | `.agents/skills/replay-runtime-correlation-specialist/SKILL.md` | Added a dedicated skill. | Replay is part of the platform direction but must remain later and evidence-based. |
| Missing Joern/CPG specialist skill distinct from the existing Joern semantic review skill. | `.agents/skills/code-property-graph-joern-specialist/SKILL.md` | Added a dedicated skill. | The platform direction names Joern and CPG as analytics-side later capabilities with large-project concerns. |
| Missing performance/scalability skill for large repositories and future workers. | `.agents/skills/performance-scalability-engineer/SKILL.md` | Added a dedicated skill. | WildFly hardening, worker readiness and repository checkout metrics require a dedicated performance owner. |
| Missing swarm coordination specialist skill for dependency graph planning and handoff. | `.agents/skills/agent-swarm-coordination-specialist/SKILL.md` | Added a dedicated skill. | The new workplan requires explicit multi-agent coordination and review sequencing. |
| Missing security sandbox skill for untrusted repository checkout and safe Git operations. | `.agents/skills/security-sandbox-specialist/SKILL.md` | Added a dedicated skill. | The requested phase executes server-side checkout of external repositories and explicitly calls out sandbox/workspace isolation. |
| Missing senior roles for the new platform phase. | `.agents/roles/*.md`, `.codex/agents/*.toml`, `.agents/orchestrator/routing-rules.md` | Added senior gRPC/Proto, Git/Workspace, Plugin Integration, Documentation, Security/Sandbox, Performance, Analysis Storage and Joern/CPG roles and routing. | The requested role list required these responsibilities or clear equivalents; existing roles covered only the earlier generic set. |
| Old `docs/workplan/` described a completed distributed-orchestrator baseline and included parser/Joern/BTM/graph/report worker planning as near-term workflow material. | `docs/workplan/*` | Removed the old directory content and created a new workspace/gRPC workplan. | The user explicitly stated the old workplan is completed and must be deleted and replaced. |

## Existing Skills Kept

Existing skills were kept when they remained compatible with the verified baseline and the requested phase. Overlaps are documented in [skill-inventory.md](skill-inventory.md) instead of merging skills, because the overlapping skills have distinct review or implementation responsibilities.
