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
| Joern mapping is ambiguous | Mapping is marked as ambiguous and not silently linked |
| LLM lacks evidence | Diagnosis reports insufficient evidence |
| Tests fail after generated fix | No PR is created |
