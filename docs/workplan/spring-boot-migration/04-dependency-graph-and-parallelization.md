# Dependency Graph And Parallelization

## Slice Dependency Graph

```text
00 Inventory And Workplan
  -> 01 Spring Boot Architecture Decision
      -> 02 Version Catalog And Dependency Verification
          -> 03 Spring Boot App Module
              -> 04 Typed Spring Configuration
                  -> 05 Spring Bean Wiring For Existing Use Cases
                      -> 06 gRPC Server Lifecycle In Spring Boot
                      -> 07 REST/API Spring Strategy
                      -> 10 Persistence And Workspace Configuration
                  -> 11 Joern And External Tool Configuration
              -> 08 Observability Bridge Review
              -> 09 Constructor Explicitness And Dependency Minimization

03 Spring Boot App Module
  + 06 gRPC Server Lifecycle In Spring Boot
  + 10 Persistence And Workspace Configuration
      -> 12 Docker And Deployment Baseline

Implemented selected slices
  -> 13 Documentation And Architecture Test Synchronization
      -> 14 Final Quality Gate
```

Slice 12 depends on the implemented parts of Slices 03, 06 and 10.

Slice 13 depends on the behavior that was actually implemented. It must not document planned behavior as completed behavior.

Slice 14 is the final blocking verification slice.

This Spring Boot migration is a prerequisite infrastructure workplan. It does not replace the EPIC delivery plan for JSONL runtime import, incident replay, graph projection or LLM diagnosis.

## Blocking Slices

| Slice | Why it blocks |
|---|---|
| 00 | Establishes verified baseline and replaces active workplan. |
| 01 | Accepts Spring Boot as an outer boundary before dependency changes. |
| 02 | Adds version catalog and strict dependency verification metadata. |
| 03 | Creates the module required by all Spring-specific slices. |
| 05 | Provides Spring bean wiring needed by gRPC, REST and storage integration. |
| 14 | Final quality gate before commit or push. |

## Parallelization Opportunities

These slices may run in parallel after their prerequisites if write scopes remain disjoint:

| Parallel group | Slices | Guardrail |
|---|---|---|
| Boot configuration and constructor cleanup | 04 and 09 | Constructor cleanup must not touch the same config classes being actively changed by Slice 04 unless ownership is coordinated. |
| gRPC and REST integration | 06 and 07 | Shared boot configuration must be stable before parallel work starts. |
| Observability and dependency minimization | 08 and 09 | Do not add annotation processors or generated methods to observability event models if it would change equality, sanitization or log message behavior. |
| Joern and persistence configuration | 10 and 11 | Avoid concurrent edits to the same boot configuration package. |
| Documentation drafting | 13 alongside later implementation | Final docs must be updated from actual diffs, not assumptions. |

## Role Ownership Map

| Slice | Primary owner | Review partners |
|---|---|---|
| 00 | Senior Workflow Architect | Senior Documentation Engineer |
| 01 | Senior System Architect | Senior Documentation Engineer, Senior Requirement Engineer |
| 02 | Senior DevOps | Senior Java Backend, Senior Tester |
| 03 | Senior Java Backend | Senior DevOps, Senior Tester |
| 04 | Senior Java Backend | Senior DevOps |
| 05 | Senior Java Backend | Senior System Architect, Senior Tester |
| 06 | Senior gRPC/Protobuf Specialist | Senior Java Backend, Senior Tester |
| 07 | Senior System Architect | Senior Java Backend, Senior Tester |
| 08 | Senior System Architect | Senior Security Sandbox Engineer, Senior Tester |
| 09 | Senior Java Backend | Senior Tester |
| 10 | Senior Analysis Storage Architect | Senior Security Sandbox Engineer, Senior Java Backend |
| 11 | Senior Joern CPG Specialist | Senior DevOps, Senior Security Sandbox Engineer |
| 12 | Senior DevOps | Senior Security Sandbox Engineer |
| 13 | Senior Documentation Engineer | Senior System Architect, Senior Tester |
| 14 | Senior Tester | Senior DevOps |

## Conflict Rules

- Do not let two slices edit `settings.gradle.kts` concurrently.
- Do not let two slices edit `gradle/libs.versions.toml` concurrently.
- Do not let two slices update `gradle/verification-metadata.xml` concurrently.
- Do not split or rename modules while another slice is changing dependencies.
- Do not edit proto contracts while gRPC lifecycle tests are being migrated unless the proto slice owns both changes.
- Do not update architecture tests after implementation without checking the actual dependency graph.
