# Plugin Client Integration

The plugin is a producer and client. It must not become an analysis platform.

## Plugin Responsibilities

The plugin may:

- determine the repository remote URL from the build checkout
- determine branch and commit when available
- collect build context such as build tool, build ID, root project name and declared modules
- build `AnalyzeRepositoryRequest`
- send the request through gRPC
- receive `AnalyzeRepositoryResponse`
- display or persist the analysis session ID and checkout diagnostics as client-visible status
- report communication, validation and server-side checkout errors clearly

## Analytics Responsibilities

Analytics owns:

- workspace creation
- Git clone and checkout
- commit resolution
- source-root detection
- analysis session creation
- job registration
- later parser or analyzer execution

The plugin must not duplicate these responsibilities.

## Explicit Plugin Non-Scope

The plugin must not:

- perform AST analysis
- run JavaParser for platform evidence
- run Joern or create CPG artifacts
- generate BTM files
- run replay
- construct LLM prompts
- write server-side workspace files
- decide that analysis is complete

## Request Construction

The plugin should construct requests from verified build and Git metadata only. Missing branch or commit information must remain missing and be marked according to the request contract. The plugin must not guess the current branch from display text if Git metadata is unavailable.

## Response Handling

The plugin should handle:

- accepted session with ready checkout
- accepted session with checkout diagnostics
- rejected request
- gRPC unavailable
- timeout
- schema mismatch
- authentication or authorization failure if later introduced

The plugin should surface session ID, workspace ID and resolved commit when present. It should never claim that parser analysis completed from an `AnalyzeRepositoryResponse`.

## Tests

Required plugin-side tests:

- request builder maps repository, branch, commit and build context
- missing optional values remain explicit
- fake gRPC server receives the expected request
- checkout diagnostics are shown without converting them into analysis findings
- network and server errors are reported clearly
