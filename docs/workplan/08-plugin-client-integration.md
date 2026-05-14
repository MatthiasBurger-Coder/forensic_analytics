# 08 - Plugin Client Integration

## Plugin Responsibilities

The plugin should only:

- create the request,
- determine repository URL,
- determine branch,
- determine commit,
- determine build context,
- send the gRPC request,
- evaluate the response,
- report errors clearly.

The plugin remains the producer and build adapter. It does not own analysis.

## Analytics Responsibilities

Analytics should:

- receive the request,
- create the analysis session,
- prepare the workspace,
- clone or checkout the repository,
- resolve the commit,
- detect source roots as metadata,
- register the first job state,
- return session and checkout result.

## Explicit Non-Responsibilities For The Plugin

The plugin must not:

- analyze AST,
- execute Joern,
- generate BTM rules,
- replace server-side workspace creation,
- become the analysis platform,
- persist canonical analysis data,
- build replay, graph or LLM evidence packages.

## Error Handling

Plugin errors should distinguish:

- unavailable Analytics endpoint,
- request validation failure,
- rejected repository reference,
- checkout failure,
- timeout,
- incompatible schema version,
- server-side internal failure.

Errors should include the Analytics message and correlation/request identifier where available, without leaking secrets.
