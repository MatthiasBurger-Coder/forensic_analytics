# Quality And Leakage Gates

## Quality Authority

`QUALITY.md` is authoritative for repository quality gates.

Minimum repository command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Verified frontend commands:

```bash
cd forensic-ui && npm run test -- src/pages/workspaces/WorkspaceListPage.test.tsx
cd forensic-ui && npm run test -- src/pages/workspaces/WorkspaceListPage.test.tsx src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts
cd forensic-ui && npm run build
```

## Slice Gates

| Slice | Targeted Checks | Required Checks |
|---|---|---|
| S01 | Read-only contract and DTO verification | None |
| S02 | `cd forensic-ui && npm run test -- src/pages/workspaces/WorkspaceListPage.test.tsx` | `cd forensic-ui && npm run build` |
| S03 | Workspace list and optional mapper/API Vitest commands | `cd forensic-ui && npm run build` |
| S04 | Frontend targeted checks | `./gradlew test --dependency-verification strict --console=plain --stacktrace` |

The full local quality gate is required before commit readiness or publication.

## Leakage Rules

The implementation and tests must verify that UI branch selection does not
render or derive options from:

- private checkout paths;
- H2 paths or JDBC URLs;
- raw Git stdout or stderr;
- credentials, tokens, secrets or authorization headers;
- browser Git state;
- `defaultBranch` when the branch is absent from the selected workspace's
  public `branches[]`;
- local row index or branch text without the opaque `workspaceBranchId`.

## Evidence Integrity Rules

- Branch names are data values only.
- Selected branch is operator intent, not confirmed execution evidence.
- Confirmed branch state comes only from repository-source public branch
  records, including status, resolved commit and source snapshot.
- Stale branch options must remain tied to the existing stale/error UI state.
- Missing branch records must be shown as unavailable, not guessed.

## STOP Conditions

Stop and report if:

- the verified quality command cannot be run from WSL;
- npm scripts or Gradle tasks differ from the documented names;
- tests need branch options not present in public fixtures;
- implementation needs a backend route or contract not in this workflow;
- a quality failure cannot be classified as related or unrelated.
