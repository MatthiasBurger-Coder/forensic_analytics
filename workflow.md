# Workplan: `forensic_analytics` — Cross-Repo-Handoff finalisieren und Migration verifizieren

## 1. Ziel

Dieses Workplan-Dokument beschreibt die notwendigen Schritte im Repository `forensic_analytics`, damit die Migration aus `forensics_tracing` als Zielplattform vollständig verifiziert ist.

Zielzustand:

```text
Repository: forensic_analytics
Rolle: Forensic Analytics Plattform / Engine
Baseline: Java 25, JUnit 6, Gradle 9.4.0
Ingestion: kann engine-request.json aus forensics_tracing importieren
CLI: ingest-request ist stabil verifiziert
Testbed: enthält einen realistischen Handoff-Test
Status: Migration ist dokumentiert und nachvollziehbar abgeschlossen
```

---

## 2. Ausgangslage

Der Vergleich hat gezeigt:

* `forensic_analytics/main` besitzt bereits die Zielstruktur als Multi-Projekt-Monorepo.
* Java 25, JUnit 6 und Gradle 9.4.0 sind in `forensic_analytics` aktiv.
* gRPC-Ingestion, lokale Request-Ingestion, CLI, Testbed, Engine, Repository-Adapter und Joern-Docker-Adapter sind vorhanden.
* Die Analytics-Seite kann ein `engine-request.json` lesen und importieren.
* Der echte Abschluss hängt davon ab, dass ein aus `forensics_tracing/main` erzeugter Request importiert wird.

---

## 3. Non-Goals

Nicht Teil dieses Workplans:

* Keine Build-Tool-Adapter aus `forensics_tracing` nach `forensic_analytics` verschieben.
* Kein direkter Compile-Dependency auf `forensics_tracing`.
* Kein gRPC-Client-Zwang für den lokalen Handoff.
* Keine Pflicht für Docker oder Joern im Standard-Quality-Gate.
* Keine Graph-, Replay- oder LLM-Funktionalität erfinden.
* Keine künstlichen Runtime-Traces erzeugen.
* Keine Senkung von Coverage- oder Architekturregeln.

---

## 4. Zielarchitektur für dieses Repository

```text
forensic_analytics
  -> domain
  -> application
  -> engine
  -> ingestion-request
  -> ingestion-grpc
  -> cli
  -> persistence
  -> repository-source adapter
  -> joern-docker adapter
  -> testbed
  -> bootstrap/server
```

Dependency-Richtung:

```text
cli -> ingestion-request/application/domain
bootstrap -> ingestion-grpc -> application -> domain
engine -> application -> domain
adapters -> application/domain
persistence -> application/domain
```

Verboten:

```text
domain -> gRPC
domain -> CLI
domain -> persistence
domain -> Joern/Docker
domain -> forensics_tracing
application -> concrete adapters
analytics -> Gradle/Maven plugin lifecycle
```

---

## 5. Slice 0 — Preflight

### Ziel

Arbeitszustand prüfen und sicherstellen, dass `forensic_analytics/main` aktuell ist.

### Commands

```bash
git status --short
git branch --show-current
git fetch --all --prune
git switch main
git pull --ff-only
java --version
./gradlew --version
```

Windows PowerShell:

```powershell
git status --short
git branch --show-current
git fetch --all --prune
git switch main
git pull --ff-only
java --version
.\gradlew.bat --version
```

### Akzeptanzkriterien

```text
[ ] Working Tree ist sauber oder lokale Änderungen sind dokumentiert.
[ ] main ist aktuell.
[ ] Java 25 ist aktiv.
[ ] Gradle Wrapper nutzt 9.4.0.
[ ] Tests können grundsätzlich gestartet werden.
```

---

## 6. Slice 1 — Ingestion-Vertrag gegen Plugin-Handoff prüfen

### Ziel

Sicherstellen, dass `forensic_analytics` exakt die Struktur importieren kann, die `forensics_tracing` erzeugt.

