# Subagents

The implementation should use subagents as planned roles. Actual delegation depends on the active Codex workflow and repository state, but the responsibilities below are binding for the future implementation plan.

## Senior System Architect

Responsibility: preserve hexagonal architecture, dependency direction and evidence boundaries.

Slices: 1, 2, 3, 5, 8, 12.

Coordination: works with the gRPC/Proto Specialist on DTO boundaries, the Java Backend Developer on application services and the Documentation Engineer on architecture text.

Artifacts: architecture review notes, boundary decisions, risk list for any service or port naming mismatch.

## Senior Java Backend Developer

Responsibility: implement domain and application services with Java 25, JUnit 6 and repository style.

Slices: 2, 3, 5, 8, 9, 12.

Coordination: works with the Git/Workspace Specialist for port contracts, the Tester for regression coverage and the System Architect for dependency direction.

Artifacts: domain models, application services, fake-port tests, integration wiring notes.

## Senior DevOps Engineer

Responsibility: verify Gradle, local bootstrap, opt-in hardening execution and resource controls.

Slices: 10, 11, 12.

Coordination: works with the Tester on hardening execution, the Git/Workspace Specialist on disk and timeout behavior and the Documentation Engineer on run instructions.

Artifacts: hardening run configuration, timeout and disk-space notes, quality-gate evidence.

## Senior Tester

Responsibility: design and execute regression-first test coverage.

Slices: 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12.

Coordination: works with every implementation role and reviews testability before implementation begins.

Artifacts: mini test repository scenario, fake gRPC tests, adapter tests, source-root fixtures, hardening test checklist.

## Senior gRPC/Proto Specialist

Responsibility: protect gRPC compatibility, Protobuf field numbering, DTO mapping and transport validation.

Slices: 1, 2, 6, 7.

Coordination: works with the Plugin Integration Developer on client compatibility, the System Architect on boundary rules and the Tester on mapper/validator tests.

Artifacts: proto contract review, mapper coverage, compatibility notes.

## Senior Git/Workspace Specialist

Responsibility: design Git operations, workspace layout, cleanup, timeout behavior and large-repository handling.

Slices: 3, 4, 5, 7, 9, 10, 11.

Coordination: works with the Java Backend Developer on ports, the DevOps Engineer on resource controls and the Tester on local repository fixtures.

Artifacts: Git port contract, adapter behavior matrix, workspace lifecycle checklist, WildFly hardening metrics.

## Senior Plugin Integration Developer

Responsibility: keep the plugin as a producer/client and prevent analysis logic from moving into the plugin.

Slices: 6, 7, 12.

Coordination: works with the gRPC/Proto Specialist on generated client compatibility, the Tester on fake-server tests and the Documentation Engineer on plugin-side behavior notes.

Artifacts: plugin request builder, client error handling, plugin boundary review.

## Senior Documentation Engineer

Responsibility: keep workplan, README, architecture and operational documentation aligned with the implementation.

Slices: 1, 10, 11, 12 and documentation review for every slice.

Coordination: works with the System Architect for architecture wording, the DevOps Engineer for commands and the Agent Swarm Orchestrator for status tracking.

Artifacts: updated docs, slice status, quality evidence summary, commit notes.

## Senior Agent Swarm Orchestrator

Responsibility: coordinate dependencies, parallel work groups, review handoffs and final readiness.

Slices: all slices, with strongest ownership of 4, 7, 10, 11 and 12 coordination.

Coordination: receives findings from every role and ensures no worker changes files outside its assigned responsibility.

Artifacts: dependency map, parallel execution status, review checklist, final readiness report.
