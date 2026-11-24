#!/usr/bin/env bash
# Runs one function of lib.sh (used by load.py --at). WORK, STARTUP are inherited from the caller.
set -Eeuo pipefail
source "$(dirname "$0")/lib.sh"
"$@"
