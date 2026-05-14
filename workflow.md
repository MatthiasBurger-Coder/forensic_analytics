# workflow.md — Einrichtung des Joern-Dockercontainers für `forensic_analytics`

**Status:** Draft
**Zielrepository:** `forensic_analytics`
**Ziel:** Wiederholbare, containerisierte Joern-Umgebung bereitstellen, damit `forensic_analytics` Code Property Graphs erzeugen, Joern-Abfragen ausführen und Ergebnisse später in Analysis Store / Replay / Reports weiterverarbeiten kann.
**Arbeitsmodell:** Slice-basiert, subagent-fähig, mit klaren Verantwortlichkeiten, Qualitätsgates und Stop-Regeln.

---

## 1. Zielbild

Der Joern-Container wird als **Infrastruktur-/Analyse-Adapter** für `forensic_analytics` aufgebaut.

Er soll:

* Joern isoliert und reproduzierbar bereitstellen,
* Quellcode-Verzeichnisse read-only einhängen können,
* Joern-Workspace und Analyseergebnisse persistent ablegen,
* Smoke-Tests und Beispielanalysen unterstützen,
* später von `forensic_analytics` automatisiert angesteuert werden können,
* keine fachliche Domänenlogik enthalten,
* keine dauerhafte Analyseentscheidung im Container treffen,
* keine Plugin-Verantwortung übernehmen.

Der Container ist damit nicht „die Plattform“, sondern ein austauschbarer technischer Adapter für semantische Codeanalyse.

---

## 2. Leitplanken

### 2.1 Architektur

* Hexagonale Architektur bleibt verbindlich.
* Joern liegt außerhalb der Domain.
* Docker-, Shell-, CLI- und Joern-spezifische Details gehören in Adapter-/Infrastructure-Bereiche.
* Domain-Modelle dürfen keine Joern-Klassen, keine Docker-Klassen und keine Shell-Kommandos kennen.
* Der spätere Zugriff auf Joern erfolgt über Ports, z. B. `CodePropertyGraphAnalysisPort`.
* Docker Compose ist nur lokale Entwicklungs-/Testinfrastruktur, nicht Domain-Verhalten.

### 2.2 Runtime-Grenzen

* `forensic_analytics` kann weiterhin auf Java 25 und Gradle 9.4.0 ausgerichtet werden.
* Der Joern-Container verwendet den von Joern unterstützten Runtime-Stack.
* Joern-Runtime nicht blind auf Java 25 umbauen.
* Falls ein eigenes Image gebaut wird, muss die Joern-Version explizit gepinnt werden.
* Nightly Images dürfen nur für explorative Entwicklung verwendet werden; für reproduzierbare CI/CD-Strecken ist eine gepinnte Version oder ein Digest vorzuziehen.

### 2.3 Sicherheits- und Stabilitätsregeln

* Quellcode-Mount standardmäßig read-only.
* Container läuft ohne Docker-Socket.
* Keine Host-Pfade hart codieren.
* Keine Secrets in Dockerfile, Compose-Dateien oder Skripten.
* Analyse-Output wird in dedizierten Verzeichnissen abgelegt.
* Große Repositories müssen mit Speicherlimits und JVM-Parametern getestet werden.
* Bei Speicherproblemen nicht raten, sondern reproduzierbar dokumentieren.

---

## 3. Zielstruktur im Repository

Die konkrete Struktur darf an die bestehende Repository-Struktur angepasst werden. Falls noch keine passende Struktur existiert, soll folgende Zielstruktur angelegt werden:

```text
forensic_analytics/
├── docker/
│   └── joern/
│       ├── Dockerfile
│       ├── docker-compose.joern.yml
│       ├── .env.example
│       ├── README.md
│       └── scripts/
│           ├── joern-entrypoint.sh
│           ├── joern-smoke-test.sh
│           ├── create-cpg.sh
│           ├── run-query.sh
│           └── clean-workspace.sh
├── docs/
│   └── workflows/
│       └── joern-docker-container.workflow.md
├── examples/
│   └── joern/
│       ├── sample-java-project/
│       │   └── src/main/java/example/App.java
│       └── queries/
│           ├── list-methods.sc
│           ├── list-calls.sc
│           └── find-exceptions.sc
└── data/
    └── joern/
        ├── input/
        ├── workspace/
        ├── output/
        └── logs/
```

