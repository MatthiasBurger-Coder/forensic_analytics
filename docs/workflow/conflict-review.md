# Conflict Review

## Blocking For Workflow Execution

- The previous active workflow under `docs/workflow/**` described Git Branch
  Strategy work on `feature/workflow-git-branch-strategy-20260516`. It has been
  regenerated for this workflow creation branch.
- ADR-0011 requires Three Amigos before workflow authoring and execution. This
  workflow records a readiness decision and execution must refresh it before
  mutating governance files.
- ADR-0015 requires Skill Registry and Conflict Auditor review for new skills
  and governance changes. Slice 01 must run this before skill creation.
- The repository uses `.agents/skills/<name>/SKILL.md`, not flat skill Markdown
  files. Execution must follow the verified convention or stop.
- `.codex/prompts/**` is not present. Prompt updates must target verified
  `.agents/prompts/**` files unless a slice explicitly creates a new prompt
  location.

## Blocking For Later Service Extraction

- ADR-0006 currently allows Spring Boot only in `forensic-analytics-boot-app`.
  Independent Spring Boot services require a later ADR and architecture-test
  slice.
- Kubernetes and Docker Swarm tooling is not verified. Later runtime-readiness
  slices must not invent commands or manifests.
- Trace and correlation fields must be contract-defined. Do not infer
  `correlationId`, `traceId`, `spanId` or runtime session semantics from
  similarly named fields.
- Service ownership and data ownership must be verified before creating service
  roots, contracts, deployment descriptors or migrations.

## Non-Blocking Risks

- The current Gradle layout is a modular monolith with shared domain,
  application, logging and observability modules. That is valid today but cannot
  be presented as microservice autonomy.
- `docs/arc42/09-architecture-decisions.md` omits accepted ADR-0009, ADR-0010
  and ADR-0013 while `docs/adr/README.md` lists them. Slice 10 must reconcile
  or document the reason.
- Root `README.md` is absent. Any README update must target an existing README
  such as `docs/README.md` or explicitly create a root README.
- Some EPIC metadata still uses German labels despite the English-only
  documentation rule. This is outside the immediate workflow scope unless a
  later slice explicitly includes EPIC cleanup.
