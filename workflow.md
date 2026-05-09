# Workflow: Eigenes gRPC-Ingestion-Modul für `forensic_analytics` anlegen

**Status:** Draft
**Projekt:** forensic_analytics
**Kontext:** Multi-Projekt-Monorepo
**Ziel:** Eigenes Modul bereitstellen, das Analyse-/Scan-Daten vom Plugin per gRPC entgegennimmt
**Architektur:** Hexagonale Architektur, gRPC als Inbound Adapter
**Wichtig:** Die finale Datenstruktur des Plugin-Payloads wird separat festgelegt und ist nicht Teil dieses Workflows.

---

## 1. Zielbild

Der erste technische Schritt für die Kommunikation zwischen Plugin und `forensic_analytics` ist ein eigenes gRPC-Ingestion-Modul.

Dieses Modul stellt einen gRPC-Service bereit, über den das Plugin später seine Analyse-/Scan-Daten an die zentrale Plattform sendet.

```text
Plugin
  -> gRPC Client
    -> forensic-analytics-ingestion-grpc
      -> Application Use Case
        -> Persistence Port
          -> Persistence Adapter
```

Das neue Modul ist ein reiner **Inbound Adapter**. Es empfängt Daten, validiert den Transportkontext, mappt Protobuf-DTOs auf Application Commands und ruft den Application-Layer auf.

---

## 2. Nicht-Ziele dieses Workflows

Dieser Workflow soll ausdrücklich **nicht** folgende Themen implementieren:

```text
- finale Plugin-Datenstruktur festlegen
- Joern integrieren
- Code Property Graph erzeugen
- Replay-Logik implementieren
- LLM-Kontext erzeugen
- Daten fachlich auswerten
- direkte Datenbanklogik im gRPC-Modul implementieren
- Plugin-Client vollständig implementieren
- bestehende BTM-Generierung verändern
```

Das Ziel ist nur die technische Grundlage:

```text
forensic_analytics kann gRPC-Daten vom Plugin empfangen.
```

---

## 3. Gewünschte Modulstruktur

Falls das Monorepo bereits eine andere Namenskonvention verwendet, muss diese beibehalten werden. Nicht blind neue Konventionen einführen.

Zielstruktur, falls noch keine verbindliche Struktur existiert:

```text
forensic-analytics/
├── forensic-analytics-domain/
├── forensic-analytics-application/
├── forensic-analytics-persistence/
├── forensic-analytics-ingestion-grpc/
├── forensic-analytics-bootstrap/
├── settings.gradle.kts
└── build.gradle.kts
```

Neues Modul:

```text
forensic-analytics-ingestion-grpc
```

Verantwortung:

```text
- gRPC Service Definition bereitstellen
- Protobuf-Klassen generieren
- gRPC Endpoint implementieren
- Requests entgegennehmen
- Transportdaten validieren
- Protobuf DTOs auf Application Commands mappen
- Application Use Cases aufrufen
- gRPC Responses zurückgeben
```

---

## 4. Architekturregeln

### 4.1 Modul als Inbound Adapter

Das Modul `forensic-analytics-ingestion-grpc` ist ein Adapter nach innen.

Erlaubt:

```text
forensic-analytics-ingestion-grpc
  -> forensic-analytics-application
  -> generated protobuf/grpc classes
  -> gRPC runtime
```

Nicht erlaubt:

```text
forensic-analytics-ingestion-grpc
  -> direkte Datenbankzugriffe
  -> Joern
  -> LLM-Komponenten
  -> Runtime Replay
  -> Plugin-Interna
```

### 4.2 Kein Domain Leakage

Protobuf-Klassen dürfen nicht als Domain-Objekte verwendet werden.

Es muss gemappt werden:

```text
Proto DTO
  -> Application Command
    -> Domain Model
```

### 4.3 Finale Payload-Struktur bleibt offen