Hinweis: `data/joern/**` ist lokale Laufzeitablage und gehört standardmäßig nicht in Git, außer `.gitkeep` oder dokumentierte Beispieldaten werden bewusst versioniert.

---

## 4. Subagent-Modell

Die Arbeit wird so geschnitten, dass mehrere Subagents parallel oder nacheinander arbeiten können.

### 4.1 Orchestrator Agent

**Rolle:** Gesamtsteuerung, Konsistenz, Merge-Reihenfolge, Stop-Entscheidungen.

**Aufgaben:**

* Zielstruktur prüfen.
* Bestehende Repository-Konventionen lesen.
* Slice-Reihenfolge festlegen.
* Änderungen der Subagents zusammenführen.
* Konflikte zwischen Architektur, Docker und Tests entscheiden.
* Vor jedem Slice prüfen, ob die bisherige Arbeit lauffähig bleibt.

**Darf nicht:**

* fachliche Architekturentscheidungen stillschweigend ändern,
* bestehende Qualitätsgates abschwächen,
* fehlschlagende Tests ignorieren.

**Stop-Regeln:**

* Wenn unklar ist, wo Docker-Infrastruktur im Repository liegen soll.
* Wenn bestehende Build-/CI-Konventionen widersprechen.
* Wenn Joern-Version, Image oder Runtime nicht reproduzierbar bestimmbar ist.

---

### 4.2 Docker Infrastructure Agent

**Rolle:** Dockerfile, Compose-Datei, Volumes, Umgebungsvariablen, Container-Härtung.

**Aufgaben:**

* `docker/joern/Dockerfile` erstellen oder anpassen.
* `docker-compose.joern.yml` erstellen.
* `.env.example` definieren.
* Mounts für Input, Workspace, Output und Logs sauber abbilden.
* Container ohne privilegierte Rechte ausführen.
* Health-/Smoke-Mechanismus vorbereiten.

**Akzeptanzkriterien:**

* `docker compose -f docker/joern/docker-compose.joern.yml config` läuft ohne Fehler.
* Container kann gestartet werden.
* Joern CLI ist im Container ausführbar.
* Mounts sind dokumentiert.
* Quellcode-Mount ist read-only.

---

### 4.3 Joern Runtime Agent

**Rolle:** Joern-spezifische Installation, Versionierung, CLI-Kommandos, JVM-Parameter.

**Aufgaben:**

* Entscheiden, ob offizielles Joern-Image als Basis genutzt wird oder ein eigenes Image gebaut wird.
* Joern-Version dokumentieren.
* JVM-Heap-Konfiguration über Environment steuerbar machen.
* `create-cpg.sh` erstellen.
* `run-query.sh` erstellen.
* Standardpfade im Container festlegen.

**Akzeptanzkriterien:**

* `joern --help` oder vergleichbarer CLI-Aufruf funktioniert.
* Beispielprojekt kann importiert werden.
* CPG-Erzeugung kann automatisiert ausgeführt werden.
* Query-Skript kann gegen erzeugten CPG/Workspace laufen.
* Speicherparameter sind über `.env` konfigurierbar.

---

### 4.4 Example & Test Data Agent

**Rolle:** Minimalbeispiel, reproduzierbare Smoke-Analyse, Testdaten.

**Aufgaben:**

* Kleines Java-Beispielprojekt unter `examples/joern/sample-java-project` anlegen.
* Beispiel bewusst klein halten.
* Mindestens eine Methode, einen Methodenaufruf und eine Exception-Situation einbauen.
* Beispielqueries unter `examples/joern/queries` anlegen.
* Erwartete Output-Struktur dokumentieren.

**Akzeptanzkriterien:**

* Beispielprojekt ist ohne Build notwendig analysierbar.
* Query `list-methods.sc` liefert sichtbare Methoden.
* Query `find-exceptions.sc` findet mindestens eine Exception- oder Throw-Struktur, sofern Joern diese im Beispiel abbildet.

