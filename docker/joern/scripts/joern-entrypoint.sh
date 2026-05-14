#!/usr/bin/env bash
set -euo pipefail

mkdir -p "${ANALYSIS_INPUT_DIR:-/analysis/input}"
mkdir -p "${JOERN_WORKSPACE_DIR:-/analysis/workspace}"
mkdir -p "${ANALYSIS_OUTPUT_DIR:-/analysis/output}"
mkdir -p "${ANALYSIS_LOG_DIR:-/analysis/logs}"

exec "$@"
