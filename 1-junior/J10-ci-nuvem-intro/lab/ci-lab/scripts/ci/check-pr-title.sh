#!/usr/bin/env bash
# Valida o titulo da PR contra Conventional Commits (a mesma regex do hook da J7.09).
set -Eeuo pipefail
title="${1:-}"
pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._-]+\))?!?: .{1,72}$'
if [[ "$title" =~ $pattern ]]; then
  echo "ok: title follows Conventional Commits"
else
  echo "error: PR title must look like 'feat(scope): short description' (got a title that does not match)" >&2
  exit 1
fi