Da die vom Plugin gelieferte Datenstruktur separat festgelegt wird, darf dieser Workflow nur einen stabilen technischen Rahmen schaffen.

Erlaubt ist ein generisches Ingestion Envelope mit minimalem Payload-Platzhalter.

Beispiel:

```text
AnalysisDataEnvelope
├── sessionId
├── buildIdentity
├── moduleIdentity
├── pluginIdentity
├── schemaVersion
├── payloadType
└── payloadBytes | jsonPayload | reserved placeholder
```

Die konkrete Struktur wie `ClassPayload`, `MethodPayload`, `DependencyPayload`, `GeneratedRulePayload` usw. wird später ergänzt.

---

## 5. Vorgeschlagener gRPC-Vertrag für den ersten Slice

Die konkrete Protobuf-Datei soll minimal bleiben, aber bereits session-orientiert sein.

Zielservice:

```proto
syntax = "proto3";

package de.burger.forensics.analytics.ingestion.v1;

option java_multiple_files = true;
option java_package = "de.burger.forensics.analytics.ingestion.v1";
option java_outer_classname = "ForensicIngestionProto";

service ForensicIngestionService {
  rpc StartAnalysisSession(StartAnalysisSessionRequest)
      returns (StartAnalysisSessionResponse);

  rpc UploadAnalysisData(stream AnalysisDataEnvelope)
      returns (UploadAnalysisDataResponse);

  rpc CompleteAnalysisSession(CompleteAnalysisSessionRequest)
      returns (CompleteAnalysisSessionResponse);

  rpc AbortAnalysisSession(AbortAnalysisSessionRequest)
      returns (AbortAnalysisSessionResponse);
}

message StartAnalysisSessionRequest {
  BuildIdentity build_identity = 1;
  PluginIdentity plugin_identity = 2;
  string schema_version = 3;
}

message StartAnalysisSessionResponse {
  string session_id = 1;
  IngestionStatus status = 2;
  string message = 3;
}

message AnalysisDataEnvelope {
  string session_id = 1;
  BuildIdentity build_identity = 2;
  ModuleIdentity module_identity = 3;
  PluginIdentity plugin_identity = 4;
  string schema_version = 5;
  string payload_type = 6;
  bytes payload = 7;
}

message UploadAnalysisDataResponse {
  string session_id = 1;
  IngestionStatus status = 2;
  int64 received_items = 3;
  string message = 4;
}

message CompleteAnalysisSessionRequest {
  string session_id = 1;
}

message CompleteAnalysisSessionResponse {
  string session_id = 1;
  IngestionStatus status = 2;
  string message = 3;
}

message AbortAnalysisSessionRequest {
  string session_id = 1;
  string reason = 2;
}

message AbortAnalysisSessionResponse {
  string session_id = 1;
  IngestionStatus status = 2;
  string message = 3;
}

message BuildIdentity {
  string project_id = 1;
  string repository_url = 2;
  string branch_name = 3;
  string commit_hash = 4;
  string build_id = 5;
  string scan_timestamp = 6;
}

message ModuleIdentity {
  string module_name = 1;
  string module_path = 2;
}

message PluginIdentity {
  string plugin_name = 1;
  string plugin_version = 2;
}

enum IngestionStatus {
  INGESTION_STATUS_UNSPECIFIED = 0;
  INGESTION_STATUS_ACCEPTED = 1;
  INGESTION_STATUS_COMPLETED = 2;
  INGESTION_STATUS_ABORTED = 3;
  INGESTION_STATUS_REJECTED = 4;
}
```

Wichtig: Dieses Proto ist nur der erste technische Rahmen. Der spätere fachliche Payload wird separat erweitert.

---

## 6. Slice-Plan

## Slice 1: Repository-Struktur prüfen

### Ziel

Vor Änderungen muss die bestehende Projektstruktur analysiert werden.

### Aufgaben

