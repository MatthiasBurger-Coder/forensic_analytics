# 10. Quality Requirements

## 10.1 Quality Tree

```text
Quality
├── Performance
│   ├── Incremental static imports
│   ├── Fast incident graph queries
│   └── Low runtime overhead
├── Scalability
│   ├── Multi-module support
│   ├── Large legacy codebase support
│   └── Runtime event partitioning
├── Security
│   ├── Runtime redaction
│   ├── Secret protection
│   ├── Auditability
│   └── Retention control
├── Observability
│   ├── Adapter correlation
│   ├── Sanitized operation logs
│   └── Logging framework isolation
├── Traceability
│   ├── Rule to source mapping
│   ├── Event to rule mapping
│   ├── Patch to incident mapping
│   └── Diagnosis to evidence mapping
└── Extensibility
    ├── Replaceable storage adapters
    ├── Replaceable LLM provider
    ├── Replaceable graph projection
    └── Replaceable vector projection
```

## 10.2 Quality Scenarios

| Scenario | Expected Result |
|---|---|
| Import changed Java files only | The platform detects changed files by hash and avoids full reprocessing |
| Load incident by CorrelationID | Replay timeline is generated in interactive time |
| Runtime event contains secret-like value | Value is redacted before persistence |
| Adapter operation fails | Logs contain operation category, correlation ID and exception category without raw payloads or secrets |
| Joern mapping is ambiguous | Mapping is marked as ambiguous and not silently linked |
| LLM lacks evidence | Diagnosis reports insufficient evidence |
| Tests fail after generated fix | No PR is created |

## 10.3 Governance Quality Scenarios

| Scenario | Expected Result |
|---|---|
| Requirement lacks scope, non-goals or testability | `workflow create` returns to the Three Amigos Requirement Gate |
| Requirement has blocking questions | `workflow create` asks focused clarification questions and does not create a final checked `docs/workflow/workflow.md` |
| Skill responsibilities conflict | Skill Registry / Conflict Auditor blocks workflow release until ownership is explicit |
| `workflow execute` starts without checked `docs/workflow/workflow.md` and checked arc42 documentation | Execution stops before implementation |
| Backend slice lacks JUnit 6 strategy or hexagonal review | Slice quality gate blocks continuation |
| Service-split slice lacks Microservice Senior Expert review | Slice quality gate blocks continuation |
| Frontend slice lacks React or UX review | Slice quality gate blocks continuation |
| `push auto` diff contains product implementation files | `push auto` stops and reports the blocked file |
| Documentation-only governance change is ready for release | `git diff --check`, registry, organigramm and process documentation checks are recorded |
| `workflow create` reaches Final Gate | No blocking questions remain, `workflow.md` is executable and testable, arc42 is checked or updated, and Documentation Governance passed |
