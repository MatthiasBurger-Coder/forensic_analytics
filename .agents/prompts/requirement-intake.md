# Requirement Intake Prompt

Use before workflow authoring when a user introduces or changes a requirement.

## Required Flow

1. Load `three-amigos-requirement-gatekeeper`.
2. Normalize the requirement.
3. Separate confirmed facts from assumptions.
4. Validate business goal, technical goal, scope and non-goals.
5. Validate architecture fit.
6. Validate quality and testability.
7. Validate dependency and deadlock risks.
8. Validate required skills and role reviews.
9. Run the Requirement Clarification Loop and record original request,
   interpreted intent, change type, affected process strand, affected
   architecture area, explicit requirements, implicit requirements,
   assumptions, non-goals, risks, open questions, blocking questions and
   confidence level.
10. Return `READY_FOR_WORKFLOW`, `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` or
    `REQUIRES_REFINEMENT`.

## Decision Rules

- Confidence >= 90% returns `READY_FOR_WORKFLOW` when no blocking questions remain.
- Confidence 70-89% returns `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when every assumption is non-blocking and documented.
- Confidence < 70% returns `REQUIRES_REFINEMENT`.

## Stop Conditions

Return `REQUIRES_REFINEMENT` when continuing would require guessing requirements, ownership, API contracts, data ownership, quality commands, architecture decisions, runtime facts or evidence semantics.

Return `REQUIRES_REFINEMENT` while any blocking question remains open. Blocking
questions prevent final checked `docs/workflow/workflow.md` creation and
release for `workflow execute`.
