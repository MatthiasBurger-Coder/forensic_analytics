# Agent Handoff Matrix

## Purpose

This matrix defines expected handoffs between workflow roles and skills.

## Matrix

| From | To | Handoff trigger | Required artifacts |
| --- | --- | --- | --- |
| Three Amigos Requirement Gatekeeper | Workflow Authoring | `READY_FOR_WORKFLOW` | normalized requirement, acceptance criteria, role map, blockers |
| Workflow Authoring | Workflow Executor | executable workflow approved for execution | slice plan, dependencies, write scopes, quality gates |
| Workflow Executor | Skill Registry & Conflict Auditor | before new skill creation or governance edits | active slice, affected skills, expected outputs |
| Skill Registry & Conflict Auditor | Specialist reviewer | conflict found | conflict type, affected files, required resolution |
| Senior Swarm Orchestrator | Agent Handoff Protocol | parallel or delegated work begins | owner map, disjoint write scopes, merge order |
| Contract-First API Steward | Senior gRPC/Proto Specialist | gRPC/protobuf contract change | proto files, message semantics, compatibility report |
| Data Ownership & Persistence Steward | Security & Threat Modeling | sensitive data or cross-boundary flow | data ownership report, sensitivity assessment |
| Observability & Runtime Diagnostics | Security & Threat Modeling | logging or telemetry contains sensitive context | observability check, redaction needs |
| Quality Gate Orchestrator | Release & Branch Governance | commit or push readiness | quality result report, failed or skipped checks |
| Release & Branch Governance | Git commit preparation skills | commit explicitly allowed | changed-file scope, message draft, validation evidence |

## Required Handoff Fields

- `source_agent`
- `target_agent`
- `slice_id`
- `input_artifacts`
- `output_artifacts`
- `assumptions`
- `known_risks`
- `blockers`
- `validation_status`
- `next_action`

## Rules

- Every handoff has one source and one target owner.
- Reviewers may block but must provide resolution steps.
- Handoffs must not hide unresolved assumptions.
- File ownership and merge order are required for parallel work.