```text
1. Prüfe vorhandene Module.
2. Prüfe Build-System und Namenskonventionen.
3. Prüfe vorhandene Package-Struktur.
4. Prüfe, ob bereits application/domain/bootstrap Module existieren.
5. Prüfe, ob bereits gRPC, Protobuf oder Spring/Netty verwendet wird.
```

### Erwartetes Ergebnis

Eine kurze Analyse im Codex-Output:

```text
- Build-System erkannt: Gradle/Maven
- vorhandene Module erkannt
- Zielposition für neues Modul bestimmt
- notwendige Build-Anpassungen identifiziert
```

### Stop-Regel

Wenn die vorhandene Struktur nicht eindeutig ist, nicht raten. Stoppen und berichten:

```text
STOP: Existing module structure is ambiguous. Manual decision required.
```

---

## Slice 2: Neues Modul `forensic-analytics-ingestion-grpc` anlegen

### Ziel

Ein eigenes Modul für gRPC-Ingestion existiert im Monorepo.

### Aufgaben bei Gradle

```text
1. Modulverzeichnis anlegen:
   forensic-analytics-ingestion-grpc/

2. Build-Datei anlegen:
   forensic-analytics-ingestion-grpc/build.gradle.kts

3. Modul in settings.gradle.kts registrieren.

4. Java-Source-Sets anlegen:
   src/main/java
   src/main/proto
   src/test/java

5. Modulabhängigkeiten setzen:
   - application module
   - protobuf plugin
   - grpc runtime
   - test dependencies
```

### Beispielhafte Gradle-Richtung

Nur verwenden, wenn es zur bestehenden Projektstruktur passt:

```kotlin
plugins {
    java
    id("com.google.protobuf")
}

dependencies {
    implementation(project(":forensic-analytics-application"))

    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("com.google.protobuf:protobuf-java")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.grpc:grpc-testing")
}
```

Wenn das Projekt Version Catalogs verwendet, müssen die Dependencies über `libs.versions.toml` eingebunden werden.

### Erwartetes Ergebnis

```text
- Modul ist registriert
- Modul kompiliert grundsätzlich
- Source-Sets existieren
```

### Verifikation

```bash
./gradlew projects
./gradlew :forensic-analytics-ingestion-grpc:compileJava
```

---

## Slice 3: Protobuf-Datei anlegen

### Ziel

Der gRPC-Vertrag für den ersten technischen Ingestion-Rahmen existiert.

### Aufgaben

```text
1. Datei anlegen:
   forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto

2. Minimalen Service definieren:
   - StartAnalysisSession
   - UploadAnalysisData
   - CompleteAnalysisSession
   - AbortAnalysisSession

3. Minimale Identity Messages definieren:
   - BuildIdentity
   - ModuleIdentity
   - PluginIdentity

4. Envelope definieren:
   - sessionId
   - buildIdentity
   - moduleIdentity
   - pluginIdentity
   - schemaVersion
   - payloadType
   - payload bytes

5. Keine finale fachliche Payload-Struktur modellieren.
```

### Erwartetes Ergebnis

```text
- Protobuf-Datei existiert
- gRPC Java-Klassen werden generiert
- compileJava läuft erfolgreich
```

### Verifikation

```bash
./gradlew :forensic-analytics-ingestion-grpc:generateProto
./gradlew :forensic-analytics-ingestion-grpc:compileJava
```

---

## Slice 4: Application-Port vorbereiten oder anbinden

### Ziel

Das gRPC-Modul ruft keine Datenbank direkt auf, sondern einen Application Use Case.

### Aufgaben

Prüfe zuerst, ob bereits ein passender Use Case oder Port existiert.

Falls nicht vorhanden, im Application-Modul minimal vorbereiten:

```text
ReceiveAnalysisSessionUseCase
ReceiveAnalysisDataUseCase
CompleteAnalysisSessionUseCase
AbortAnalysisSessionUseCase
```

Alternativ als ein zusammenhängender Port:

