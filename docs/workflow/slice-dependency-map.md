# Slice Dependency Map

```text
S01 Metadata Contract And Owner Path Verification
├── S02 Gateway Forwarding And Public REST Serialization
│   └── S03 UI Metadata Data Path And Branch Listing
├── S04 Selected Branch Persistence Through Repository-Source Metadata
└── S05 Runtime Smoke Diagnostics And Documentation Closure depends on S02, S03 and S04
```

S02 and S04 may be reviewed independently after S01 because gateway forwarding and persistence have separate write scopes. S03 waits for S02. S05 closes documentation after implementation evidence exists.