### Zu prüfende Analytics-Dateien

```text
forensic-analytics-ingestion-request/src/main/java/**/EngineIngestionRequestReader.java
forensic-analytics-ingestion-request/src/main/java/**/EngineIngestionRequestImporter.java
forensic-analytics-domain/src/main/java/**/AnalysisPayloadKind.java
forensic-analytics-domain/src/main/java/**/AnalysisPayloadDescriptor.java
forensic-analytics-application/src/main/java/**/ForensicIngestionUseCase.java
forensic-analytics-cli/src/main/java/**/ForensicAnalyticsCli.java
forensic-analytics-testbed/src/test/java/**/*.java
```

### Erwarteter JSON-Vertrag

```json
{
  "schemaVersion": "...",
  "buildIdentity": {
    "projectId": "...",
    "repositoryUrl": "...",
    "branchName": "...",
    "commitHash": "...",
    "buildId": "...",
    "scanTimestamp": "..."
  },
  "moduleIdentity": {
    "moduleName": "...",
    "modulePath": "..."
  },
  "pluginIdentity": {
    "pluginName": "forensics-tracing",
    "pluginVersion": "..."
  },
  "payloads": [
    {
      "payloadId": "byteman-rules",
      "kind": "RULE_ARTIFACTS",
      "contentType": "text/x-byteman",
      "file": "...",
      "attributes": {
        "artifact": "btm-rules"
      }
    }
  ]
}
```

### Payload-Kinds müssen exakt unterstützt werden

```text
SOURCE_FACTS
SEMANTIC_ARTIFACTS
RULE_ARTIFACTS
RUNTIME_TRACE
DIAGNOSTIC_REPORT
```

### Commands

```bash
rg -n "AnalysisPayloadKind|RULE_ARTIFACTS|DIAGNOSTIC_REPORT|EngineIngestionRequestReader|EngineIngestionRequestImporter|ingest-request" \
  forensic-analytics-domain forensic-analytics-application forensic-analytics-ingestion-request forensic-analytics-cli forensic-analytics-testbed
```

### Akzeptanzkriterien

```text
[ ] Alle Plugin-Payload-Kinds sind in Analytics vorhanden.
[ ] Reader erwartet dieselben Feldnamen wie der Plugin-Writer erzeugt.
[ ] Relative und absolute Payload-Pfade werden unterstützt.
[ ] Fehlende Payload-Dateien führen zu klarer Fehlermeldung.
[ ] Kein protobuf/gRPC DTO leakt in Domain oder Application.
```

---

## 7. Slice 2 — Reales Plugin-Request-Fixture aufnehmen

### Ziel

Ein Fixture oder Testfall mit einer realistischen `engine-request.json`-Struktur aus `forensics_tracing` absichern.

### Vorgehen

Variante A — bevorzugt:

```text
- Ein im Test temporär erzeugtes engine-request.json verwenden.
- Struktur muss exakt dem Plugin-Writer entsprechen.
- Payload-Dateien werden real temporär geschrieben.
- Keine direkte Dependency auf forensics_tracing.
```

Variante B — optional:

```text
- Ein dokumentiertes Fixture unter forensic-analytics-testbed/src/test/resources anlegen.
- Payload-Dateien als kleine Testartefakte daneben ablegen.
- Nur verwenden, wenn die Fixture-Dateien stabil und bewusst versioniert werden sollen.
```

### Tests

Ergänzen oder prüfen:

```text
EngineIngestionRequestReaderTest
EngineIngestionRequestImporterTest
ForensicAnalyticsCliTest
RepositoryAnalysisTestbedTest
```

### Testfälle