```java
public interface ForensicIngestionUseCase {
    StartAnalysisSessionResult start(StartAnalysisSessionCommand command);
    UploadAnalysisDataResult upload(UploadAnalysisDataCommand command);
    CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command);
    AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command);
}
```

### Regeln

```text
- Commands liegen im Application-Modul.
- Keine Protobuf-Klassen im Application-Modul verwenden.
- Keine gRPC-Abhängigkeiten im Application-Modul einführen.
- Keine Persistenzlogik im gRPC-Modul.
```

### Erwartetes Ergebnis

```text
- Application-Port existiert oder wurde korrekt angebunden
- gRPC-Modul kann gegen Application-Port kompilieren
```

### Verifikation

```bash
./gradlew :forensic-analytics-application:compileJava
./gradlew :forensic-analytics-ingestion-grpc:compileJava
```

---

## Slice 5: Proto-zu-Command Mapper implementieren

### Ziel

Das gRPC-Modul mappt Transportdaten sauber auf Application Commands.

### Vorgeschlagene Klassen

```text
forensic-analytics-ingestion-grpc/src/main/java/.../mapper/
├── BuildIdentityMapper.java
├── ModuleIdentityMapper.java
├── PluginIdentityMapper.java
├── AnalysisDataEnvelopeMapper.java
└── IngestionStatusMapper.java
```

### Regeln

```text
- Mapper sind deterministisch und zustandslos.
- Keine Business-Logik in Mappern.
- Null/Leerwertprüfung entweder im Validator oder beim Command-Aufbau.
- Protobuf bleibt im Adapter.
```

### Erwartetes Ergebnis

```text
Proto DTO -> Application Command funktioniert isoliert testbar.
```

### Tests

```text
- BuildIdentity wird korrekt gemappt
- ModuleIdentity wird korrekt gemappt
- PluginIdentity wird korrekt gemappt
- payload bytes werden unverändert übernommen
- schemaVersion wird unverändert übernommen
- payloadType wird unverändert übernommen
```

---

## Slice 6: Request-Validierung implementieren

### Ziel

Ungültige Requests werden früh und nachvollziehbar abgelehnt.

### Vorgeschlagene Klassen

```text
validator/
├── StartAnalysisSessionRequestValidator.java
├── AnalysisDataEnvelopeValidator.java
├── CompleteAnalysisSessionRequestValidator.java
└── AbortAnalysisSessionRequestValidator.java
```

### Minimale Validierungsregeln

StartAnalysisSession:

```text
- projectId darf nicht leer sein
- repositoryUrl darf nicht leer sein
- commitHash darf nicht leer sein
- buildId darf nicht leer sein
- pluginName darf nicht leer sein
- pluginVersion darf nicht leer sein
- schemaVersion darf nicht leer sein
```

UploadAnalysisData:

```text
- sessionId darf nicht leer sein
- payloadType darf nicht leer sein
- schemaVersion darf nicht leer sein
- payload darf nicht leer sein
```

CompleteAnalysisSession:

```text
- sessionId darf nicht leer sein
```

AbortAnalysisSession:

```text
- sessionId darf nicht leer sein
- reason sollte nicht leer sein
```

### Fehlerverhalten

Ungültige Requests sollen mit passendem gRPC Status beantwortet werden:

```text
INVALID_ARGUMENT bei fehlenden Pflichtfeldern
FAILED_PRECONDITION bei ungültigem Session-Zustand
INTERNAL bei unerwarteten Fehlern
UNAVAILABLE bei temporärer Nichtverfügbarkeit abhängiger Komponenten
```

---

## Slice 7: gRPC Service implementieren

### Ziel

Der gRPC-Service ist technisch lauffähig und ruft den Application Use Case auf.

### Vorgeschlagene Klasse

```text
ForensicIngestionGrpcService.java
```

### Verantwortlichkeiten

