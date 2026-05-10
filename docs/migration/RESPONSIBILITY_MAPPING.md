# Responsibility Mapping

## Ziel

Dieses Dokument klassifiziert die derzeit sichtbaren Verantwortlichkeiten aus `forensics_tracing` fuer die kontrollierte Migration nach `forensic_analytics`.
Es ist eine Planungsgrundlage, keine Code-Migration.

## Kategorie A: Muss in forensics_tracing bleiben

Build-Tool-Integration:

- Gradle Plugin ID `de.burger.forensics.btmgen`.
- Gradle Plugin Implementierung `BtmGenPlugin`.
- Gradle Extension `BtmGenExtension`.
- Gradle Tasks:
  - `GenerateBtmTask`
  - `AnalyzeForensicsSemanticsTask`
  - `ImportForensicsSemanticsTask`
- Gradle Task-Konfiguration fuer Inputs, Outputs, LocalState, SourceSets, Subprojects und Runtime Helper Attachment.
- Gradle TestKit Tests.
- Maven Mojo-Klassen:
  - `BtmGenMojo`
  - `BtmGenAggregateMojo`
  - `AnalyzeMojo`
  - `AnalyzeAggregateMojo`
  - `AnalyzeSemanticsMojo`
  - `ImportSemanticsMojo`
  - `CleanForensicsAnalysisMojo`
- Maven Parameter-Mapping, MavenSession-/Reactor-Handling und Maven Plugin Descriptor Konfiguration.
- Build-Tool-spezifische Logging Adapter:
  - `GradlePluginLogAdapter`
  - `GradleLogAdapter`
  - `MavenLogAdapter`
- Plugin Publishing und Plugin Portal Metadaten.
- Consumer-Projekt Wiring, SourceSet-Erkennung und Maven-Reactor-Source-Root-Sammlung.
- Lokale Plugin-Ausgabepfade fuer Legacy-Modus, solange Kompatibilitaet benoetigt wird.

## Kategorie B: Muss nach forensic_analytics wandern

Engine-neutrale Analysemodelle und Konzepte:

- Analyseidentitaet:
  - `AnalysisRunId`
  - `AnalysisRunStatus`
  - `AnalysisSchemaVersion`
  - `BuildId`
  - `BuildIdentity`
  - `SourceFingerprint`
- Artefakt- und Provenienzmodell:
  - `ArtifactChecksum`
  - Analysemanifest-Konzept, sofern es als Engine-Artefakt neu modelliert wird.
- Source- und Scan-Facts:
  - `ScanEvent`
  - `SourceLocation`
  - `SourceContext`
  - `MethodScanContext`
- Semantikmodell:
  - `SemanticAnalysisRequest`
  - `SemanticAnalysisResult`
  - `SemanticNode`
  - `SemanticEdge`
  - `SemanticMethod`
  - `CallRelation`
  - `ControlFlowRelation`
  - `DataFlowPath`
  - `DataFlowStep`
  - `SemanticAnchor`
- Analyse- und Scanner-Ports:
  - `CodeScanPort`
  - `SemanticAnalysisPort`
  - `SemanticAnalysisStorePort`
  - `SourceFingerprintPort`
  - kuenftige Repository-Source-Ports.
- Application-Orchestrierung, soweit sie nicht Build-Tool-spezifisch ist:
  - Source-Scanning koordinieren.
  - Scan-Facts in kanonische Analyseergebnisse ueberfuehren.
  - Semantische Analyse ueber Ports anstossen.
  - Artefaktreferenzen als Engine-Ergebnis modellieren.
- Repository-Analysemodell:
  - `AnalysisRequest`
  - `AnalysisResult`
  - `AnalysisFinding`
  - `AnalysisArtifact`
  - `RepositoryLocation`
  - `RepositoryRevision`
  - `AnalysisProfile`
- Graph-, Replay-, Finding- und Report-Konzepte, sobald sie eingefuehrt werden.

## Kategorie C: Temporaere Duplikation erlaubt

Temporaere Duplikation ist nur als Uebergang erlaubt und muss pro Klasse eine Entfernen-Notiz bekommen.

Zulaessige Kandidaten:

- Kleine Value Objects fuer Analyse-IDs und Artefaktreferenzen, solange Plugin und Engine noch keine gemeinsame Transportgrenze haben.
- DTOs fuer Plugin-zu-Engine Uploads, bis die gRPC-Schemata stabil sind.
- Einfache Source-Root- oder Repository-Metadaten, falls Plugin-Legacy-Modus und Engine-Modus parallel bestehen.
- Byteman-Regel-Metadaten, falls die Engine zuerst nur Artefakte annimmt und die lokale Plugin-Regelgenerierung parallel weiterlaeuft.

Nicht temporaer duplizieren:

