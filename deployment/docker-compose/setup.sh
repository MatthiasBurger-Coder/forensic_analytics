#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

MODE="${1:-repository-to-btm}"

usage() {
  cat <<'USAGE'
Usage:
  bash deployment/docker-compose/setup.sh [repository-to-btm|full|gui-smoke]

Modes:
  repository-to-btm  Build jars and images, start repository-to-btm.local.yml, run documented health checks.
  full               Build documented full local stack inputs and images, start forensic-analytics-local.
  gui-smoke          Build documented GUI smoke inputs, start query-report-api-service and forensic-ui.

The script wraps the documented local Docker Compose runbook commands. Docker remains optional
for the repository quality gate. Before startup it checks for running Forensic Analytics
containers and refuses to stop them unless ALLOW_FORENSIC_ANALYTICS_RESTART=1 is set.
When the restart override is set, it stops known local Compose projects without removing
named volumes, so repeated deploys can rebind their documented host ports.
USAGE
}

run() {
  printf '\n> %s\n' "$*"
  "$@"
}

compose_repository_to_btm() {
  docker compose -f "${REPO_ROOT}/deployment/docker-compose/repository-to-btm.local.yml" "$@"
}

compose_full() {
  docker compose \
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

ensure_network() {
  ensure_docker_available

  if ! docker network inspect forensic_analytics >/dev/null 2>&1; then
    run docker network create forensic_analytics
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

  run compose_repository_to_btm down --remove-orphans
  run compose_full -p forensic-analytics-local down --remove-orphans
  run compose_gui_smoke down --remove-orphans
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
  stop_known_local_stacks
  build_repository_to_btm_jars
  run compose_repository_to_btm config
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
  stop_known_local_stacks
  build_full_jars
  build_ui
  run compose_full config
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