---

### 4.5 Integration Adapter Agent

**Rolle:** Vorbereitung der späteren Anbindung an `forensic_analytics`.

**Aufgaben:**

* Noch keine vollständige Plattformintegration bauen, wenn diese nicht Teil des aktuellen Slices ist.
* Einen klaren Adapter-Zielpunkt dokumentieren.
* Spätere Port-Schnittstelle skizzieren, ohne Domain mit Joern zu koppeln.
* Output-Formate vorbereiten: JSON, CSV oder textbasierte Query-Ergebnisse.
* Prüfen, ob ein späterer gRPC-Ingestion-Schritt dadurch sauber ergänzt werden kann.

**Akzeptanzkriterien:**

* Es gibt keine direkte Joern-Abhängigkeit in Domain-Modulen.
* Der Container kann unabhängig gestartet und getestet werden.
* Der spätere Analysefluss ist dokumentiert:

    * Repository Checkout
    * Joern CPG-Erzeugung
    * Query-Ausführung
    * Ergebnisablage
    * spätere Übernahme in Analysis Store

---

### 4.6 QA & Documentation Agent

**Rolle:** Qualitätsprüfung, README, Ausführbarkeit, Entwicklerführung.

**Aufgaben:**

* `docker/joern/README.md` schreiben.
* Alle Befehle lokal ausführbar dokumentieren.
* Troubleshooting für Speicher, Rechte und Volumes ergänzen.
* `.gitignore` prüfen.
* Quality-Gates ausführen.
* Ergebnis mit Diff und Befehlsprotokoll dokumentieren.

**Akzeptanzkriterien:**

* Neue Entwickler können den Container anhand der README starten.
* Smoke-Test ist dokumentiert.
* Fehlerfälle sind nachvollziehbar beschrieben.
* Keine temporären Artefakte werden versehentlich versioniert.

---

## 5. Arbeitsweise für Codex/Subagents

Jeder Subagent arbeitet nach diesem Muster:

```text
1. Repository-Struktur prüfen.
2. Relevante bestehende Dateien lesen.
3. Nur den eigenen Slice ändern.
4. Lokale Tests/Checks für den Slice ausführen.
5. Ergebnis dokumentieren.
6. Bei Unsicherheit stoppen und konkret berichten.
```

Subagents dürfen nicht:

* große Umbauten außerhalb ihres Slices durchführen,
* bestehende Architekturentscheidungen überschreiben,
* Qualitätsgates entfernen,
* `.env` mit echten Werten erzeugen,
* Secrets speichern,
* generierte Joern-Artefakte committen,
* Domain-Code an Docker oder Joern koppeln.

---

## 6. Slices

## Slice 0 — Repository-Vorprüfung und Branch anlegen

**Verantwortlich:** Orchestrator Agent

### Ziel

Arbeitsbasis schaffen, ohne bestehende Arbeit zu beschädigen.

### Aufgaben

1. Aktuellen Branch prüfen.
2. Git-Status prüfen.
3. Falls uncommitted Changes vorhanden sind: nicht überschreiben.
4. Neuen Branch anlegen:

```bash
git checkout -b feature/joern-docker-container
```

5. Bestehende Projektstruktur prüfen:

```bash
find . -maxdepth 3 -type f | sort | sed 's#^./##'
```

6. Prüfen, ob bereits Docker-, Compose-, Devcontainer- oder Joern-Dateien existieren:

```bash
find . \( -iname '*joern*' -o -iname 'Dockerfile' -o -iname '*compose*' -o -iname '.env*' \) -print
```

### Akzeptanzkriterien

* Branch existiert.
* Bestehende relevante Dateien sind bekannt.
* Es wurde nichts überschrieben.

### Stop-Regel

Wenn uncommitted Changes existieren, die nicht zum aktuellen Joern-Slice gehören, stoppen und melden.

---

## Slice 1 — Zielstruktur und Git-Ignore vorbereiten

**Verantwortlich:** Docker Infrastructure Agent

### Ziel

Verzeichnisstruktur für Joern-Infrastruktur und Laufzeitdaten vorbereiten.

