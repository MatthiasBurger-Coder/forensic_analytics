---
name: spring-boot
description: Use only when a verified project module already uses Spring Boot, Spring wiring, Boot auto-configuration, or runtime bootstrap configuration.
---

# Spring Boot

## Purpose

Guide Spring Boot usage when a verified module already uses Spring Boot for application bootstrap, dependency wiring, runtime configuration, observability, service startup, or infrastructure integration.

This skill must not be used to introduce Spring Boot into a module unless the workflow explicitly requires a Spring Boot migration or a new Spring Boot service.

---

## Activation Rules

Use this skill only when at least one of the following is verified:

- The affected module already contains Spring Boot dependencies.
- The affected module contains a Spring Boot application class.
- The affected module uses Spring configuration, component scanning, auto-configuration, starters, actuator, or Boot-based runtime properties.
- The active workflow explicitly requires creating or migrating a module into a Spring Boot service.

Do not assume Spring Boot usage from project naming alone.

---

## Practices

- Inspect build files before adding or changing Spring Boot behavior.
- Keep domain and application packages free from Spring Boot annotations.
- Place Spring Boot configuration, bean wiring, adapters, controllers, clients, persistence configuration, health checks, and runtime bootstrap code in adapter, bootstrap, infrastructure, or delivery packages.
- Use constructor injection only.
- Avoid field injection.
- Avoid leaking Spring types into domain models, ports, use cases, or application services.
- Keep service boundaries explicit.
- Do not introduce shared common Spring Boot libraries between microservices unless architecture governance explicitly allows it.
- Prefer configuration properties for runtime settings instead of hard-coded values.
- Validate externalized configuration where possible.
- Use Spring Boot Actuator only in infrastructure/runtime layers.
- Apply `.agents/skills/resilience-engineering/SKILL.md` for service timeouts, health checks, readiness/liveness, retries, circuit breakers, graceful degradation, diagnostics, and failure-mode decisions.
- Keep tests independent from a full Spring Boot application context unless integration behavior requires it.
- Prefer slice tests, unit tests, contract tests, and adapter integration tests over broad `@SpringBootTest` usage.
- Use `@SpringBootTest` only when bootstrap, wiring, configuration, or runtime integration must be verified.
- Document every new Spring Boot runtime behavior in the affected architecture documentation and workflow artifacts.

---

## Hexagonal Architecture Rules

- Domain must not depend on Spring Boot.
- Application layer must not depend on Spring Boot.
- Ports must not depend on Spring Boot.
- Adapters may use Spring Boot.
- Infrastructure may use Spring Boot.
- Bootstrap modules may use Spring Boot.
- Controllers, schedulers, message consumers, repositories, REST clients, gRPC clients, configuration classes, and actuator integration belong outside the domain core.
- Spring Boot is a runtime and wiring mechanism, not the architecture itself.

### Allowed

```text
bootstrap
 └─ spring configuration

adapter-rest
 └─ controller

adapter-grpc
 └─ grpc endpoint

adapter-persistence
 └─ spring repository

application
 └─ use cases

domain
 └─ business model
```

### Forbidden

```text
domain
 └─ @Component

domain
 └─ @Service

application
 └─ @Autowired

application
 └─ Spring Boot configuration
```

---

## Microservice Rules

When the affected module is a microservice:

* The service must remain independently buildable.
* The service must remain independently runnable.
* The service must own its runtime configuration.
* The service must own its persistence boundary.
* The service must expose explicit contracts through REST, gRPC, messaging, or documented APIs.
* Do not create hidden coupling through shared Spring Boot configuration modules.
* Do not move domain logic into controllers, configuration classes, entities, repositories, or DTOs.

---

## Preferred Technology Stack

* Spring Boot 4.x
* Spring Framework 7.x
* Spring Actuator
* Spring Validation
* Spring Data (only in persistence adapters)
* Spring Security (only where required)
* Lombok (optional, project decision)
* Micrometer
* OpenTelemetry

---

## Verification

Before implementation:

* Verify Spring Boot dependencies exist.
* Verify module boundaries.
* Verify the target package belongs to bootstrap, infrastructure, or adapter layers.

After implementation:

* Run affected module tests.
* Run integration tests if runtime behavior changed.
* Verify architecture boundaries.
* Run the relevant quality gate from `QUALITY.md`.
* Verify no Spring Boot annotations leaked into domain or application packages.
* Verify service autonomy for microservice modules.

---

## Stop Rules

Stop and report if:

* Spring Boot cannot be verified in the target module.
* A change would introduce Spring Boot into the domain layer.
* A change would introduce Spring Boot into the application layer.
* A shared library would become a hidden microservice dependency.
* Service autonomy would be violated.
* The requested implementation conflicts with active workflow governance.
