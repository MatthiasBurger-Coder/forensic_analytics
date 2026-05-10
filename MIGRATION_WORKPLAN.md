# Migration Workplan

## 1. Ziel

Das Ziel ist eine kontrollierte Extraktion der wiederverwendbaren Forensics-Analysefaehigkeiten aus `forensics_tracing` in die Engine `forensic_analytics`.

Langfristig soll `forensics_tracing` ein Build-Adapter bleiben, waehrend `forensic_analytics` Analyseorchestrierung, kanonische Modelle, Persistenz, gRPC-Ingestion, CLI, Server, Joern-Containerbetrieb, Graph, Replay, Reports und spaetere LLM-Kontexte besitzt.

## 2. Non-Goals

- Keine Big-Bang-Migration.
- Keine Entfernung funktionierender Plugin-Funktionalitaet ohne Ersatz.
- Keine Verschiebung von Gradle Tasks oder Maven Mojos in die Engine.
- Keine Einfuehrung leerer Vanity-Module.
- Keine Senkung von Coverage- oder Architekturregeln.
- Keine Aenderung der `forensics_tracing` Baseline ohne explizite Entscheidung.

## 3. Aktuelle Architektur

`forensics_tracing`:

- Single-Project Gradle/Maven Plugin.
- Java 17/JUnit 5 nach vorhandener Build- und AGENTS-Baseline.
- Enthaelt Domain-, Application-, Adapter-, Plugin- und Infrastructure-Pakete in einem Repository.
- Besitzt lokale BTM-Generierung, JavaParser-Scanning, Joern-CLI-Aufruf, H2-Analyseablage, Manifest-/Checksum-Artefakte und Runtime-Tracing-Helfer.

`forensic_analytics`:

- Multi-Module Projekt mit Java 25/JUnit 6.
- Bestehende Module:
  - `forensic-analytics-domain`
  - `forensic-analytics-application`
  - `forensic-analytics-persistence`
  - `forensic-analytics-ingestion-grpc`
  - `forensic-analytics-bootstrap`
- Aktueller Fokus: session-basierte gRPC-Ingestion fuer Plugin-Uploads.
- Noch fehlend: Engine-Orchestrierung, CLI, Repository-Adapter, JavaParser-Adapter, Joern-Docker-Adapter, Byteman-Adapter und kanonisches Analysemodell.

## 4. Zielarchitektur

Zielrichtung:

```text
forensics_tracing
  -> Gradle Adapter
  -> Maven Adapter
  -> Legacy BTM local mode
  -> spaeter Engine CLI oder gRPC Client

forensic_analytics
  -> Domain
  -> Application
  -> Engine Orchestration
  -> Ingestion gRPC
  -> Repository Source Adapter
  -> JavaParser Adapter
  -> Joern Docker Adapter
  -> Byteman Adapter
  -> Persistence
  -> CLI
  -> Server/Bootstrap
```

Abhaengigkeitsrichtung:

```text
cli -> engine -> application -> domain
server/bootstrap -> ingestion-grpc -> application -> domain
persistence -> application/domain
adapters -> application/domain
```

Verboten:

- `domain` darf nicht von Gradle, Maven, gRPC, Docker, Joern, H2, JavaParser, Spring, LLM-Providern oder UI abhaengen.
- `application` darf keine konkreten Adapter direkt instanziieren.
- Plugin-Code darf nicht in die Engine verschoben werden.

## 5. Repository Responsibility Split

`forensics_tracing` bleibt verantwortlich fuer:

- Gradle Plugin Registrierung.
- Gradle Task Inputs/Outputs und Extension.
- Maven Mojo Registrierung und Parameterbindung.
- Build-Tool-Lifecycle-Integration.
- Consumer-Projekt SourceSet-/Reactor-Erkennung.
- Legacy lokale BTM-Generierung, solange Kompatibilitaet gefordert ist.
- Runtime Helper Attachment fuer Byteman-Nutzung.

`forensic_analytics` wird verantwortlich fuer:

- Kanonische Analyse- und Evidenzmodelle.
- Repository-Analyseorchestrierung.
- Source-Fact-Import.
- Joern-Semantik ueber Docker-Adapter.
- Artefakt- und Finding-Modell.
- Persistenz und Projektionen.
- gRPC-Ingestion.
- CLI- und Server-Einstiegspunkte.
- Spaetere Graph-, Replay-, Report- und LLM-Kontextverarbeitung.

