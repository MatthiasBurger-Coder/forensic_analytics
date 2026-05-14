# 08 - Quality, CI, and Rollout

Status: completed contract-baseline slice.

## Objective

Record quality coverage, CI status, rollout order, and migration controls for the completed contract baseline.

## Verified Current Baseline

- `QUALITY.md` defines the minimum command as `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- `QUALITY.md` defines the full local quality gate as `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- The repository uses Java 25, Gradle 9.4.0, JUnit 6, ArchUnit, JaCoCo, strict dependency verification, and evidence-first quality rules.
- No `.github/workflows` directory is present, so CI enforcement is not implemented in this repository state.
- Queue, worker, store, projection, and server-facing contracts are implemented as a technology-neutral baseline; concrete distributed runtime behavior remains planned, not implemented.

## Future Target

- Each implementation slice has targeted regression tests before behavior changes.
- Future CI should run the minimum quality command and protect architecture boundaries, evidence semantics, and dependency verification after CI workflows are introduced.
- Full local gate should run before integration or release when feasible.
- Rollout keeps the synchronous path available until distributed orchestration is verified end to end.

## Completed Contract Baseline

- Domain tests cover job lifecycle, retry/dead-letter provenance, projection availability, and LLM output labeling.
- Application tests cover typed worker contracts, server-facing status views, and provider-neutral architecture boundaries.
- Persistence tests cover in-memory job storage, deterministic ordering, idempotent artifact storage, and explicit artifact metadata conflicts.
- The existing synchronous repository analysis flow remains covered by `DefaultRunRepositoryAnalysisUseCaseTest` and was not changed.
- No quality threshold, dependency verification rule, CI workflow, runtime bootstrap, provider dependency, or module definition was changed.

## Subagent Roles

- Quality reviewer: select targeted tests and full gate expectations.
- Architecture reviewer: verify ArchUnit and module boundaries.
- Implementation worker: keep changes small and report commands run.
- Documentation reviewer: keep workplan and quality docs aligned with implemented behavior.
- Security reviewer: review sensitive runtime data flows when storage, LLM, graph, or vector slices change.

## Implementation Steps

1. Inspect `QUALITY.md`, Gradle tasks, CI workflows, and existing tests before changing quality behavior.
2. Add or update targeted regression tests per slice before implementation.
3. Add architecture tests when new modules, adapters, or provider boundaries are introduced.
4. Run narrow tests first, then minimum quality command, then full local gate where feasible.
5. Keep migration reversible until distributed orchestration reaches parity with synchronous analysis.

## Affected Files or Modules to Inspect

- `QUALITY.md`
- `AGENTS.md`
- `settings.gradle.kts`
- `.github/workflows`, if present
- `forensic-analytics-application/src/test`
- `forensic-analytics-domain/src/test`
- adapter and persistence test modules touched by each slice

## Evidence and Provenance Rules

- Tests must verify explicit unknown, incomplete, failed, retry, and projection states where implemented.
- Quality gates must not allow graph, report, vector, or LLM projections to become primary evidence.
- Test fixtures may be synthetic only when clearly named as fixtures.

## Stop Conditions

Stop and report if:

- a documented Gradle task cannot be found;
- CI and `QUALITY.md` disagree about required verification;
- dependency verification fails because a new dependency was added without review;
- coverage or ArchUnit rules would need to be weakened.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

For documentation-only work, run:

```bash
git diff --check
```

## Done Criteria

- Each implemented contract slice has targeted verification evidence.
- Local commands are aligned with `QUALITY.md`; CI workflows are not present in this repository state.
- No quality threshold, dependency verification rule, or architecture rule was weakened.
- Rollout state clearly says which contract parts are implemented and which runtime parts remain planned.
