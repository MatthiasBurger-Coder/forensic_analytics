#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

MODE="${1:-repository-to-btm}"

usage() {
  cat <<'USAGE'
Usage:
  bash deployment/docker-compose/setup.sh [repository-to-btm|docker|full|gui-smoke]

Modes:
  repository-to-btm  Build jars and images, start repository-to-btm.local.yml, run documented health checks.
  docker             Re-deploy the full local Docker stack without removing named volumes.
  full               Reset local persistence, build documented full stack inputs and images, start forensic-analytics-local.
  gui-smoke          Build documented GUI smoke inputs, start query-report-api-service and forensic-ui.

The script wraps the documented local Docker Compose runbook commands. Docker remains optional
for the repository quality gate. Before startup it checks for running Forensic Analytics
containers and refuses to stop them unless ALLOW_FORENSIC_ANALYTICS_RESTART=1 is set,
except for docker and full modes. The docker mode is an explicit re-deploy without
data loss: it stops known local Compose projects without removing named volumes. The
full mode is an explicit local reset: it stops known local Compose projects and removes
named volumes before starting forensic-analytics-local. When the restart override is set
for other modes, it stops known local Compose projects without removing named volumes,
so repeated deploys can rebind their documented host ports.
USAGE
}

run() {
  printf '\n> %s\n' "$*"
  "$@"
}

compose_repository_to_btm() {
  docker compose --env-file "$(postgres_env_file)" \
    -f "${REPO_ROOT}/deployment/docker-compose/repository-to-btm.local.yml" \
    "$@"
}

compose_postgres() {
  docker compose --env-file "$(postgres_env_file)" \
    -f "${REPO_ROOT}/docker/postgres/docker-compose.yml" \
    "$@"
}

compose_full() {
  docker compose --env-file "$(postgres_env_file)" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/repository-source-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/ingestion-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/java-parser-analysis-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/joern-analysis-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/analysis-orchestrator-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/query-report-api-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/forensic-ingestion-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/forensic-gateway-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/analysis-store-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/repository-analysis-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/java-ast-analysis-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/btm-generation-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/forensic-ui.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/forensic-analytics.local.yml" \
    "$@"
}

compose_gui_smoke() {
  docker compose -p forensic-analytics-smoke \
    -f "${REPO_ROOT}/deployment/docker-compose/services/query-report-api-service.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/services/forensic-ui.compose.yml" \
    -f "${REPO_ROOT}/deployment/docker-compose/forensic-analytics.local.yml" \
    "$@"
}

wait_for_url() {
  local url="$1"
  local attempts="${2:-60}"
  local delay_seconds="${3:-2}"

  printf '\nWaiting for %s\n' "${url}"
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl -fsS --max-time 5 "${url}" >/dev/null; then
      printf 'OK: %s\n' "${url}"
      return 0
    fi
    sleep "${delay_seconds}"
  done

  printf 'ERROR: health check did not pass: %s\n' "${url}" >&2
  return 1
}

wait_for_postgres() {
  local attempts="${1:-60}"
  local delay_seconds="${2:-2}"

  printf '\nWaiting for forensic-postgres\n'
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if [[ "$(docker inspect --format '{{.State.Health.Status}}' forensic-postgres 2>/dev/null || true)" == "healthy" ]]; then
      printf 'OK: forensic-postgres\n'
      return 0
    fi
    sleep "${delay_seconds}"
  done

  printf 'ERROR: forensic-postgres did not become healthy.\n' >&2
  return 1
}

ensure_repository_source_db_role() {
  printf '\nEnsuring repository-source PostgreSQL role and schema\n'
  run docker exec forensic-postgres bash /docker-entrypoint-initdb.d/01-repository-source-role.sh
}

postgres_env_file() {
  if [[ -f "${REPO_ROOT}/docker/postgres/.env" ]]; then
    printf '%s\n' "${REPO_ROOT}/docker/postgres/.env"
    return 0
  fi

  printf '%s\n' "${REPO_ROOT}/docker/postgres/.env.example"
}

