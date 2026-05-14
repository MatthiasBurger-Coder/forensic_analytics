#!/usr/bin/env bash
set -euo pipefail

rm -rf "${JOERN_WORKSPACE_DIR:-/analysis/workspace}"/*
rm -rf "${ANALYSIS_OUTPUT_DIR:-/analysis/output}"/*
rm -rf "${ANALYSIS_LOG_DIR:-/analysis/logs}"/*

echo "Joern workspace, output and logs cleaned."
