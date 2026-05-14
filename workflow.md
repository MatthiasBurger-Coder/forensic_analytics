# workflow.md

# EPIC: Dev-Team als Subagent-System für forensic_analytics

**Projekt:** forensic_analytics
**Architektur:** Hexagonale Architektur
**Zielplattform:** Java 25, Gradle 9.4, JUnit 6, gRPC, Protobuf
**Betriebsmodell:** Multi-Agent / Skill-basiertes Subagent-System
**Ablage:** `.agents/skills/*/SKILL.md` und `.codex/agents/*.toml`
**Wichtige Regel:** Vorhandene Agents nur gezielt und minimal erweitern. Keine destruktiven Änderungen.

---

# 1. Zielsetzung

Dieses Workflow-Dokument beschreibt den vollständigen Aufbau eines professionellen Development-Teams als Subagent-System.

Die Agents arbeiten rollenbasiert und greifen auf gemeinsame Experten-Skills zurück.

Das Ziel ist:

* parallele Entwicklung
* saubere Verantwortlichkeiten
* kontrollierte Architekturführung
* automatische Qualitätsprüfung
* koordinierte Slice-basierte Umsetzung
* kontrollierte Agent-Orchestrierung
* reproduzierbare Entwicklungsprozesse

---

# 2. Zielstruktur

```text
.agents/
├── AGENTS.md
├── orchestrator/
│   ├── swarm-orchestrator.md
│   └── routing-rules.md
│
├── roles/
│   ├── senior-java-backend.md
│   ├── senior-react-frontend.md
│   ├── senior-ux-designer.md
│   ├── senior-system-architect.md
│   ├── senior-tester.md
│   ├── senior-devops.md
│   └── senior-swarm-orchestrator.md
│
└── skills/
    ├── forensic-architecture-hexagonal/SKILL.md
    ├── forensic-architecture-archunit-hexagonal/SKILL.md
    ├── forensic-architecture-modular-monorepo/SKILL.md
    ├── forensic-backend-java-25/SKILL.md
    ├── forensic-backend-junit6/SKILL.md
    ├── forensic-backend-grpc/SKILL.md
    ├── forensic-backend-protobuf/SKILL.md
    ├── forensic-backend-spring-core/SKILL.md
    ├── forensic-frontend-react/SKILL.md
    ├── forensic-frontend-hexagonal/SKILL.md
    ├── forensic-frontend-ux-guidelines/SKILL.md
    ├── forensic-devops-docker/SKILL.md
    ├── forensic-devops-kubernetes/SKILL.md
    ├── forensic-devops-gradle/SKILL.md
    ├── forensic-devops-ci-cd/SKILL.md
    ├── forensic-devops-observability/SKILL.md
    ├── forensic-quality-testing-strategy/SKILL.md
    ├── forensic-quality-mutation-testing/SKILL.md
    ├── forensic-quality-architecture-validation/SKILL.md
    ├── forensic-quality-gates/SKILL.md
    ├── forensic-orchestration-swarm-coordination/SKILL.md
    ├── forensic-orchestration-slice-execution/SKILL.md
    ├── forensic-orchestration-conflict-resolution/SKILL.md
    └── forensic-orchestration-branch-strategy/SKILL.md

.codex/
└── agents/
    ├── senior_java_backend.toml
    ├── senior_react_frontend.toml
    ├── senior_ux_designer.toml
    ├── senior_system_architect.toml
    ├── senior_tester.toml
    ├── senior_devops.toml
    └── senior_swarm_orchestrator.toml
```

---

# 3. Grundprinzipien

## 3.1 Rollenbasierte Verantwortung

Jeder Agent besitzt:

* eine klar definierte Verantwortung
* definierte Grenzen
* definierte Inputs/Outputs
* festgelegte Qualitätsregeln
* festgelegte Architekturregeln

---

## 3.2 Skill-Sharing

Skills sind:

* wiederverwendbar
* unabhängig von Rollen
* zentral versionierbar
* gemeinsam nutzbar

Agents referenzieren Skills.

Keine Duplikate von Wissen.

---

## 3.3 Slice-basierte Entwicklung

Alle Arbeiten erfolgen in:

* kleinen Slices
* klaren Verantwortungsbereichen
* validierbaren Schritten
* commitbaren Zwischenständen

Jeder Slice:

1. Analyse
2. Planung
3. Umsetzung
4. Tests
5. QUALITY.md
6. Architekturprüfung
7. Commit
8. Dokumentation

---

# 4. Rollenbeschreibung

---

# 4.1 Senior Java Backend Developer

## Datei

```text
.agents/roles/senior-java-backend.md
```

## Verantwortung

* Backend-Implementierung
* Domain-Modellierung
* Ports & Adapters
* gRPC-Integration
* Protobuf-Definitionen
* Persistence
* Performance
* Nebenläufigkeit
* Runtime-Analyse
* Scanner-Integration