```text
- StartAnalysisSessionRequest empfangen
- Request validieren
- Request in Command mappen
- Application Use Case aufrufen
- Result in gRPC Response mappen
- Fehler in gRPC Status übersetzen
```

### Streaming Upload

Für `UploadAnalysisData(stream AnalysisDataEnvelope)` soll zunächst ein einfacher serverseitiger Collector implementiert werden.

Ablauf:

```text
1. onNext(envelope)
2. envelope validieren
3. envelope in Command mappen
4. Application Use Case upload(command) aufrufen
5. receivedItems erhöhen
6. onCompleted() -> UploadAnalysisDataResponse senden
```

### Hinweis

Noch keine Optimierung für sehr große Datenmengen implementieren. Erst lauffähig machen, danach bei Bedarf Backpressure, Chunking, Batching oder persistente Streams ergänzen.

---

## Slice 8: Bootstrap/Wiring vorbereiten

### Ziel

Das neue gRPC-Modul kann vom ausführbaren Server-/Bootstrap-Modul gestartet werden.

### Aufgaben

```text
1. Prüfe vorhandenes Bootstrap-Modul.
2. Binde forensic-analytics-ingestion-grpc als Dependency ein.
3. Stelle sicher, dass der gRPC Server beim Start registriert wird.
4. Port konfigurierbar machen.
5. Default-Port vorschlagen: 9090.
```

### Konfiguration

Beispielhafte Properties, falls das Projekt Properties/YAML verwendet:

```properties
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.port=9090
```

Falls keine Konfigurationsstruktur existiert, keine große Config-Architektur erfinden. Minimal halten und dokumentieren.

---

## Slice 9: Tests für das Modul

### Ziel

Das Modul ist unabhängig testbar.

### Testebenen

```text
1. Mapper Tests
2. Validator Tests
3. Service Tests mit Fake Use Case
4. gRPC Integrationstest mit InProcessServer
```

### Mindesttests

```text
- StartAnalysisSession akzeptiert gültigen Request
- StartAnalysisSession lehnt fehlende buildId ab
- UploadAnalysisData akzeptiert gültigen Stream
- UploadAnalysisData zählt empfangene Items korrekt
- UploadAnalysisData lehnt leeren payloadType ab
- CompleteAnalysisSession akzeptiert gültige sessionId
- AbortAnalysisSession akzeptiert gültige sessionId und reason
```

### Fake Use Case

Tests dürfen einen Fake oder Stub des Application Use Case verwenden.

Wichtig:

```text
- keine echte Datenbank
- kein Joern
- kein Plugin
- kein Netzwerk-Port im Unit-Test
```

---

## Slice 10: Dokumentation ergänzen

### Ziel

Das neue Modul ist für Entwickler nachvollziehbar dokumentiert.

### Dateien prüfen/ergänzen

```text
README.md
ARCHITECTURE.md
MODULES.md
```

Nur vorhandene Dokumentationsstruktur nutzen. Keine unnötigen neuen Dokumente erzeugen, wenn bereits passende Dateien existieren.

### Inhalt

```text
- Zweck des Moduls
- Architekturrolle als Inbound Adapter
- gRPC Port / Konfiguration
- Service-Methoden
- Hinweis, dass finale Plugin-Datenstruktur separat folgt
- Hinweis, dass Joern/Replay/LLM nicht Teil dieses Moduls sind
```

---

## Slice 11: Qualitätsprüfung

### Ziel

Der gesamte Build bleibt stabil.

### Standardprüfung

Falls Gradle verwendet wird:

```bash
./gradlew clean check
```

Zusätzlich, falls vorhanden:

