# Slice Title

Status: planned slice. Update this status only when the slice is actively implemented or completed.

## Objective

State the smallest user-visible and architecture-visible outcome.

## Verified Current Baseline

- List only facts verified from source, tests, build files, docs, ADRs, or explicit user-provided verified facts.
- Mark missing modules, ports, statuses, adapters, schemas, or tasks as not found instead of assuming replacements.

## Future Target

- Describe planned behavior.
- Mark every unimplemented concept as planned.
- Do not select concrete queue, database, graph, vector, object-store, or server technology without ADR and dependency review.

## Subagent Roles

- Planning reviewer:
- Implementation worker:
- Quality reviewer:
- Documentation reviewer:

## Implementation Steps

1. Perform read-only verification.
2. Write or update regression tests when behavior changes.
3. Implement the smallest change.
4. Update documentation only where verified behavior changes.
5. Run the relevant verification commands.

## Affected Files or Modules to Inspect

- `AGENTS.md`
- `QUALITY.md`
- `settings.gradle.kts`
- Relevant production module:
- Relevant test module:
- Relevant ADRs:
- Relevant docs:

## Evidence and Provenance Rules

- Preserve evidence source, analysis run identity, source snapshot identity, artifact references, ordering, and completeness state where available.
- Keep generated hypotheses, reports, graph projections, and vector projections separate from primary evidence.
- Represent unknown, incomplete, unresolved, skipped, and failed states explicitly.

## Stop Conditions

Stop and report if:

- a required symbol, module, Gradle task, field, status, schema, or artifact path cannot be verified;
- implementation requires a concrete technology choice without ADR and dependency review;
- behavior would fabricate evidence or infer missing runtime facts;
- edits would exceed the approved write scope.

## Verification Commands

```bash
git diff --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Use narrower targeted tests first when a slice touches implementation code.

## Done Criteria

- Current baseline was verified and reported.
- Tests or documented rationale cover changed behavior.
- Evidence and provenance rules are preserved.
- Verification commands were run or blockers were reported.
- Changed files stay within the approved write scope.
