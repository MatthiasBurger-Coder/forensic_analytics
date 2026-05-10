# Inspection Report

## 1. Ziel und Umfang

Dieser Bericht dokumentiert die read-only Inspektion fuer die erste Ausfuehrung von `workflow.md`.
Es wurde keine Code-Migration durchgefuehrt.

Gepruefte Repositories:

- `D:\Projects\forensics_tracing`
- `D:\Projects\forensic_analytics`

Gepruefte Baseline:

- Java 25 wurde fuer die Workflow-Kommandos explizit ueber `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot` gesetzt.
- Beide Gradle Wrapper melden Gradle 9.4.0.
- `forensic_analytics` verwendet Java 25 und JUnit 6.
- `forensics_tracing` ist nach aktueller Build-Konfiguration weiterhin Java 17 und JUnit 5. Das ist ein bekannter Migrationskonflikt zwischen bestehendem Plugin-Repository und Ziel-Workflow.

## 2. Aktuelle Struktur von forensics_tracing

`forensics_tracing` ist ein einzelnes Gradle-Projekt mit Gradle- und Maven-Plugin-Funktionalitaet, Byteman-Regelgenerierung, JavaParser-Scanning, Joern-CLI-Integration, H2-basierter Analyseablage und Runtime-Tracing-Helfern.

Wichtige Build- und Qualitaetsdateien:

- `settings.gradle.kts`: Single-Project Build `forensics-tracing`.
- `build.gradle.kts`: Java Library, Gradle Plugin, Maven Plugin Development, JaCoCo, SonarCloud, Lombok, Java-17-Toolchain.
- `gradle/libs.versions.toml`: JUnit 5.13.4, Gradle 9.4.0 Wrapper, JaCoCo 0.8.13, JavaParser, H2, Maven Plugin Tools.
- `QUALITY.md`: lokaler Gate mit `clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage`.
- `AGENTS.md`: Java 17, Gradle 9.4.0, JUnit 5, Hexagonal Architecture, Regression-first Workflow.
- `README.md`: dokumentiert Gradle- und Maven-Nutzung, BTM-Generierung, Joern-Analyse und Runtime-Tracing.

Hauptpakete unter `src/main/java/de/burger/forensics`:

- `domain`: Domain-Modelle fuer Regeln, Scan-Events, Analyseidentitaet, Cache-Profile, semantische Joern-Beziehungen, Ports und Condition-Strategien.
- `application`: Use Cases fuer Regelgenerierung, semantische Analyse, Source-Fingerprints und Tracing-Fassade.
- `adapters`: JavaParser-Scanner, Joern-CLI-Adapter, Dateisystem-Writer und H2-Persistenzadapter.
- `adaptersupport`: Parser-, Joern- und H2-Unterstuetzungsklassen, darunter `ProcessJoernCommandExecutor`.
- `plugin`: Gradle-Plugin, Maven-Mojos, build-tool-neutrale Runner, Byteman-Renderer und Datei-Writer.
- `infrastructure`: Runtime-Tracing-Helfer und AspectJ-basierte Logging-Infrastruktur.

Wichtige Orchestrierung:

- `BtmGenerationRunner` koordiniert Source-Scanning, Regelgenerierung, Rendering, H2-Persistenz, Manifest- und Checksum-Artefakte.
- `ForensicsSemanticAnalysisRunner` koordiniert Joern-CLI-Semantik, Analyse-Store und Manifest-Aktualisierung.
- `GenerateRulesUseCase` ist ein application-level Use Case fuer Scanner- und Renderer-Koordination.
- `AnalyzeSemanticsUseCase` koordiniert semantische Analyse und Persistenzimport.

Build-Tool-Adapter:

- Gradle: `BtmGenPlugin`, `GenerateBtmTask`, `AnalyzeForensicsSemanticsTask`, `ImportForensicsSemanticsTask`.
- Maven: `BtmGenMojo`, `BtmGenAggregateMojo`, `AnalyzeMojo`, `AnalyzeAggregateMojo`, `AnalyzeSemanticsMojo`, `ImportSemanticsMojo`, `CleanForensicsAnalysisMojo`.

Tests:

- Breite Unit- und Adaptertests in den jeweiligen Paketstrukturen.
- Gradle TestKit Tests fuer Plugin-Verhalten.
- Maven-Paritaetstests fuer Mojo- und Gradle-Verhalten.
- ArchUnit- und SOLID-Tests unter `src/test/java/de/burger/forensics/quality`.