## Pflicht-Skills

```text
skills:
- skills/forensic-backend-java-25/SKILL.md
- skills/forensic-backend-junit6/SKILL.md
- skills/forensic-backend-grpc/SKILL.md
- skills/forensic-backend-protobuf/SKILL.md
- skills/forensic-architecture-hexagonal/SKILL.md
- skills/forensic-architecture-archunit-hexagonal/SKILL.md
- skills/forensic-quality-testing-strategy/SKILL.md
```

## Regeln

* Keine Framework-Abhängigkeiten im Domain-Core
* Ports strikt trennen
* Keine Business-Logik in Adaptern
* Tests verpflichtend
* ArchUnit verpflichtend

---

# 4.2 Senior React Frontend Developer

## Datei

```text
.agents/roles/senior-react-frontend.md
```

## Verantwortung

* React Frontend
* UI-Komponenten
* State-Management
* Frontend-Hexagon
* API-Integration
* Accessibility
* Frontend-Performance
* Monitoring

## Pflicht-Skills

```text
skills:
- skills/forensic-frontend-react/SKILL.md
- skills/forensic-frontend-hexagonal/SKILL.md
- skills/forensic-frontend-ux-guidelines/SKILL.md
- skills/forensic-architecture-hexagonal/SKILL.md
- skills/forensic-quality-testing-strategy/SKILL.md
```

## Regeln

* UI strikt von Businesslogik trennen
* Keine API-Calls direkt in Komponenten
* State zentralisieren
* Komponenten testbar halten

---

# 4.3 Senior UX Designer

## Datei

```text
.agents/roles/senior-ux-designer.md
```

## Verantwortung

* UX-Strategie
* Benutzerführung
* Informationsarchitektur
* Design-System
* Accessibility
* User-Flows
* Konsistenz
* Analyse-Visualisierung

## Pflicht-Skills

```text
skills:
- skills/forensic-frontend-ux-guidelines/SKILL.md
- skills/forensic-frontend-react/SKILL.md
```

## Regeln

* Keine UI ohne UX-Konzept
* Accessibility priorisieren
* Analyseoberflächen verständlich gestalten
* Entwicklerfreundliche UI-Strukturen

---

# 4.4 Senior System Architect

## Datei

```text
.agents/roles/senior-system-architect.md
```

## Verantwortung

* Gesamtarchitektur
* Hexagonale Architektur
* Modulgrenzen
* Monorepo-Strategie
* Event-Flows
* Kommunikationsdesign
* Skalierbarkeit
* Sicherheitsarchitektur
* Architektur-Reviews

## Pflicht-Skills

```text
skills:
- skills/forensic-architecture-hexagonal/SKILL.md
- skills/forensic-architecture-archunit-hexagonal/SKILL.md
- skills/forensic-architecture-modular-monorepo/SKILL.md
- skills/forensic-backend-grpc/SKILL.md
- skills/forensic-backend-protobuf/SKILL.md
```

## Regeln

* Architektur vor Implementierung
* Keine zyklischen Abhängigkeiten
* Strikte Modulgrenzen
* Domain-zentrierte Struktur

---

# 4.5 Senior Tester

## Datei

```text
.agents/roles/senior-tester.md
```

## Verantwortung

* Teststrategie
* Unit-Tests
* Integrationstests
* Architekturtests
* Mutation-Testing
* Regressionstests
* Performance-Tests
* CI-Validierung

## Pflicht-Skills

```text
skills:
- skills/forensic-backend-junit6/SKILL.md
- skills/forensic-quality-testing-strategy/SKILL.md
- skills/forensic-quality-mutation-testing/SKILL.md
- skills/forensic-quality-architecture-validation/SKILL.md
- skills/forensic-quality-gates/SKILL.md
```

## Regeln

* Kein Merge ohne Tests
* ArchUnit verpflichtend
* Regressionen blockieren
* Quality-Gates erzwingen

---

# 4.6 Senior DevOps

## Datei

```text
.agents/roles/senior-devops.md
```

## Verantwortung

* Docker
* Kubernetes
* CI/CD
* Build-Systeme
* Gradle
* Deployment
* Monitoring
* Logging
* Skalierung
* Infrastruktur-Automatisierung

## Pflicht-Skills

```text
skills:
- skills/forensic-devops-docker/SKILL.md
- skills/forensic-devops-kubernetes/SKILL.md
- skills/forensic-devops-gradle/SKILL.md
- skills/forensic-devops-ci-cd/SKILL.md
- skills/forensic-devops-observability/SKILL.md
```

## Regeln

* Reproduzierbare Builds
* Immutable Infrastructure
* Build-Caching aktivieren
* Security-Scanning verpflichtend

---

