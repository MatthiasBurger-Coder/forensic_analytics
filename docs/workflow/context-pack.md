# Workflow Context Pack

- Workflow: GOV-02 and GOV-04 Role Inventory Validation and Skill Schema Standardization
- Workflow ID: `gov-02-04-role-inventory-skill-schema`
- Version: `gov-02-04-role-inventory-skill-schema-v1`
- Branch: `feature/workflow-gov-02-04-20260814`
- Process strand: `workflow execute`
- Execution profile: `FULL_PATH`
- Affected: `.agents/skills/**`, `.agents/roles/**`, `docs/agents/**`, `docs/skill-audit/**`, `docs/workflow/**` and checked governance-risk notes only
- Forbidden: product implementation, service contracts, persistence, runtime, Docker, frontend behavior, build logic and `push auto`
- Required roles: Senior Requirement Engineer, Senior System Architect, Senior Java Backend Developer, Senior React Frontend Developer, Senior Tester
- Conditional roles: Senior Workflow Architect, Senior Documentation Engineer, Skill Registry Conflict Auditor
- Quality: targeted deterministic governance checks, strict Gradle test command per slice, full local gate in S05 and `git diff --check`
- Completed slices: S01; execution continues with S02
- Registry status: derived cache; S04 refreshes registry and audit evidence from verified sources
- Arc42 status: checked; update only when execution verifies a governance-risk transition
- ADR references: ADR-0015 and ADR-0021

Hashes and machine-readable provenance are recorded in
[`context-pack.json`](context-pack.json). This pack is a navigation aid and
becomes stale when a recorded governing file, workflow scope or governance
rule changes.
