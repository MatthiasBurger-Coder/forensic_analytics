# 1. Introduction and Goals

## 1.1 Requirements Overview

The Forensics Platform is an independent analysis platform for Java systems. It combines static analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported root-cause analysis.

The platform shall answer not only where an error happened, but how it emerged, which runtime values triggered it, which code path was actually executed and how the defect can be tested and fixed safely.

## 1.2 Business Goals

- Reconstruct runtime failures from exceptions or correlation IDs.
- Connect runtime events with static source-code context.
- Provide evidence-based LLM root-cause analysis.
- Prepare safe and reviewable fix plans.
- Support future controlled repair automation.

## 1.3 Key Capabilities

- Static Fact Ingestion
- Joern Semantic Ingestion
- Rule Planning and Byteman Generation
- Runtime Event Collection
- Exception Detection and Incident Creation
- Replay Engine
- Graph-Based UI Context
- LLM Incident Analysis
- Fix Planning
- Automated Repair Preparation

## 1.4 Stakeholders

| Stakeholder | Interest |
|---|---|
| Developer | Understand failures faster and reproduce them reliably |
| Lead Developer | Assess root cause, fix scope and regression risk |
| Platform Operator | Run and operate the Forensics Platform safely |
| Security Responsible | Ensure runtime data is protected and redacted |
| Reviewer | Review evidence-based fix proposals |
| Build/Tooling Engineer | Integrate Gradle, Maven, Byteman and Joern adapters |

## 1.5 Quality Goals

| Goal | Description |
|---|---|
| Traceability | Every runtime event must be traceable to source-code and rule context |
| Evidence-based diagnosis | LLM output must refer to available evidence |
| Security | Runtime values are sensitive by default |
| Scalability | Large multi-module Java systems must be analyzable incrementally |
| Extensibility | Storage engines, LLM providers and tool adapters must be replaceable |