require_postgres_runtime_env() {
  local env_file="${REPO_ROOT}/docker/postgres/.env"

  if [[ ! -f "${env_file}" ]]; then
    printf 'ERROR: docker/postgres/.env is required before starting PostgreSQL runtime containers.\n' >&2
    printf 'Copy docker/postgres/.env.example to docker/postgres/.env and replace placeholder passwords first.\n' >&2
    return 1
  fi

  if ! grep -Eq '^POSTGRES_PASSWORD=.+$' "${env_file}" \
      || grep -Eq '^POSTGRES_PASSWORD=change-me$' "${env_file}" \
      || ! grep -Eq '^REPOSITORY_SOURCE_DB_PASSWORD=.+$' "${env_file}" \
      || grep -Eq '^REPOSITORY_SOURCE_DB_PASSWORD=change-me-repository-source$' "${env_file}"; then
    printf 'ERROR: docker/postgres/.env must define non-placeholder POSTGRES_PASSWORD and REPOSITORY_SOURCE_DB_PASSWORD values.\n' >&2
    return 1
  fi
}

ensure_network() {
  ensure_docker_available

  if ! docker network inspect forensic_analytics >/dev/null 2>&1; then
    run docker network create forensic_analytics
  fi

  if ! docker network inspect forensic_repository_source_db >/dev/null 2>&1; then
    run docker network create forensic_repository_source_db
  fi
}

ensure_docker_available() {
  if ! docker info >/dev/null 2>&1; then
    printf 'ERROR: Docker is not available. Cannot verify protected Forensic Analytics instances.\n' >&2
    return 1
  fi
}

docker_ps_for_filter() {
  local filter="$1"
  local format='{{.Names}}\tproject={{.Label "com.docker.compose.project"}}\tstatus={{.Status}}'

  docker ps --filter "${filter}" --format "${format}" 2>/dev/null || true
}

running_forensic_analytics_instances() {
  {
    docker_ps_for_filter 'network=forensic_analytics'
    docker_ps_for_filter 'name=forensic-postgres'
    docker_ps_for_filter 'label=com.docker.compose.project=forensic_analytics'
    docker_ps_for_filter 'label=com.docker.compose.project=forensic-analytics-local'
    docker_ps_for_filter 'label=com.docker.compose.project=forensic-analytics-repository-to-btm'
    docker_ps_for_filter 'label=com.docker.compose.project=forensic-analytics-smoke'
  } | sed '/^$/d' | sort -u
}

guard_protected_forensic_analytics_instance() {
  ensure_docker_available

  if [[ "${ALLOW_FORENSIC_ANALYTICS_RESTART:-}" == "1" ]]; then
    printf 'WARNING: ALLOW_FORENSIC_ANALYTICS_RESTART=1 set; setup.sh may stop known local stacks but will not remove named volumes.\n' >&2
    return 0
  fi

  local running_instances
  running_instances="$(running_forensic_analytics_instances)"

  if [[ -n "${running_instances}" ]]; then
    printf 'ERROR: A protected Forensic Analytics Docker instance is already running.\n' >&2
    printf 'setup.sh will not stop or recreate containers automatically because local persistence data is protected development state.\n' >&2
    printf '\nRunning containers:\n%s\n' "${running_instances}" >&2
    printf '\nStop the stack manually after preserving the persistence state, or rerun with ALLOW_FORENSIC_ANALYTICS_RESTART=1 to let setup.sh stop known local Compose projects without removing named volumes.\n' >&2
    return 1
  fi
}

stop_known_local_stacks() {
  guard_protected_forensic_analytics_instance

  stop_known_local_stacks_preserving_state
}

stop_known_local_stacks_preserving_state() {
  ensure_docker_available

  run compose_repository_to_btm down --remove-orphans
  run compose_postgres down --remove-orphans
  run compose_full -p forensic-analytics-local down --remove-orphans
  run compose_gui_smoke down --remove-orphans
}

reset_known_local_stacks() {
  ensure_docker_available

  printf 'WARNING: full mode resets known local Forensic Analytics Compose projects and removes named volumes.\n' >&2
  run compose_repository_to_btm down -v --remove-orphans
  run compose_postgres down -v --remove-orphans
  run compose_full -p forensic-analytics-local down -v --remove-orphans
  run compose_gui_smoke down -v --remove-orphans
}

