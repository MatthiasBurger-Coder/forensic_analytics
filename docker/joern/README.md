# Joern Docker Container

This container provides a local Joern runtime for `forensic_analytics`.

## Purpose

- Create Code Property Graphs from source code.
- Run Joern queries against generated CPGs.
- Store generated artifacts in local runtime folders.
- Keep Joern isolated as infrastructure tooling instead of coupling it to domain code.

## Image

The wrapper image is built from a digest-pinned Joern image:

```text
ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295
```

This digest was resolved from `ghcr.io/joernio/joern:nightly` on 2026-05-14. Update `docker/joern/.env.example` and `docker/joern/Dockerfile` together when the repository chooses a newer pinned digest.

## Prepare Environment

```bash
cp docker/joern/.env.example docker/joern/.env
```

On Windows PowerShell with Docker available through WSL, run Docker commands through WSL from the repository root:

```powershell
wsl.exe --cd /mnt/d/Projects/forensic_analytics -- bash -lc 'docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config'
```

## Build

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml build
```

## Smoke Test

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/joern-smoke-test.sh
```

## Create CPG

Copy the sample project into the local input folder first:

```bash
rm -rf data/joern/input/sample-java-project
cp -R examples/joern/sample-java-project data/joern/input/sample-java-project
```

Then create the CPG:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/create-cpg.sh sample-java-project /analysis/input/sample-java-project /analysis/output
```

## Run Query

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/run-query.sh /analysis/output/sample-java-project.cpg.bin.zip /analysis/queries/list-methods.sc /analysis/output/list-methods.txt
```

## Runtime Folders

| Folder | Purpose | Git |
| --- | --- | --- |
| `data/joern/input` | Local source input | ignored except `.gitkeep` |
| `data/joern/workspace` | Joern workspace | ignored except `.gitkeep` |
| `data/joern/output` | Generated CPG and query output | ignored except `.gitkeep` |
| `data/joern/logs` | Runtime logs | ignored except `.gitkeep` |

## Scripts

| Script | Purpose |
| --- | --- |
| `joern-entrypoint.sh` | Creates runtime folders and delegates to the requested command. |
| `joern-smoke-test.sh` | Verifies that the Joern CLI is available. |
| `create-cpg.sh` | Creates a CPG file from a source directory. |
| `run-query.sh` | Runs a Joern script against a CPG file and stores the output. |
| `clean-workspace.sh` | Removes local workspace, output and log contents inside the mounted runtime folders. |

## Troubleshooting

### Out of Memory

Increase `JOERN_HEAP` and `JOERN_CONTAINER_MEMORY` in `docker/joern/.env`.

### Permission Problems

Remove local runtime files and recreate folders from the repository root:

```bash
rm -rf data/joern/input/* data/joern/workspace/* data/joern/output/* data/joern/logs/*
```

### CLI Command Changed

Run the container interactively and inspect Joern CLI help before changing scripts:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern joern --help
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern joern-parse --help
```

Do not substitute new commands by name similarity. Update the scripts only from verified CLI behavior.
