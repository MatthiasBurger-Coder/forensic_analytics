# Workflow Context Pack

- Workflow: GOV-01–GOV-05 Governance Registry and Agent Definition Reconciliation
- Version: 1
- Branch: `feature/workflow-gov-skill-governance-20260814`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Affected: `.agents`, `.codex`, governance docs, skill audit, workflow docs and checked arc42 notes
- Forbidden: product code, build/runtime/contracts/persistence and `push auto`
- Required roles: Senior Requirement Engineer, Senior System Architect, Senior Java Backend Developer, Senior React Frontend Developer, Senior Tester
- Conditional roles: Senior Workflow Architect, Senior Documentation Engineer, Skill Registry Conflict Auditor, Flowchart Integrity Auditor
- Quality: strict Gradle test command, targeted deterministic governance checks and `git diff --check`
- Baseline: 77 skills, 19 role files, 6 reusable Codex skills, 34 Codex agents
- Cache discrepancy: registry records 18 project roles; GOV-02 reconciles this
- Hashes are in `context-pack.json`; the pack is stale if governing files or the workflow change.