```bash
./gradlew validatePlugins
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

Falls das Projekt eigene Quality Tasks besitzt, diese verwenden.

### Erwartetes Ergebnis

```text
- Build erfolgreich
- Tests erfolgreich
- keine Architekturverletzungen
- keine direkten DB-Zugriffe im gRPC-Modul
- keine Protobuf-Abhängigkeit im Domain-Modul
- keine Protobuf-Abhängigkeit im Application-Modul, außer bewusst dokumentiert und begründet
```

---

## 7. Akzeptanzkriterien

Der Workflow ist abgeschlossen, wenn folgende Punkte erfüllt sind:

```text
[ ] Neues Modul forensic-analytics-ingestion-grpc existiert.
[ ] Modul ist im Monorepo registriert.
[ ] Protobuf/gRPC Generierung funktioniert.
[ ] ForensicIngestionService ist definiert.
[ ] StartAnalysisSession ist implementiert.
[ ] UploadAnalysisData Streaming ist implementiert.
[ ] CompleteAnalysisSession ist implementiert.
[ ] AbortAnalysisSession ist implementiert.
[ ] gRPC-Service ruft ausschließlich Application Use Cases auf.
[ ] Keine direkte Persistenz im gRPC-Modul.
[ ] Keine Joern-, Replay- oder LLM-Logik im gRPC-Modul.
[ ] Mapper sind vorhanden und getestet.
[ ] Validatoren sind vorhanden und getestet.
[ ] Integrationstest mit gRPC InProcessServer existiert.
[ ] Build läuft erfolgreich.
[ ] Dokumentation beschreibt Zweck und Grenzen des Moduls.
```

---

## 8. Definition of Done

```text
- Das Modul ist technisch lauffähig.
- Ein Test-Client kann eine Analyse-Session starten.
- Ein Test-Client kann mehrere Payload-Envelopes streamen.
- Der Server zählt empfangene Payloads korrekt.
- Eine Session kann abgeschlossen werden.
- Eine Session kann abgebrochen werden.
- Fehlerhafte Requests werden kontrolliert abgelehnt.
- Das Modul verletzt die hexagonale Architektur nicht.
```

---

## 9. Späterer Folgeworkflow

Nach diesem Workflow folgt ein separater Workflow für die finale Datenstruktur.

Voraussichtliche nächste Themen:

```text
- Plugin Payload Schema definieren
- ScanEvent-Datenstruktur übertragen
- SourceFile/Class/Method/Dependency Payloads modellieren
- BTM Rule Payload modellieren
- unresolved type references übertragen
- Scan-Warnings übertragen
- lokale Plugin-H2-Datenbank exportieren
- Plugin-seitigen gRPC Client implementieren
- Server-seitige Persistenz anbinden
```

---

## 10. Codex-Ausführungsanweisung

Arbeite diesen Workflow sliceweise ab.

Vorgehen pro Slice:

```text
1. Bestehenden Code analysieren.
2. Nur den aktuellen Slice ändern.
3. Tests für den Slice ergänzen.
4. Lokale Verifikation ausführen.
5. Diff prüfen.
6. Ergebnis kurz dokumentieren.
7. Erst danach den nächsten Slice beginnen.
```

Wichtig:

```text
- Nicht raten, wenn Modulnamen oder Architekturgrenzen unklar sind.
- Keine finale Plugin-Datenstruktur erfinden.
- Keine Joern-/Replay-/LLM-Logik ergänzen.
- Keine Datenbanklogik in das gRPC-Modul schreiben.
- Keine Protobuf-Klassen in Domain-Modelle leaken lassen.
- Bestehende Projektkonventionen haben Vorrang vor Beispielen in diesem Workflow.
```

---

## 11. Erwarteter Commit-Scope

Der Commit zu diesem Workflow sollte ungefähr diesen Scope haben:

```text
Add gRPC ingestion module for forensic analytics plugin uploads
```

Mögliche Commit Message:

```text
feat: add grpc ingestion module for plugin analysis uploads

Add a dedicated forensic analytics ingestion module that exposes a
session-based gRPC service for receiving plugin scan data. The module acts as
an inbound adapter and maps protobuf transport objects to application commands.

The implementation intentionally keeps the plugin payload generic because the
final scan data schema will be defined in a follow-up step.
```