### Aufgaben

1. Zielverzeichnisse anlegen:

```bash
mkdir -p docker/joern/scripts
mkdir -p docs/workflows
mkdir -p examples/joern/sample-java-project/src/main/java/example
mkdir -p examples/joern/queries
mkdir -p data/joern/input data/joern/workspace data/joern/output data/joern/logs
```

2. `.gitkeep` nur dort anlegen, wo leere Ordner versioniert werden sollen:

```bash
touch data/joern/input/.gitkeep
touch data/joern/workspace/.gitkeep
touch data/joern/output/.gitkeep
touch data/joern/logs/.gitkeep
```

3. `.gitignore` ergänzen:

```gitignore
# Joern local runtime data
/data/joern/input/**
/data/joern/workspace/**
/data/joern/output/**
/data/joern/logs/**
!/data/joern/input/.gitkeep
!/data/joern/workspace/.gitkeep
!/data/joern/output/.gitkeep
!/data/joern/logs/.gitkeep

# Local environment files
.env
*.env.local
```

### Akzeptanzkriterien

* Struktur ist vorhanden.
* Laufzeitdaten werden nicht versehentlich versioniert.
* `.env.example` bleibt versionierbar.

---

## Slice 2 — Dockerfile erstellen

**Verantwortlich:** Docker Infrastructure Agent + Joern Runtime Agent

### Ziel

Reproduzierbares Docker-Image für Joern bereitstellen.

### Empfehlung

Für den ersten Schritt wird ein eigenes dünnes Wrapper-Image auf Basis des offiziellen Joern-Images bevorzugt. Dadurch bleibt die Joern-Installation upstream-nah, aber Projektpfade, Skripte und Defaults können kontrolliert werden.

### Datei

`docker/joern/Dockerfile`

### Zielinhalt

```dockerfile
FROM ghcr.io/joernio/joern:nightly

USER root

RUN mkdir -p /analysis/input \
    /analysis/workspace \
    /analysis/output \
    /analysis/logs \
    /opt/forensic-analytics/joern/scripts

COPY scripts/*.sh /opt/forensic-analytics/joern/scripts/

RUN chmod +x /opt/forensic-analytics/joern/scripts/*.sh

ENV ANALYSIS_INPUT_DIR=/analysis/input
ENV JOERN_WORKSPACE_DIR=/analysis/workspace
ENV ANALYSIS_OUTPUT_DIR=/analysis/output
ENV ANALYSIS_LOG_DIR=/analysis/logs
ENV JOERN_HEAP=8G

WORKDIR /analysis

ENTRYPOINT ["/opt/forensic-analytics/joern/scripts/joern-entrypoint.sh"]
CMD ["joern", "--help"]
```

### Nacharbeit

Wenn `ghcr.io/joernio/joern:nightly` für reproduzierbare Builds nicht akzeptabel ist, muss dieser Slice nachträglich auf eine gepinnte Version oder einen Digest umgestellt werden.

### Akzeptanzkriterien

* Dockerfile baut lokal.
* Skripte werden kopiert.
* Pfade sind einheitlich.
* Joern wird nicht in die Projekt-Domain eingebettet.

---

## Slice 3 — Entrypoint und Hilfsskripte erstellen

**Verantwortlich:** Joern Runtime Agent

### Ziel

Joern-Aufrufe vereinheitlichen und für spätere Automatisierung vorbereiten.

### Datei: `docker/joern/scripts/joern-entrypoint.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

mkdir -p "${ANALYSIS_INPUT_DIR:-/analysis/input}"
mkdir -p "${JOERN_WORKSPACE_DIR:-/analysis/workspace}"
mkdir -p "${ANALYSIS_OUTPUT_DIR:-/analysis/output}"
mkdir -p "${ANALYSIS_LOG_DIR:-/analysis/logs}"

exec "$@"
```

### Datei: `docker/joern/scripts/create-cpg.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="${1:-sample-project}"
SOURCE_DIR="${2:-${ANALYSIS_INPUT_DIR:-/analysis/input}}"
OUTPUT_DIR="${3:-${ANALYSIS_OUTPUT_DIR:-/analysis/output}}"
HEAP="${JOERN_HEAP:-8G}"

