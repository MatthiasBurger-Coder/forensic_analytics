# Microservice Governance

## Status

Planned governance for future service-split work. This document does not claim
that the repository currently contains independently deployable microservices.

## Service Boundary Rule

Future services must be defined by business responsibility and bounded context,
not by technical package, Gradle module or layer names. A technical module is
not a microservice.

Each future microservice must own:

- internal domain model
- application behavior
- adapters
- tests
- configuration
- ports
- health checks
- observability expectations
- Dockerfile or container build evidence when containerization is in scope
- README or service-level operation notes

## Forbidden Coupling

Future microservices must not share Java implementation modules, domain modules,
DTO modules, service modules, repository modules, utility modules, internal
error-model modules or test-fixture modules.

Direct class dependencies between services are forbidden. Direct cross-service
database access is forbidden.

## Allowed Integration

Services may integrate only through explicit external contracts:

- REST/OpenAPI
- gRPC/protobuf
- approved message or event contracts

Contracts may be centrally documented, but they must not become shared Java
implementation code.

## Runtime Independence

A candidate service is not a microservice until runtime readiness is verified.
Required evidence includes build, start, test, configuration, observability,
healthcheck and container-readiness evidence.

Docker, Docker Swarm and Kubernetes readiness must be verified from repository
tooling before commands or manifests are documented. Missing Swarm or Kubernetes
tooling must be recorded as missing evidence, not treated as implemented
deployment support.

## Migration Slice Requirements

Every future production migration slice must document:

- scope
- non-scope
- service boundary
- owned data
- contract impact
- test impact
- risk level
- rollback or strangler strategy
- quality-gate commands
- forbidden changes

## Current State

The current repository is governed as a modular monolith. Future target service
names in workflow documents are planning candidates only until a later workflow
creates verified service boundaries, contracts and runtime-readiness evidence.
