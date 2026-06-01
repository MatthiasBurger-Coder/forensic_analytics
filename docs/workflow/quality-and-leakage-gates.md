# Quality And Leakage Gates

## Required Minimum

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Targeted Backend

```bash
./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
```

## Targeted Frontend

```bash
npm test -- --run src/pages/workspaces/CreateWorkspacePage.test.tsx src/pages/workspaces/WorkspaceListPage.test.tsx src/adapters/api/mappers.test.ts
npm run build
```

## Full Local Gate

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Leakage Checks

- No raw Git output in public diagnostics.
- No local workspace paths in UI or public DTOs.
- No credentials or repository URLs with userinfo in diagnostics.
- No branch name used as filesystem path.
- No branch selection test may perform a fetch, checkout or workspace-content
  update.
- Status-only checks may mark a branch with a red not-up-to-date indicator
  without loading branch content.
- No analysis-result deletion without owner contract.
