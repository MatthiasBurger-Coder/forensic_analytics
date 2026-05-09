# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse
- Version: 0.1
- Date: 2026-05-03

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```