## 3. Aktuelle Struktur von forensic_analytics

`forensic_analytics` ist bereits ein Multi-Module Gradle-Projekt.

Build-Baseline:

- `settings.gradle.kts` inkludiert:
  - `forensic-analytics-domain`
  - `forensic-analytics-application`
  - `forensic-analytics-persistence`
  - `forensic-analytics-ingestion-grpc`
  - `forensic-analytics-bootstrap`
- `build.gradle.kts` setzt Java 25, JUnit 6, JaCoCo, ArchUnit und Package-Coverage.
- `gradle/libs.versions.toml` enthaelt JUnit 6.0.3, gRPC 1.80.0, Protobuf 4.34.1, JaCoCo 0.8.14.
- `QUALITY.md` definiert den Java-25/JUnit-6-Gate mit Dependency Verification.
- `AGENTS.md` beschreibt die Forensic-Analytics-Regeln fuer Evidenz, Graph, Replay, LLM, Reporting und Hexagonal Architecture.

Bestehende Module:

- `forensic-analytics-domain`
  - Aktuell: `IngestionSession`, `IngestionSessionState`, `IngestionPayload`.
  - Verantwortung: minimale Domain fuer Ingestion-Sitzungen und Payload-Metadaten.
- `forensic-analytics-application`
  - Aktuell: `ForensicIngestionUseCase`, `DefaultForensicIngestionUseCase`, Command- und Result-Records, `IngestionSessionRepository`.
  - Verantwortung: Session-orientierte Ingestion-Orchestrierung ohne konkrete Persistenz oder gRPC-Abhaengigkeit.
- `forensic-analytics-persistence`
  - Aktuell: `InMemoryIngestionSessionRepository`.
  - Verantwortung: lokaler Test-/Bootstrap-Persistenzadapter.
- `forensic-analytics-ingestion-grpc`
  - Aktuell: `forensic_ingestion.proto`, `ForensicIngestionGrpcService`, Mapper und Validatoren.
  - Verantwortung: inbound gRPC Adapter fuer Plugin-Uploads.
- `forensic-analytics-bootstrap`
  - Aktuell: `ForensicAnalyticsServerApplication`, `GrpcIngestionServerFactory`, `GrpcIngestionServerSettings`.
  - Verantwortung: Start und Wiring des gRPC-Ingestion-Servers.

Bestehende Dokumentation:

- `docs/README.md`: Moduluebersicht und gRPC-Ingestion-Baseline.
- `docs/adr/ADR-0001-plugins-are-producers.md`: Plugins sind Produzenten, nicht die Plattform.
- `docs/adr/ADR-0002-canonical-analysis-model.md`: kanonisches Analysemodell als Ziel.
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`: Runtime Events sind sensibel.
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`: Graph und Vector DB sind Projektionen.
- `docs/arc42`: Architekturuebersicht mit Zielbausteinen fuer Import, Rule Planning, Replay, Graph, Vector Context, LLM und Adapter.

## 4. Bestehende wiederverwendbare Core-Logik

In `forensics_tracing` gibt es wiederverwendbare Kernlogik, die fachlich eher zur Engine gehoert:

- Analyseidentitaet und Artefaktmetadaten:
  - `AnalysisRunId`, `AnalysisRunStatus`, `AnalysisSchemaVersion`, `BuildId`, `BuildIdentity`, `SourceFingerprint`, `ArtifactChecksum`.
- Source- und Scan-Modell:
  - `ScanEvent`, `SourceLocation`, `SourceContext`, `MethodScanContext`, `Rule`, `RuleId`, `RuleTemplate`.
- Cache- und Profilmodelle:
  - `CachedScanResult`, `ScanDependency`, `ScanPhase`, `ScanProfile`, `SourceFileFingerprint`.
- Semantikmodell:
  - `SemanticAnalysisRequest`, `SemanticAnalysisResult`, `SemanticNode`, `SemanticEdge`, `SemanticMethod`, `CallRelation`, `ControlFlowRelation`, `DataFlowPath`, `SemanticAnchor`.
- Application-Orchestrierung:
  - `GenerateRulesUseCase`, `AnalyzeSemanticsUseCase`, `SourceFingerprintService`, `SemanticAnchorMatcher`.
- Ports:
  - `CodeScanPort`, `RuleRenderPort`, `SemanticAnalysisPort`, `SemanticAnalysisStorePort`, `AnalysisStorePort`, `ArtifactChecksumPort`, `SourceFingerprintPort`.