```text
[ ] Importiert RULE_ARTIFACTS Payload.
[ ] Importiert DIAGNOSTIC_REPORT Payloads für Manifest/Checksums.
[ ] Importiert mehrere Payloads in stabiler Reihenfolge.
[ ] Schlägt fehl, wenn payloads leer ist.
[ ] Schlägt fehl, wenn Payload-Datei fehlt.
[ ] Schlägt fehl, wenn kind unbekannt ist.
[ ] Akzeptiert pluginName=forensics-tracing.
[ ] Schreibt Summary mit requestFile, status und uploadedPayloads.
```

### Commands

```bash
./gradlew :forensic-analytics-ingestion-request:test \
  :forensic-analytics-cli:test \
  :forensic-analytics-testbed:test \
  --dependency-verification strict \
  --console=plain \
  --stacktrace
```

Windows:

```powershell
.\gradlew.bat :forensic-analytics-ingestion-request:test `
  :forensic-analytics-cli:test `
  :forensic-analytics-testbed:test `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

### Akzeptanzkriterien

```text
[ ] Analytics-Test nutzt eine Handoff-Struktur, die dem Plugin-Writer entspricht.
[ ] Import funktioniert ohne forensics_tracing Compile-Dependency.
[ ] Test deckt mindestens RULE_ARTIFACTS und DIAGNOSTIC_REPORT ab.
[ ] Summary wird geprüft.
```

---

## 8. Slice 3 — Cross-Repo-Smoke mit echtem `forensics_tracing` Output

### Ziel

Den tatsächlichen lokalen Handoff zwischen beiden Repositories ausführen.

Dieser Slice ist ein manueller oder dokumentierter Integrations-Smoke und muss nicht zwingend im Standard-Unit-Gate laufen.

### Voraussetzung

In `forensics_tracing/main` muss der Engine-Handoff gemergt sein.

### Schritt 1 — Request in `forensics_tracing` erzeugen

Im Repository `forensics_tracing`:

```bash
./gradlew generateBtmRules \
  -Pforensics.engineRequestEnabled=true \
  -Pforensics.engineRequestFile=build/forensics/engine-request.json \
  --dependency-verification strict \
  --console=plain \
  --stacktrace
```

Windows:

```powershell
.\gradlew.bat generateBtmRules `
  -Pforensics.engineRequestEnabled=true `
  -Pforensics.engineRequestFile=build\forensics\engine-request.json `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

### Schritt 2 — Request in `forensic_analytics` importieren

Im Repository `forensic_analytics`:

```bash
./gradlew :forensic-analytics-cli:run \
  --args="ingest-request --request <path-to-forensics_tracing>/build/forensics/engine-request.json --output build/forensics/handoff-smoke" \
  --dependency-verification strict \
  --console=plain \
  --stacktrace
```

Windows:

