# Joern Docker Container Workflow

## Goal

Provide a repeatable local Joern runtime for `forensic_analytics` so developers can create Code Property Graphs, run Joern queries and store generated artifacts in local runtime folders.

The container is infrastructure tooling. It does not contain domain decisions, replay decisions, report semantics or platform plugin behavior.

## Repository Layout

```text
docker/joern/
  Dockerfile
  docker-compose.joern.yml
  .env.example
  README.md
  scripts/
    joern-entrypoint.sh
    joern-smoke-test.sh
    create-cpg.sh
    run-query.sh
    clean-workspace.sh
examples/joern/
  sample-java-project/
  queries/
data/joern/
  input/
  workspace/
  output/
  logs/
```

`data/joern/**` is local runtime storage and is ignored except for `.gitkeep` files.

## Verified Runtime Decisions

- Joern remains outside the domain and application layers.
- The wrapper image uses a digest-pinned base image.
- The source input mount is read-only.
- The container does not mount the Docker socket.
- Generated CPG files, query output and logs stay under `data/joern`.
- The default Gradle quality gate does not require Docker or Joern.

The pinned base image is:

```text
ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295
```

The digest was resolved from `ghcr.io/joernio/joern:nightly` on 2026-05-14. Future updates must use a verified digest and must update the Dockerfile, `.env.example` and this document together.

## Local Commands

Prepare the local environment:

```bash
cp docker/joern/.env.example docker/joern/.env
```

Validate Compose:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config
```

Build the image:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml build
```

Run the smoke test:

```bash
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/joern-smoke-test.sh
```

Run the sample CPG flow:

```bash
rm -rf data/joern/input/sample-java-project
cp -R examples/joern/sample-java-project data/joern/input/sample-java-project
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/create-cpg.sh sample-java-project /analysis/input/sample-java-project /analysis/output
docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml run --rm joern /opt/forensic-analytics/joern/scripts/run-query.sh /analysis/output/sample-java-project.cpg.bin.zip /analysis/queries/list-methods.sc /analysis/output/list-methods.txt
```

On Windows with Docker available through WSL, run the same commands through WSL from the repository root:

```powershell
wsl.exe --cd /mnt/d/Projects/forensic_analytics -- bash -lc 'docker compose --env-file docker/joern/.env -f docker/joern/docker-compose.joern.yml config'
```

## Expected Output

- `data/joern/output/sample-java-project.cpg.bin.zip`
- `data/joern/output/list-methods.txt`

The method listing should include methods from `example.App` or Joern frontend-generated methods. The exact internal method set is Joern-version dependent and must not be treated as forensic evidence by itself.

## Stop Rules

Stop and report before changing scripts when:

- the Joern CLI flags differ from the documented help output,
- the pinned image cannot be pulled,
- Compose build or config fails for a reason that requires an architecture or runtime decision,
- generated Joern artifacts would need to be committed,
- domain or application code would need to reference Docker, shell scripts or Joern runtime classes.

## Verification

The repository quality gate remains the full gate from `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Docker verification is additive for this workflow and does not replace the Gradle quality gate.
