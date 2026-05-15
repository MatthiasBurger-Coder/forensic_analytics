# Stop Conditions And Uncertainty

Stop instead of guessing when any condition below appears.

## Repository And Build

- WSL cannot access `/mnt/d/Projects/forensic_analytics`.
- Gradle cache locks prevent required verification and the slice needs command output.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` or `QUALITY.md` disagree about baseline or quality commands.
- `checkPackageCoverage` disappears or behaves differently from `QUALITY.md`.
- strict dependency verification cannot resolve newly added Spring Boot artifacts.
- dependency verification metadata includes unreviewed unrelated artifacts.

## Architecture

- A slice requires Spring in `forensic-analytics-domain` or `forensic-analytics-application`.
- Application use cases must depend on gRPC, REST, persistence implementation, Joern implementation or observability.
- Observability must depend on Spring, SLF4J, AspectJ, gRPC or REST without a new accepted ADR.
- A module rename is required to continue but callers, tests and docs cannot be verified.
- Current ArchUnit rules must be weakened instead of refined.

## Spring Boot

- Spring Boot plugin behavior changes tasks or classpaths outside the approved module.
- Boot startup requires a database, Joern container, graph DB, vector DB or live LLM provider.
- component scanning accidentally treats domain or application classes as Spring components.
- profile defaults can write outside an explicit workspace root.
- Actuator or web dependencies are needed but no dependency decision exists.

## Dependency Minimization

- a new annotation processor is needed only for boilerplate reduction.
- generated `equals`, `hashCode` or `toString` behavior is proposed for evidence models.
- an added dependency can be replaced by a small explicit constructor or JDK API.

## gRPC And Contracts

- integrating with Spring requires proto changes not covered by the slice.
- a request/session/build identifier is about to be repurposed as a generic operational correlation ID.
- streaming upload behavior or gRPC status mapping changes unexpectedly.
- a third-party gRPC starter is proposed without version, security, lifecycle and dependency verification.

## Evidence And Security

- missing runtime evidence would be filled in to make replay or reports complete.
- logging would include raw payloads, source content, method arguments, method return values, raw exception messages, stack frames, credentials or LLM prompts.
- Joern or parser failures would be converted into successful semantic evidence.
- Docker or workspace configuration can read uncontrolled host paths.
- runtime data sensitivity, redaction or retention ownership is unclear.

## Documentation

- README or arc42 would need to claim unimplemented behavior.
- ADRs conflict and the intended source of truth is unclear.
- a public command, task, property, module name, proto field or endpoint cannot be verified exactly.

## Escalation Format

When stopping, report:

- expected item
- what was found instead
- files or commands inspected
- why continuing would be unsafe
- smallest proposed next decision
