#!/usr/bin/env bash
set -euo pipefail

joern --help >/tmp/joern-help.txt

if [ ! -s /tmp/joern-help.txt ]; then
  echo "Joern help output is empty. Joern may not be available on PATH." >&2
  exit 1
fi

echo "Joern CLI is available."
