# Documentation Synchronization

## Required Documentation Review

After implementation, review and update only documentation that reflects actual behavior.

Likely documentation targets:

- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/10-quality-requirements.md`
- `docs/adr/README.md`
- optional new ADR for logging boundary and dependency choice
- `docs/README.md` if the integration changes public usage

## arc42 Updates

Update `docs/arc42/05-building-block-view.md` if a new observability module or infrastructure building block is created.

Update `docs/arc42/06-runtime-view.md` if request/command correlation lifecycle becomes part of runtime flow.

Update `docs/arc42/08-crosscutting-concepts.md` if a shared correlation/logging concept is implemented.

Update `docs/arc42/10-quality-requirements.md` if logging becomes part of security, traceability, or observability quality scenarios.

## ADR Need

Create a new ADR only if implementation makes an architecture decision that needs durable justification, such as:

- selecting SLF4J API as an infrastructure logging facade
- rejecting or accepting Spring AOP/AspectJ
- defining logs as operational diagnostics only
- introducing a new observability module

Do not create an ADR that describes planned behavior before implementation exists.

## README Need

Update README material only if the user-facing operation changes, such as:

- a new runtime configuration flag
- a new documented correlation header
- a new server startup option
- new required dependency setup

Do not document internal-only logging classes as public API.

## Documentation Stop Conditions

Stop documentation updates if:

- source and implementation disagree
- an ADR would need to justify a decision that was not actually made
- quality commands in docs would differ from `QUALITY.md`
- docs would imply logs are verified forensic evidence