# 4.7 Senior Agent Swarm Orchestrator

## Datei

```text
.agents/roles/senior-swarm-orchestrator.md
```

## Verantwortung

* Agent-Koordination
* Slice-Steuerung
* Konfliktmanagement
* Routing
* Branch-Koordination
* Task-Verteilung
* Agent-Synchronisation
* Qualitätskontrolle

## Pflicht-Skills

```text
skills:
- skills/forensic-orchestration-swarm-coordination/SKILL.md
- skills/forensic-orchestration-slice-execution/SKILL.md
- skills/forensic-orchestration-conflict-resolution/SKILL.md
- skills/forensic-orchestration-branch-strategy/SKILL.md
```

## Regeln

* Keine konkurrierenden Änderungen ohne Synchronisation
* Branch-Isolation erzwingen
* Merge-Konflikte früh erkennen
* Agent-Abhängigkeiten koordinieren

---

# 5. Experten-Skills

---

# 5.1 Hexagonale Architektur

## Datei

```text
.agents/skills/forensic-architecture-hexagonal/SKILL.md
```

## Inhalte

* Ports & Adapters
* Domain Isolation
* Dependency Rule
* Application Layer
* UseCases
* Adapter Separation
* Event Boundaries
* Testing Isolation
* Framework Independence

## Pflichtregeln

* Domain kennt keine Frameworks
* Adapter nur über Ports
* Infrastruktur außerhalb des Cores

---

# 5.2 ArchUnit für Hexagonal

## Datei

```text
.agents/skills/forensic-architecture-archunit-hexagonal/SKILL.md
```

## Inhalte

* Architekturtests
* Modulregeln
* Layer-Validierung
* Package-Regeln
* Dependency-Prüfung
* Adapter-Validierung
* Boundary-Tests

## Pflichtregeln

* Jede Architekturregel testbar
* Verstöße blockieren Build
* Architekturdrift verhindern

---

# 5.3 JUnit 6 Expertenwissen

## Datei

```text
.agents/skills/forensic-backend-junit6/SKILL.md
```

## Inhalte

* JUnit 6 Migration
* Dynamic Tests
* Parallel Testing
* Extensions
* Integration Testing
* Test Isolation
* Mocking
* Testcontainers
* Architekturtests

## Pflichtregeln

* Tests deterministisch
* Keine Shared States
* Parallelisierung berücksichtigen

---

# 5.4 Protobuf Expertenwissen

## Datei

```text
.agents/skills/forensic-backend-protobuf/SKILL.md
```

## Inhalte

* Proto3
* Message-Versionierung
* Backward Compatibility
* Streaming
* Large Payload Handling
* Event-Modellierung
* Performance
* gRPC Contracts

## Pflichtregeln

* Niemals Breaking Changes
* Messages versionieren
* Contracts stabil halten

---

# 6. Slice-Workflow

---

# 6.1 Slice starten

## Verantwortlich

Senior Swarm Orchestrator

## Schritte

1. Aufgabe analysieren
2. Betroffene Module identifizieren
3. Verantwortliche Agents bestimmen
4. Branch erzeugen
5. Slice dokumentieren
6. Agenten starten

---

# 6.2 Umsetzung

## Regeln

* Jeder Agent arbeitet isoliert
* Keine direkten Überschneidungen
* Änderungen klein halten
* Häufig validieren

---

# 6.3 Quality Assurance

## Required

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

`QUALITY.md` is the authoritative quality contract for this repository. Optional external checks such as SonarCloud must follow the credentials and skip/report rules documented there.

---

# 6.4 Architekturvalidierung

## Verpflichtend

* ArchUnit
* Modulprüfung
* Dependency-Prüfung
* Hexagonale Regeln

---

# 6.5 Abschluss

## Schritte

1. QUALITY.md ausführen
2. Architektur validieren
3. Tester-Agent Review
4. System-Architect Review
5. Commit erstellen
6. Dokumentation aktualisieren
7. Push vorbereiten

---

# 7. Sicherheitsregeln

## Agents dürfen niemals

* Architekturgrenzen verletzen
* Domain mit Framework koppeln
* Tests überspringen
* Quality Gates ignorieren
* Ungeprüfte Commits erstellen
* Fremde Agent-Dateien überschreiben

---

# 8. Zielbild

Das Ergebnis ist ein professionelles Multi-Agent-Entwicklungssystem mit:

* klaren Rollen
* gemeinsamen Experten-Skills
* kontrollierter Architektur
* reproduzierbaren Prozessen
* paralleler Entwicklung
* automatischer Qualitätssicherung
* orchestrierter Slice-Ausführung
* skalierbarer Teamstruktur

Dieses System bildet die Grundlage für eine langfristig wartbare und skalierbare Entwicklungsplattform innerhalb von forensic_analytics.
