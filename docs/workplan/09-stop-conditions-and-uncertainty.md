# Stop Conditions And Uncertainty

## Source Authority

Stop if:

- `logging.zip` is required but cannot be found
- the unzipped logging source package is not accepted as the source material
- source files change before implementation and the plan is not revalidated

## Architecture

Stop if:

- a planned logging class would need to live in domain or application
- application or domain would need to depend on Spring, AspectJ, SLF4J MDC, gRPC, REST, or a concrete logging provider
- a new module name or package ownership cannot be verified
- architecture tests conflict with the selected approach and the conflict cannot be justified

## Security And Evidence

Stop if:

- implementation requires logging method parameters or return values
- runtime payloads, source content, stack traces, credentials, local paths, or LLM prompts would be logged
- logs are being treated as canonical evidence without a dedicated evidence model
- correlation IDs are inferred from unrelated fields
- gRPC request/session IDs are silently repurposed as generic correlation IDs

## Dependencies

Stop if:

- strict dependency verification fails for unclear reasons
- adding a dependency introduces a concrete logging binding
- dependency changes require a version upgrade unrelated to the logging task
- dependency metadata cannot be updated through a verified Gradle workflow

## Behavior

Stop if:

- REST response headers or error payloads change unexpectedly
- gRPC status mapping changes unexpectedly
- CLI stdout/stderr output changes unexpectedly
- server startup/shutdown behavior changes unexpectedly
- async correlation propagation becomes required but no ownership model exists

## Documentation

Stop if:

- documentation would describe behavior not implemented
- an ADR decision cannot be supported by verified code and build changes
- `QUALITY.md` and planned quality commands disagree

## Git Safety

Stop before staging or committing if:

- unrelated local changes overlap the planned write scope
- broad line-ending-only changes appear in `git status`
- generated build output appears as task changes
- any modified file cannot be tied to the requested logging integration