Diese Logik ist nicht vollstaendig engine-neutral, weil einige Modelle noch stark auf Byteman-Regeln, lokale Artefaktdateien, H2-Store und Plugin-Ausgabeformen ausgerichtet sind. Sie ist dennoch die wichtigste Quelle fuer spaetere Migrationsslices.

## 5. Bestehende Plugin-only-Logik

Folgende Verantwortung sollte im Plugin-Repository bleiben:

- Gradle Plugin ID und Plugin-Metadaten.
- Gradle Extension `BtmGenExtension`.
- Gradle Tasks und Task-Inputs/Outputs.
- Maven Mojo-Klassen und Maven-Parameter-Mapping.
- Maven Plugin Descriptor Build-Logik.
- Gradle/Maven Logging Adapter.
- Consumer-Build-Integration, SourceSet-Erkennung und Reactor-Source-Root-Sammlung.
- Plugin Runtime Helper Attachment.
- Plugin Publishing Konfiguration.
- Tests, die Gradle TestKit, Maven Mojo-Harness oder Build-Tool-Lifecycle pruefen.

## 6. Bestehende Engine-/Server-Logik

`forensic_analytics` enthaelt bereits Engine-nahe Grundlagen:

- Session-basierte Ingestion-Domain.
- Application Use Case fuer Start, Upload, Complete und Abort von Analyse-Sitzungen.
- gRPC-Transportvertrag fuer Plugin-Uploads.
- Transportvalidierung und Mapping von Protobuf DTOs zu Application Commands.
- In-memory Repository als austauschbarer Persistenzadapter.
- Bootstrap fuer gRPC Server.

Noch nicht vorhanden:

- Engine-Modul fuer lokale Analyseorchestrierung.
- CLI-Modul.
- Repository-Checkout-/Source-Acquisition-Adapter.
- JavaParser-Adapter.
- Joern-Docker-Adapter.
- Byteman-Adapter.
- Kanonisches Analysemodell fuer Scan-Facts, Rules, Runtime Events, Graph, Replay und Findings.

## 7. Offene Architekturfragen

1. Soll `forensics_tracing` kurzfristig auf Java 25 und JUnit 6 migriert werden, oder bleibt es als Adapter zunaechst Java 17/JUnit 5 kompatibel?
2. Soll Byteman-Regelgenerierung langfristig Teil der Engine werden oder als optionaler Plugin-/Adapterpfad im Plugin verbleiben?
3. Welche Artefaktform wird die erste stabile Grenze zwischen Plugin und Engine: gRPC DTO, JSON Artefakt, lokaler CLI-Aufruf oder Kombination?
4. Welche Teile des bestehenden H2-Analysis-Store sind kanonische Engine-Persistenz und welche sind nur lokale Plugin-Ausgabe?
5. Welche Joern-Version und welches Container-Image sollen fuer den Docker-Adapter gepinnt werden?
6. Soll die bestehende gRPC-Ingestion vor der lokalen Engine-CLI erweitert werden oder soll zuerst ein lokaler CLI-Analysepfad entstehen?
7. Wie lange muss der Legacy-Modus in `forensics_tracing` erhalten bleiben?

## 8. Erste Risikoeinschaetzung

- Baseline-Konflikt: `forensics_tracing` ist Java 17/JUnit 5, der Migrationsworkflow fordert Java 25/JUnit 6.
- Verantwortungsueberlappung: `BtmGenerationRunner` mischt aktuell Analyseorchestrierung, H2-Persistenz, Artefaktdateien und Byteman-Ausgabe.
- Persistenzgrenze: H2-Modelle koennen nicht unbesehen in die Engine wandern, weil Graph, Replay und Evidenzmodelle spaeter andere Anforderungen haben.
- Joern-Betrieb: Host-CLI-Aufruf existiert im Plugin; die Engine soll Docker-basiert arbeiten.
- API-Stabilitaet: Plugin-Nutzer duerfen nicht durch eine Big-Bang-Migration gebrochen werden.
- Datenklassifikation: Runtime- und Trace-Daten muessen in `forensic_analytics` sensibel behandelt werden; bestehende Plugin-Modelle sind dafuer noch nicht ausreichend.
- Modulwachstum: `forensic_analytics` hat bereits mehrere Module. Neue Module sollten nur entstehen, wenn sie im Slice getestet und genutzt werden.