mkdir -p "${OUTPUT_DIR}"

if [ ! -d "${SOURCE_DIR}" ]; then
  echo "Source directory does not exist: ${SOURCE_DIR}" >&2
  exit 1
fi

CPG_FILE="${OUTPUT_DIR}/${PROJECT_NAME}.cpg.bin.zip"

# Java source analysis should be performed through Joern's JVM-based frontend.
# The exact frontend command may vary between Joern versions; keep this wrapper isolated.
joern-parse "${SOURCE_DIR}" --output "${CPG_FILE}" -J-Xmx"${HEAP}"

echo "CPG created: ${CPG_FILE}"
```

### Datei: `docker/joern/scripts/run-query.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

CPG_FILE="${1:?Missing CPG file path}"
QUERY_FILE="${2:?Missing query file path}"
OUTPUT_FILE="${3:-${ANALYSIS_OUTPUT_DIR:-/analysis/output}/query-result.txt}"
HEAP="${JOERN_HEAP:-8G}"

if [ ! -f "${CPG_FILE}" ]; then
  echo "CPG file does not exist: ${CPG_FILE}" >&2
  exit 1
fi

if [ ! -f "${QUERY_FILE}" ]; then
  echo "Query file does not exist: ${QUERY_FILE}" >&2
  exit 1
fi

mkdir -p "$(dirname "${OUTPUT_FILE}")"

joern --script "${QUERY_FILE}" "${CPG_FILE}" -J-Xmx"${HEAP}" | tee "${OUTPUT_FILE}"
```

### Datei: `docker/joern/scripts/joern-smoke-test.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

joern --help >/tmp/joern-help.txt || true

if [ ! -s /tmp/joern-help.txt ]; then
  echo "Joern help output is empty. Joern may not be available on PATH." >&2
  exit 1
fi

echo "Joern CLI is available."
```

### Datei: `docker/joern/scripts/clean-workspace.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

