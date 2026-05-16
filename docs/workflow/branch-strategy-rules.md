# Branch Strategy Rules For `workflow create`

## Required Prefixes

`workflow create` may use only these prefixes:

```text
feature/
fix/
docs/
architecture/
```

The default is:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
```

Special prefixes are used only when the scope clearly matches:

- `fix/` for a concrete defect fix.
- `docs/` for clearly documentation-only work.
- `architecture/` for clearly architecture, system-structure or binding
  architecture-rule work.

Everything else uses `feature/`.

## Disallowed Workflow Prefixes

Do not use these prefixes for `workflow create` unless repository governance is
explicitly changed to allow them:

```text
feat/
refactor/
test/
build/
ci/
quality/
agent/
chore/
codex/
```

These prefixes may still exist for normal development work. This rule is scoped
to automatic workflow creation.

## Decision Order

```text
Concrete defect fix?
  -> fix/workflow-<short-topic>-<yyyyMMdd>

Clearly documentation-only?
  -> docs/workflow-<short-topic>-<yyyyMMdd>

Clearly architecture, system structure or architecture rules?
  -> architecture/workflow-<short-topic>-<yyyyMMdd>

Everything else:
  -> feature/workflow-<short-topic>-<yyyyMMdd>
```

When a special category is unclear, use `feature/` only when doing so does not
hide a known fix, docs-only or architecture-only scope.

## Required Branch Decision Output

Before creating a workflow branch, record:

```text
Detected workflow branch prefix: <feature|fix|docs|architecture>
Reason: <short reason>
Proposed branch name: <prefix>/workflow-<short-topic>-<yyyyMMdd>
```

## Short Topic Rules

The `<short-topic>` value is derived from the user request:

1. Use lower-case ASCII.
2. Use kebab-case.
3. Do not use spaces.
4. Do not use umlauts or special characters.
5. Do not force a ticket number unless the project requires one.
6. Describe the work specifically.
7. Do not use vague names such as `update`, `misc`, `stuff`, `new` or
   `workflow`.

The `workflow-` prefix and `yyyyMMdd` date suffix are mandatory.

## Examples

```text
feature/workflow-git-branch-strategy-20260516
feature/workflow-grpc-ingestion-20260516
fix/workflow-branch-conflict-check-20260516
docs/workflow-skill-landscape-20260516
architecture/workflow-microservice-boundaries-20260516
```