```powershell
.\gradlew.bat :forensic-analytics-cli:run `
  --args="ingest-request --request D:\Projects\forensics_tracing\build\forensics\engine-request.json --output build\forensics\handoff-smoke" `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

### Erwartete Datei

```text
build/forensics/handoff-smoke/engine-request-import-summary.txt
```

### Erwarteter Inhalt

```text
status=COMPLETED
uploadedPayloads=1
```

Bei aktiviertem Analysis Store können es mehrere Payloads sein:

```text
uploadedPayloads>=1
```

### Akzeptanzkriterien

```text
[ ] Echtes engine-request.json aus forensics_tracing wird importiert.
[ ] Alle referenzierten Payload-Dateien sind lesbar.
[ ] CLI beendet mit Exit Code 0.
[ ] Summary enthält status=COMPLETED.
[ ] uploadedPayloads ist mindestens 1.
```

---

## 9. Slice 4 — Testbed-Härtung für Cross-Repo-Handoff

### Ziel

Den Handoff als dauerhaft nachvollziehbares Testbed-Szenario dokumentieren oder absichern.

### Option A — Dokumentierter Testbed-Smoke

Ergänzen:

```text
docs/migration/CROSS_REPO_HANDOFF_SMOKE.md
```

Inhalt:

```text
- Voraussetzung: forensics_tracing main mit Engine Request
- Command zum Erzeugen des Request
- Command zum Import in forensic_analytics
- Erwartete Summary
- Bekannte Grenzen
- Nicht Teil des Standard-Gates, weil zwei Repositories beteiligt sind
```

### Option B — Testbed-Script

Optional ein Script im Testbed anlegen:

```text
forensic-analytics-testbed/src/test/resources oder scripts/
```

Aber nur, wenn es keine festen lokalen Pfade erzwingt.

Mögliche Parameter:

```text
FORENSICS_TRACING_REPO
FORENSIC_ANALYTICS_REPO
ENGINE_REQUEST_FILE
HANDOFF_OUTPUT_DIR
```

### Nicht tun

```text
- Keine absolute D:\Projects-Pfade hart codieren.
- Keine forensics_tracing Compile-Dependency hinzufügen.
- Kein Docker oder Joern für diesen Smoke erzwingen.
```

### Akzeptanzkriterien

```text
[ ] Handoff-Smoke ist reproduzierbar dokumentiert.
[ ] Keine lokalen Pfade sind fest verdrahtet.
[ ] Testbed bleibt ohne externe Services im Standard-Gate lauffähig.
```

---

## 10. Slice 5 — CLI-Ausgabe und Fehlerdiagnostik final prüfen

### Ziel

Sicherstellen, dass der CLI-Import nutzbar und fehlertolerant ist.

### Zu prüfende Fälle

```text
[ ] --help zeigt ingest-request.
[ ] fehlendes --request erzeugt klare Fehlermeldung.
[ ] fehlende Request-Datei erzeugt klare Fehlermeldung.
[ ] fehlende Payload-Datei erzeugt klare Fehlermeldung.
[ ] unbekanntes payload kind erzeugt klare Fehlermeldung.
[ ] erfolgreicher Import schreibt Summary.
[ ] Standalone ServiceLoader-Pfad benötigt keinen RunRepositoryAnalysisUseCase für ingest-request.
```

### Commands

```bash
./gradlew :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-cli:run --args="--help" --dependency-verification strict --console=plain --stacktrace
```

Windows:

```powershell
.\gradlew.bat :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat :forensic-analytics-cli:run --args="--help" --dependency-verification strict --console=plain --stacktrace
```

### Akzeptanzkriterien

```text
[ ] CLI-Verhalten ist getestet.
[ ] Fehler sind für Nutzer verständlich.
[ ] ingest-request bleibt unabhängig vom analyze-ServiceLoader.
```

---

## 11. Slice 6 — Migration Status finalisieren

### Ziel

Die Migration dokumentiert abschließen oder bewusst offene Punkte klar markieren.

### Neue oder zu aktualisierende Datei

```text
docs/migration/MIGRATION_STATUS.md
```

### Inhalt

```markdown
# Migration Status: forensics_tracing -> forensic_analytics

## Completed

- Java 25 / JUnit 6 baseline in forensic_analytics
- Engine module
- Domain models
- Application use case
- Repository source adapter
- Joern Docker adapter boundary
- CLI analyze command
- gRPC ingestion payload descriptors
- Engine request ingestion
- CLI ingest-request command
- Testbed handoff scenario
- Standalone ingest-request wiring

## Completed in forensics_tracing

- Engine request generation on main: yes/no
- Gradle mapping: yes/no
- Maven mapping: yes/no
- Legacy mode retained: yes/no
- Java 25 / JUnit 6 baseline: yes/no

## Verified Cross-Repo

- Request generated by forensics_tracing: yes/no
- Request imported by forensic_analytics: yes/no
- uploadedPayloads: n
- Summary path: ...

## Open

- Direct gRPC client in plugin
- Persistent Analytics storage beyond in-memory
- Graph model
- Replay model
- Report model
- LLM context
- Real Joern image digest decision
- WildFly performance smoke

## Final Assessment