rm -rf "${JOERN_WORKSPACE_DIR:-/analysis/workspace}"/*
rm -rf "${ANALYSIS_OUTPUT_DIR:-/analysis/output}"/*
rm -rf "${ANALYSIS_LOG_DIR:-/analysis/logs}"/*

echo "Joern workspace, output and logs cleaned."
```

### Akzeptanzkriterien

* Alle Skripte sind ausführbar.
* Fehler werden mit klaren Exit-Codes beendet.
* Pfade sind über Environment steuerbar.
* Skriptkommentare sind Englisch.

### Stop-Regel

Wenn `joern-parse` oder `joern --script` in der verwendeten Joern-Version anders aufgerufen werden müssen, nicht raten. Stattdessen tatsächliche CLI-Hilfe im Container auslesen und Skripte entsprechend anpassen.

---

## Slice 4 — Docker Compose für lokale Nutzung

**Verantwortlich:** Docker Infrastructure Agent

### Ziel

Einheitlicher lokaler Start ohne lange Docker-Kommandos.

### Datei: `docker/joern/docker-compose.joern.yml`

```yaml
services:
  joern:
    build:
      context: .
      dockerfile: Dockerfile
    image: forensic-analytics/joern:local
    container_name: forensic-analytics-joern
    working_dir: /analysis
    environment:
      ANALYSIS_INPUT_DIR: /analysis/input
      JOERN_WORKSPACE_DIR: /analysis/workspace
      ANALYSIS_OUTPUT_DIR: /analysis/output
      ANALYSIS_LOG_DIR: /analysis/logs
      JOERN_HEAP: ${JOERN_HEAP:-8G}
    volumes:
      - ../../data/joern/input:/analysis/input:ro
      - ../../data/joern/workspace:/analysis/workspace
      - ../../data/joern/output:/analysis/output
      - ../../data/joern/logs:/analysis/logs
      - ../../examples/joern/queries:/analysis/queries:ro
    deploy:
      resources:
        limits:
          memory: ${JOERN_CONTAINER_MEMORY:-12g}
    stdin_open: true
    tty: true
```

### Datei: `docker/joern/.env.example`

```dotenv
JOERN_HEAP=8G
JOERN_CONTAINER_MEMORY=12g
```

### Befehle

```bash
cp docker/joern/.env.example docker/joern/.env

docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  build

docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  run --rm joern /opt/forensic-analytics/joern/scripts/joern-smoke-test.sh
```

### Akzeptanzkriterien

* Compose-Konfiguration ist valide.
* Image baut.
* Smoke-Test läuft.
* `.env` wird nicht committed.
* `.env.example` wird committed.

---

## Slice 5 — Beispielprojekt und Joern-Queries

**Verantwortlich:** Example & Test Data Agent

### Ziel

Minimaler, reproduzierbarer Joern-Testfall.

### Datei: `examples/joern/sample-java-project/src/main/java/example/App.java`

```java
package example;

public class App {

    public static void main(String[] args) {
        App app = new App();
        app.run(args);
    }

    public String run(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing input");
        }
        return normalize(args[0]);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
```

### Datei: `examples/joern/queries/list-methods.sc`

```scala
importCode.cpg.method.name.l
```

### Datei: `examples/joern/queries/list-calls.sc`

```scala
importCode.cpg.call.name.l
```

### Datei: `examples/joern/queries/find-exceptions.sc`

```scala
importCode.cpg.call.name("<operator>.throw").l
```

### Hinweis

Die genaue Query-Form kann je nach Joern-Version angepasst werden müssen. Wenn die Query-Syntax nicht funktioniert, muss der Joern Runtime Agent im Container die tatsächliche CPGQL-Ausführung prüfen und die Query-Dateien korrigieren.

### Akzeptanzkriterien

* Beispielprojekt ist versioniert.
* Beispiel enthält Methode, Methodenaufruf und Exception-Pfad.
* Queries sind klein und nachvollziehbar.

---

## Slice 6 — Smoke-Test-End-to-End

**Verantwortlich:** QA & Documentation Agent + Joern Runtime Agent

### Ziel

Ein Entwickler kann lokal vom Beispielprojekt bis zum Query-Ergebnis alles ausführen.

### Vorbereitung

Beispielprojekt in den lokalen Input kopieren:

```bash
rm -rf data/joern/input/sample-java-project
cp -R examples/joern/sample-java-project data/joern/input/sample-java-project
```

### CPG erzeugen

```bash
docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  run --rm joern \
  /opt/forensic-analytics/joern/scripts/create-cpg.sh \
  sample-java-project \
  /analysis/input/sample-java-project \
  /analysis/output
```

### Query ausführen

```bash
docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  run --rm joern \
  /opt/forensic-analytics/joern/scripts/run-query.sh \
  /analysis/output/sample-java-project.cpg.bin.zip \
  /analysis/queries/list-methods.sc \
  /analysis/output/list-methods.txt
```

### Erwartete Ergebnisse

* Datei `data/joern/output/sample-java-project.cpg.bin.zip` existiert.
* Datei `data/joern/output/list-methods.txt` existiert.
* Ergebnis enthält mindestens Methoden aus `example.App` oder Joern-interne/Frontend-bedingte Methodennamen.

### Akzeptanzkriterien

* End-to-End-Smoke-Test ist reproduzierbar.
* Fehler sind dokumentiert.
* Output wird nicht committed.

---

## Slice 7 — README für Joern-Dockercontainer

**Verantwortlich:** QA & Documentation Agent

### Ziel

Benutzbare Dokumentation für Entwickler.

### Datei: `docker/joern/README.md`

Mindestinhalt:

````markdown
# Joern Docker Container

This container provides a local Joern runtime for forensic_analytics.

## Purpose

- Create Code Property Graphs from source code.
- Run Joern queries against generated CPGs.
- Store generated artifacts in local runtime folders.

## Build

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml build
````

## Smoke test

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/joern-smoke-test.sh
```

## Create CPG

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/create-cpg.sh sample-java-project /analysis/input/sample-java-project /analysis/output
```

## Run query

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/run-query.sh /analysis/output/sample-java-project.cpg.bin.zip /analysis/queries/list-methods.sc /analysis/output/list-methods.txt
```

## Runtime folders

| Folder                 | Purpose                    | Git                       |
| ---------------------- | -------------------------- | ------------------------- |
| `data/joern/input`     | Local source input         | ignored except `.gitkeep` |
| `data/joern/workspace` | Joern workspace            | ignored except `.gitkeep` |
| `data/joern/output`    | Generated CPG/query output | ignored except `.gitkeep` |
| `data/joern/logs`      | Runtime logs               | ignored except `.gitkeep` |

## Troubleshooting

### Out of memory

Increase `JOERN_HEAP` and `JOERN_CONTAINER_MEMORY` in `docker/joern/.env`.

### Permission problems

Remove local runtime files and recreate folders from the repository root.

### CLI command changed

Run the container interactively and inspect Joern CLI help before changing scripts.

````

### Akzeptanzkriterien

- README enthält Zweck, Build, Smoke-Test, CPG-Erzeugung, Query-Ausführung und Troubleshooting.
- Alle Kommandos sind kopierbar.
- Keine falschen Versprechen über Plattformintegration.

---

## Slice 8 — Adapter-Zielbild dokumentieren

**Verantwortlich:** Integration Adapter Agent

### Ziel

Klar festhalten, wie Joern später in `forensic_analytics` eingebunden wird, ohne jetzt zu viel Plattformlogik zu bauen.

### Datei

`docs/workflows/joern-integration-target.md`

### Inhaltliche Punkte

- Joern ist ein Infrastructure Adapter.
- Der spätere Domain-Port könnte heißen:

```java
public interface CodePropertyGraphAnalysisPort {
    CodePropertyGraphAnalysisResult analyze(CodePropertyGraphAnalysisRequest request);
}
````

* Der Adapter kann später wahlweise:

    * einen lokalen Docker-Container starten,
    * einen bereits laufenden Joern-Service nutzen,
    * eine Queue-/gRPC-gesteuerte Analyse anstoßen,
    * nur CPG-Dateien importieren.

* Der aktuelle Slice baut nur die lokale Containergrundlage.

* Die spätere Plattformentscheidung bleibt offen.

### Akzeptanzkriterien

* Domain bleibt frei von Joern-Abhängigkeiten.
* Spätere Integration ist anschlussfähig.
* Keine überflüssige Implementierung wird vorweggenommen.

---

## Slice 9 — Qualitätsgate

**Verantwortlich:** QA & Documentation Agent + Orchestrator Agent

### Ziel

Sicherstellen, dass Docker-/Dokumentationsänderungen das Projekt nicht beschädigen.

### Checks

Mindestens ausführen:

```bash
git status --short

docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  config

docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  build

docker compose \
  --env-file docker/joern/.env \
  -f docker/joern/docker-compose.joern.yml \
  run --rm joern \
  /opt/forensic-analytics/joern/scripts/joern-smoke-test.sh
```

Falls Gradle im Repository bereits vorhanden ist:

```bash
./gradlew clean check
```

Falls ein vollständiges Gate dokumentiert ist, gilt das Repository-Gate, nicht dieser Workflow als Ersatz.

### Akzeptanzkriterien

* Docker Compose config erfolgreich.
* Docker Build erfolgreich.
* Smoke-Test erfolgreich.
* Bestehendes Projektgate wurde nicht abgeschwächt.
* Fehlschläge sind dokumentiert.

---

## Slice 10 — Commit und Abschlussdokumentation

**Verantwortlich:** Orchestrator Agent

### Ziel

Nachvollziehbarer Abschluss mit sauberer Commit-Historie.

### Vor dem Commit

```bash
git status --short
git diff --stat
git diff
```

Prüfen:

* Keine echten `.env`-Dateien gestaged.
* Keine generierten CPG-Dateien gestaged.
* Keine lokalen Logs gestaged.
* Keine großen Analyseoutputs gestaged.
* README und Workflow sind enthalten.

### Commit Message Vorlage

```text
Add Joern Docker runtime workflow

Why:
- Prepare forensic_analytics for reproducible Joern-based CPG generation.
- Keep Joern isolated as infrastructure adapter instead of coupling it to the domain.
- Enable future semantic analysis, replay and analysis-store ingestion workflows.

What:
- Add Docker-based Joern runtime structure.
- Add Compose configuration and environment template.
- Add helper scripts for smoke tests, CPG creation, query execution and cleanup.
- Add minimal Java sample project and example Joern queries.
- Add documentation for local usage and future integration target.

How verified:
- docker compose config
- docker compose build
- Joern smoke test
- Existing project quality gate where available
```

### Akzeptanzkriterien

* Commit enthält nur relevante Dateien.
* Commit Message erklärt Warum, Was und Wie verifiziert wurde.
* Branch ist bereit für Review oder Push.

---

## 7. Definition of Done

Der Workflow ist abgeschlossen, wenn:

* Joern-Container lokal gebaut werden kann,
* Smoke-Test erfolgreich läuft,
* Beispielprojekt analysiert werden kann,
* mindestens eine Query ausführbar ist,
* Joern-Output lokal abgelegt wird,
* Runtime-Daten nicht versioniert werden,
* README die Nutzung vollständig beschreibt,
* spätere Integration in `forensic_analytics` als Adapter vorbereitet ist,
* keine Domain-Kopplung an Joern entstanden ist,
* bestehende Qualitätsgates nicht geschwächt wurden.

---

## 8. Bekannte Grenzen

* Dieser Workflow baut noch keinen produktiven Joern-Analyse-Service.
* Dieser Workflow definiert noch kein endgültiges gRPC-Protokoll.
* Dieser Workflow importiert Joern-Ergebnisse noch nicht in den Analysis Store.
* Dieser Workflow ersetzt keine spätere Performanceanalyse großer Repositories.
* Joern-CLI-Kommandos können sich je nach Version ändern und müssen im Container verifiziert werden.
* Für große Monorepos müssen Heap, Container-Memory, Workspace-Strategie und CPG-Ablage separat gehärtet werden.

---

## 9. Folge-Slices nach diesem Workflow

Nach erfolgreicher Einrichtung des Containers können folgende Workflows entstehen:

1. `workflow-joern-analysis-adapter.md`

    * Java-Port und Adapter für Joern-Analyse.

2. `workflow-joern-result-normalization.md`

    * Normalisierung von Joern-Ergebnissen in interne Analysemodelle.

3. `workflow-analysis-store-ingestion.md`

    * Speicherung von CPG-/Query-Ergebnissen im Analysis Store.

4. `workflow-grpc-analysis-ingestion.md`

    * Übergabe großer Analyseartefakte über gRPC.

5. `workflow-large-repository-joern-performance.md`

    * WildFly-/Großrepository-Szenarien, Speichergrenzen, Batch-Strategien.

---

## 10. Finaler Codex-Startprompt

```text
You are working in the forensic_analytics repository.

Task:
Implement the Joern Docker container setup described in docs/workflows/joern-docker-container.workflow.md.

Work slice by slice.
Do not skip slices.
Do not weaken existing quality gates.
Do not couple domain code to Docker, Joern or shell commands.
Keep Joern as an infrastructure adapter.
Use English for all source code comments and script comments.
Use clear, reproducible commands.
Do not commit generated CPG files, logs, local .env files or analysis outputs.

Required subagent responsibilities:
- Orchestrator Agent: controls sequence, checks repository state, resolves conflicts.
- Docker Infrastructure Agent: Dockerfile, Compose, volumes, env template.
- Joern Runtime Agent: Joern CLI wrappers, heap settings, smoke test.
- Example & Test Data Agent: minimal Java sample and query files.
- Integration Adapter Agent: documents future adapter target without implementing unnecessary platform logic.
- QA & Documentation Agent: README, troubleshooting, quality gates.

Stop and report if:
- uncommitted unrelated changes exist,
- Joern CLI commands differ from the workflow assumptions,
- the official image cannot be pulled,
- Docker Compose config/build fails in a way that requires architectural decisions,
- repository conventions conflict with the proposed structure.

At the end, provide:
- changed files,
- executed commands,
- command results,
- unresolved risks,
- final commit message proposal.
```
