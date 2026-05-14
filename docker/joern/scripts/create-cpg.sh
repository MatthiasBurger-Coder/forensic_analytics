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

# Java source analysis is delegated to Joern's frontend inside the container.
joern-parse -J-Xmx"${HEAP}" "${SOURCE_DIR}" --language javasrc --output "${CPG_FILE}"

echo "CPG created: ${CPG_FILE}"