- Build-Tool-Task-Klassen.
- Maven Mojo-Klassen.
- Gradle Extension-Modelle.
- Konkrete Joern-CLI- oder Docker-Ausfuehrung.
- H2-Persistenzimplementierungen ohne klares Zielmodell.

## Kategorie D: Spaeter gemeinsamer Vertrag

Diese Verantwortlichkeiten sollten als stabiler Vertrag zwischen Plugin und Engine entstehen:

- gRPC Request/Response DTOs fuer Analyse-Sessions.
- Plugin-to-Engine Analyseanforderung:
  - Projektkennung
  - Repository-Metadaten
  - Branch/Commit
  - Module
  - Source Roots
  - Build Tool
  - Profil
- Upload-Modell fuer Scan-Facts und Artefakte.
- Analyseergebnis- und Finding-Upload-Modell.
- Artefaktreferenzmodell mit Typ, Pfad/URI, Checksumme und Provenienz.
- Runtime-Trace-Event-Modell mit Sensitivitaetsklassifikation.
- Repository-Metadatenmodell.
- Analyseprofil-Schema.
- Fehler-/Limitationsmodell fuer unvollstaendige Evidenz.

## Kategorie E: Spaeter loeschen

Nur mit verifiziertem Ersatzplan loeschen:

- Plugin-interne Vollorchestrierung fuer semantische Analyse, wenn die Engine den lokalen oder remote Analysepfad uebernimmt.
- Direkte Joern-CLI-Ausfuehrung im Plugin, wenn Engine-Docker-Joern stabil ist und Legacy-Modus abgeloest wurde.
- Plugin-interne H2-Analyseablage, wenn Engine-Persistenz und Artefaktupload stabil sind.
- Manifest-/Checksum-Schreiblogik im Plugin, wenn Engine-Artefakte als Quelle der Wahrheit dienen.
- Doppelte DTOs nach Stabilisierung der gRPC-Grenze.

Nicht loeschen, bevor ein kompatibler Ersatz existiert:

- Lokale BTM-Generierung fuer bestehende Nutzer.
- Runtime-Tracing-Helfer, solange Byteman-Regeln sie referenzieren.
- Gradle/Maven Registrierungs- und Konfigurationslogik.

## Kategorie F: Unklar / Entscheidung erforderlich

1. Java-/JUnit-Baseline fuer `forensics_tracing`:
   - Aktuell ist das Plugin-Repository Java 17/JUnit 5.
   - Der Migrationsworkflow fordert Java 25/JUnit 6.
   - Entscheidung: sofortige Plugin-Baseline-Migration oder zunaechst Engine-only Java 25?

2. Byteman Ownership:
   - `Rule`, `RuleTemplate`, `RuleRenderPort`, Renderer und Strategien koennen Engine-relevant sein.
   - Gleichzeitig ist Byteman ein konkretes Instrumentierungsformat.
   - Entscheidung: Byteman als Engine-Adapter oder weiterhin Plugin-Legacy-Komponente?

3. Analyse-Store Ownership:
   - `forensics_tracing` speichert H2-Analysezustand lokal.
   - `forensic_analytics` braucht spaeter kanonische Persistenz, Graph-Projektionen und Replay.
   - Entscheidung: H2-Store migrieren, ersetzen oder nur als Legacy-Artefakt importieren?

4. Joern-Modus:
   - Plugin nutzt Host-CLI.
   - Workflow fordert Joern Docker Adapter in der Engine.
   - Entscheidung: Host-CLI als Legacy-Adapter behalten oder vollstaendig durch Container ersetzen?

5. Erste nutzbare Engine-Schnittstelle:
   - Bestehend ist gRPC-Ingestion.
   - Workflow fordert spaeter CLI.
   - Entscheidung: zuerst CLI-Analysepfad oder gRPC-Payload-Schema erweitern?

6. Runtime-Tracing:
   - `RtTrace`, `RtEvent`, `RtTracer` sind in `forensics_tracing` vorhanden.
   - Engine-Zielbild benoetigt Runtime Event Import, Redaction und Replay.
   - Entscheidung: Runtime-Helfer im Plugin/Runtime-Artefakt belassen und nur Event-Schema in Engine definieren?

## Erste empfohlene Klassifikation fuer Slice 01

Da `forensic_analytics` bereits Domain/Application/gRPC/Persistence/Bootstrap hat, sollte Slice 01 nicht einfach dieselben Module neu erzeugen.

Empfohlen fuer den naechsten Implementation-Slice:

- Bestehende Module pruefen und dokumentieren statt neu anlegen.
- Ein fehlendes `forensic-analytics-engine` Modul nur dann anlegen, wenn ein minimaler Use Case mit Test entsteht.
- Ein `forensic-analytics-cli` Modul nur dann anlegen, wenn ein minimaler CLI-Aufruf mit Test entsteht.
- Keine Klassen aus `forensics_tracing` entfernen.
- Keine Gradle-/Maven-Adapter in die Engine verschieben.