Migration is complete/incomplete because ...
```

### Akzeptanzkriterien

```text
[ ] Statusdokument enthält beide Repositories.
[ ] Cross-Repo-Smoke ist mit Ergebnis dokumentiert.
[ ] Offene Punkte sind nicht als erledigt dargestellt.
[ ] Keine unbestätigten Annahmen werden als Fakt formuliert.
```

---

## 12. Slice 7 — Full Quality Gate

### Ziel

Finale lokale Qualitätssicherung für `forensic_analytics`.

### Commands

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage \
  --dependency-verification strict \
  --console=plain \
  --stacktrace
```

Windows:

```powershell
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage `
  --dependency-verification strict `
  --console=plain `
  --stacktrace
```

Optional, falls Sonar-Token vorhanden:

```bash
./gradlew sonar --dependency-verification strict --console=plain --stacktrace
```

### Akzeptanzkriterien

```text
[ ] Full Quality Gate läuft erfolgreich.
[ ] Dependency Verification läuft strict.
[ ] Package Coverage läuft erfolgreich.
[ ] Sonar wird ausgeführt oder sauber mit Skip-Grund dokumentiert.
```

---

## 13. Slice 8 — PR/Commit Abschluss

### Ziel

Alle Analytics-Änderungen sauber committen und dokumentieren.

### Git-Prüfung

```bash
git status --short
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

### Empfohlene Commit-Nachrichten

Wenn nur Tests/Dokumentation ergänzt werden:

```text
test(migration): verify tracing engine request handoff
```

Wenn auch CLI/Importer angepasst werden müssen:

```text
fix(ingestion): align engine request intake with tracing handoff
```

Wenn Statusdokument ergänzt wird:

```text
docs(migration): record cross-repo handoff status
```

### Commit Body muss enthalten

```text
What:
- ...

Why:
- ...

How:
- ...

Verification:
- ...

Limitations:
- ...
```

---

## 14. Vollständige Definition of Done

```text
[ ] forensic_analytics/main kann plugin-erzeugtes engine-request.json importieren.
[ ] CLI ingest-request läuft mit echtem Cross-Repo-Artefakt.
[ ] Testbed oder Dokumentation sichert den Smoke reproduzierbar ab.
[ ] Keine direkte Dependency auf forensics_tracing wurde eingeführt.
[ ] Domain/Application bleiben frei von gRPC-/CLI-/Persistence-/Docker-Leaks.
[ ] Full Quality Gate läuft strict.
[ ] MIGRATION_STATUS.md dokumentiert erledigte und offene Punkte.
[ ] Offene Graph-/Replay-/LLM-/Persistenz-Themen sind klar als spätere Arbeit markiert.
```

---

## 15. Stop-and-Report-Fälle

Sofort stoppen und berichten, wenn:

```text
- forensics_tracing/main noch kein engine-request.json erzeugen kann.
- Plugin-Request-Felder nicht zum Analytics-Reader passen.
- Payload-Kinds auseinanderlaufen.
- Payload-Dateien im Request nicht lesbar sind.
- CLI ingest-request den analyze-ServiceLoader erzwingt.
- Domain oder Application eine technische Adapter-Abhängigkeit bekommt.
- Full Quality Gate fehlschlägt.
- Dependency Verification neue unerwartete Trust-Regeln verlangt.
```

Der Bericht muss enthalten:

```text
- ausgeführter Command
- konkrete Fehlermeldung
- betroffene Datei
- vermutete Ursache
- ob der Fehler die Migration blockiert
- empfohlener nächster Schritt
```

---

## 16. Finaler Report

Am Ende muss der Agent berichten:

```text
- Geänderte Dateien
- Importvertrag geprüft: ja/nein
- Echtes Plugin-Request importiert: ja/nein
- CLI Summary Pfad
- uploadedPayloads
- Full Quality Gate Ergebnis
- Sonar Ergebnis oder Skip-Grund
- Offene Punkte
- Bewertung: Migration vollständig / teilweise / blockiert
```