## 6. Proposed Module Structure

Bereits vorhanden und beibehalten:

- `forensic-analytics-domain`
  - Sofort noetig.
  - Enthaltene Domain soll schrittweise vom reinen Ingestion-Modell zum kanonischen Analysemodell erweitert werden.
- `forensic-analytics-application`
  - Sofort noetig.
  - Enthaltene Ingestion Use Cases bleiben; Repository-Analyse-Use-Cases kommen spaeter hinzu.
- `forensic-analytics-ingestion-grpc`
  - Bereits vorhanden.
  - Beibehalten als inbound Adapter fuer Plugin-Uploads.
- `forensic-analytics-persistence`
  - Bereits vorhanden.
  - In-memory Adapter ist sinnvoll fuer Bootstrap und Tests; echte Persistenz erst spaeter.
- `forensic-analytics-bootstrap`
  - Bereits vorhanden.
  - Server-Wiring fuer gRPC.

Kurzfristig zu evaluieren:

- `forensic-analytics-engine`
  - Sinnvoll fuer die erste lokale Analyseorchestrierung.
  - Erst anlegen, wenn ein getesteter minimaler Use Case entsteht.
- `forensic-analytics-cli`
  - Sinnvoll als erster benutzbarer lokaler Einstieg.
  - Erst anlegen, wenn ein getesteter `analyze`-Befehl entsteht.

Spaeter anlegen, nicht sofort:

- `forensic-analytics-adapter-git`
  - Erst wenn Repository Source Acquisition implementiert wird.
- `forensic-analytics-adapter-javaparser`
  - Erst wenn Source-Fact-Scanning in die Engine wandert.
- `forensic-analytics-adapter-joern-docker`
  - Erst wenn Docker Command Builder und Adaptertests eingefuehrt werden.
- `forensic-analytics-adapter-byteman`
  - Erst wenn Byteman Ownership entschieden ist.
- `forensic-analytics-adapter-build-gradle`
  - Nur falls Engine selbst Build-Adapter braucht; ansonsten im Plugin belassen.
- `forensic-analytics-adapter-build-maven`
  - Nur falls Engine selbst Build-Adapter braucht; ansonsten im Plugin belassen.
- `forensic-analytics-server`
  - Nur falls Server ueber Bootstrap hinaus fachliche Server-Komponenten braucht.
- `testbed`
  - Spaeter fuer externe Black-Box-Szenarien.

## 7. Slice Plan

### Slice 01: Engine Foundation auf bestehender Struktur

- Bestehende Module nicht neu erzeugen.
- `forensic-analytics-engine` nur mit minimalem getesteten Use Case anlegen, falls fuer lokale Analyse noetig.
- `forensic-analytics-cli` nur mit minimalem getesteten CLI-Pfad anlegen, falls direkt benutzbar.
- Dokumentieren, dass gRPC-Ingestion bereits existiert.

### Slice 02: Engine-neutrale Domain kopieren

- Kleine, frameworkfreie Value Objects aus `forensics_tracing` kopieren.
- Startkandidaten:
  - Analyse-ID
  - Repository-Metadaten
  - Artefaktreferenz
  - Source Location
  - Scan Fact
- Originale in `forensics_tracing` nicht entfernen.
- Tests in `forensic_analytics` hinzufuegen.

### Slice 03: Application Use Case fuer lokale Repository-Analyse

- `RunRepositoryAnalysisUseCase` definieren.
- Ports fuer Repository Source, Source Scanner, Semantic Analysis, Rule Generation und Result Store definieren.
- Fakes in Tests nutzen.
- Keine Joern-/Docker-/Git-Implementierung im Application Layer.

### Slice 04: Joern Docker Adapter

- `forensic-analytics-adapter-joern-docker` mit Command Builder, Runner und Adaptertests.
- Pinned Image benoetigt Entscheidung.
- Docker-Integration nur optional/tagged.

### Slice 05: Repository Source Adapter

- Initial lokale Repository-Pfade unterstuetzen.
- Remote Clone, Branch, Tag, Commit spaeter.

### Slice 06: CLI Entry Point

- `analyze --repo --profile --output --joern-mode`.
- CLI darf nur parsen, Use Case aufrufen und Ergebnis ausgeben.

