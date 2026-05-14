---
name: analytics-slice-workflow
description: Creates implementation slices for Forensic Analytics features such as ingestion, repository source analysis, Joern semantic artifacts, persistence, graph, replay, reports, and LLM context preparation.
---

# Skill: Analytics Slice Workflow

## Description
Creates implementation slices for Forensic Analytics features such as ingestion, repository source analysis, Joern semantic artifacts, persistence, graph, replay, reports, and LLM context preparation.

## Instructions
1. Read AGENTS.md and QUALITY.md.
2. Identify the affected forensic analytics capability.
3. Identify the domain model, application use case, port, adapter, and documentation impact.
4. Identify ingestion, persistence, graph, replay, report, or LLM evidence impact.
5. Identify required JUnit 6, ArchUnit, fixture, and integration tests.
6. Preserve evidence categories and uncertainty markers.
7. Produce small implementation slices with verification commands.

## Expected Inputs
- user task
- affected source files
- AGENTS.md
- QUALITY.md
- README or docs
- current workflow file

## Expected Outputs
- capability impact map
- ordered slice plan
- affected files per slice
- test plan
- verification commands
- open risks

## Stop Conditions
Stop if:
- capability ownership is unclear
- domain/application boundary would be violated
- evidence would need to be guessed or fabricated
- graph, replay, LLM, or report behavior cannot be verified
