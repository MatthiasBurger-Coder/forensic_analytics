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

joern -J-Xmx"${HEAP}" --script "${QUERY_FILE}" "${CPG_FILE}" | tee "${OUTPUT_FILE}"