### Slice 07: gRPC Ingestion erweitern

- Bestehende gRPC-Ingestion um stabile Analyse-Payloads erweitern.
- DTOs nicht in Domain leaken lassen.

### Slice 08: Plugin Adapter Boundary

- `forensics_tracing` baut Engine Request oder gRPC Upload.
- Legacy-Modus bleibt erhalten.
- Keine Build-Tool-Adapter in die Engine verschieben.

### Slice 09: E2E Testbed

- Erstes Szenario ohne Joern.
- Zweites Szenario mit Joern Container.
- WildFly nur als Smoke-/Ressourcen-Szenario, nicht normaler Unit-Gate.

## 8. Verification Strategy

Fuer `forensic_analytics`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Fuer `forensics_tracing`:

- Solange das Repository Java 17/JUnit 5 bleibt, muss dessen bestehender `QUALITY.md` Gate mit passender Toolchain verwendet werden.
- Wenn der Workflow Java 25/JUnit 6 auch fuer das Plugin erzwingen soll, ist zuerst ein eigener Baseline-Migrationsslice noetig.

Dokumentationsslices:

- Fuer reine Dokumentation reicht Git-Diff- und Link-/Pfadpruefung.
- Tests werden nicht als bestanden gemeldet, wenn sie nicht ausgefuehrt wurden.

## 9. Rollback Strategy

- Jeder Slice wird klein committed.
- `forensics_tracing` Legacy-Pfad bleibt bis Ersatz verifiziert ist.
- Kopierte Domainmodelle werden erst nach stabiler Engine-Grenze im Plugin ersetzt.
- gRPC-Vertraege werden versioniert und nicht still umbenannt.
- Joern-Docker-Adapter wird zuerst mit unit-getestetem Command Builder eingefuehrt.
- Bei Gate-Fehlern: Slice zurueckstellen, Ursache dokumentieren, keine Regeln abschwaechen.

## 10. Known Risks

- Baseline-Konflikt zwischen `forensics_tracing` Java 17/JUnit 5 und Workflow Java 25/JUnit 6.
- Bestehender `BtmGenerationRunner` hat mehrere Verantwortlichkeiten und muss vorsichtig geschnitten werden.
- H2-Analyseablage ist nicht automatisch die Engine-Persistenz.
- Byteman kann Domain-, Adapter- oder Legacy-Verantwortung sein; Entscheidung fehlt.
- Joern Docker Image und Ressourcenanforderungen sind noch nicht festgelegt.
- WildFly-Analyse kann Speicher- und Laufzeitgrenzen ueberschreiten.
- gRPC-Ingestion existiert bereits, aber das Payload-Schema ist noch nicht das vollstaendige kanonische Analysemodell.

## 11. Decisions Required From The User

1. Soll `forensics_tracing` kurzfristig auf Java 25 und JUnit 6 migriert werden?
2. Soll Slice 01 zuerst ein `forensic-analytics-engine` Modul oder zuerst ein `forensic-analytics-cli` Modul anlegen?
3. Soll Byteman-Regelgenerierung langfristig Engine-Adapter oder Plugin-Legacy-Funktion sein?
4. Welches Joern Docker Image mit welcher Version soll gepinnt werden?
5. Soll die bestehende gRPC-Ingestion vor der CLI erweitert werden?
6. Wie lange muss Legacy Local Mode im Plugin garantiert bleiben?

## 12. First Implementation Slice

Empfohlener naechster Slice:

```text
Slice 01: Engine Foundation auf bestehender Struktur
```

Konkretes Minimalziel:

- Kein erneutes Anlegen bereits vorhandener Module.
- `forensic-analytics-engine` nur dann erstellen, wenn ein minimaler `RunRepositoryAnalysisUseCase`-naher Koordinator mit Test implementiert wird.
- Alternativ zuerst `forensic-analytics-cli` erstellen, wenn der erste nutzbare Einstieg wichtiger ist.
- Bestehende gRPC-Ingestion unveraendert lassen, ausser Tests oder Dokumentation zeigen eine klare Luecke.

Definition of Done fuer Slice 01:

- Kleine, getestete Erweiterung.
- Keine Aenderung in `forensics_tracing`.
- `forensic_analytics` Full Gate ausgefuehrt.
- Ergebnisdokument `docs/migration/SLICE_01_ENGINE_FOUNDATION_RESULT.md`.