build_repository_to_btm_jars() {
  run ./gradlew --no-daemon --max-workers=1 \
    :forensic-gateway-service:bootJar \
    :analysis-store-service:bootJar \
    :repository-analysis-service:bootJar \
    :repository-source-service:bootJar \
    :java-ast-analysis-service:bootJar \
    :joern-cpg-analysis-service:bootJar \
    :btm-generation-service:bootJar \
    --dependency-verification strict --console=plain --stacktrace
}

build_full_jars() {
  run ./gradlew --no-daemon --max-workers=1 \
    :repository-source-service:bootJar \
    :ingestion-service:bootJar \
    :java-parser-analysis-service:bootJar \
    :joern-analysis-service:bootJar \
    :analysis-orchestrator-service:bootJar \
    :query-report-api-service:bootJar \
    :forensic-ingestion-service:bootJar \
    :forensic-gateway-service:bootJar \
    :analysis-store-service:bootJar \
    :repository-analysis-service:bootJar \
    :java-ast-analysis-service:bootJar \
    :joern-cpg-analysis-service:bootJar \
    :btm-generation-service:bootJar \
    --dependency-verification strict --console=plain --stacktrace
}

build_ui() {
  run npm --prefix forensic-ui ci
  run npm --prefix forensic-ui run test
  run npm --prefix forensic-ui run build
}

repository_to_btm() {
  ensure_network
  stop_known_local_stacks
  require_postgres_runtime_env
  build_repository_to_btm_jars
  run compose_postgres config --quiet
  run compose_postgres up -d
  wait_for_postgres
  ensure_repository_source_db_role
  run compose_repository_to_btm config --quiet
  run compose_repository_to_btm build
  run compose_repository_to_btm up -d
  run compose_repository_to_btm ps

  wait_for_url http://127.0.0.1:18080/api/health
  wait_for_url http://127.0.0.1:18082/health
  wait_for_url http://127.0.0.1:18083/health
  wait_for_url http://127.0.0.1:18087/health
  wait_for_url http://127.0.0.1:18084/health
  wait_for_url http://127.0.0.1:18085/health
  wait_for_url http://127.0.0.1:18086/health
}

full() {
  ensure_network
  reset_known_local_stacks
  require_postgres_runtime_env
  build_full_jars
  build_ui
  run compose_postgres config --quiet
  run compose_postgres up -d
  wait_for_postgres
  ensure_repository_source_db_role
  run compose_full config --quiet
  run compose_full build
  run compose_full -p forensic-analytics-local up -d
  run compose_full -p forensic-analytics-local ps

  wait_for_url http://127.0.0.1:18080/api/health
  wait_for_url http://127.0.0.1:18000/api/health
}

docker_redeploy() {
  ensure_network
  stop_known_local_stacks_preserving_state
  require_postgres_runtime_env
  build_full_jars
  build_ui
  run compose_postgres config --quiet
  run compose_postgres up -d
  wait_for_postgres
  ensure_repository_source_db_role
  run compose_full config --quiet
  run compose_full build
  run compose_full -p forensic-analytics-local up -d
  run compose_full -p forensic-analytics-local ps

  wait_for_url http://127.0.0.1:18080/api/health
  wait_for_url http://127.0.0.1:18000/api/health
}

gui_smoke() {
  ensure_network
  stop_known_local_stacks
  run ./gradlew --no-daemon --max-workers=1 \
    :query-report-api-service:bootJar \
    --dependency-verification strict --console=plain --stacktrace
  build_ui
  run compose_gui_smoke config
  run compose_gui_smoke build
  run compose_gui_smoke up -d query-report-api-service forensic-ui
  run compose_gui_smoke ps

  wait_for_url http://127.0.0.1:18000/api/health
}

cd "${REPO_ROOT}"

case "${MODE}" in
  repository-to-btm)
    repository_to_btm
    ;;
  docker)
    docker_redeploy
    ;;
  full)
    full
    ;;
  gui-smoke)
    gui_smoke
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
