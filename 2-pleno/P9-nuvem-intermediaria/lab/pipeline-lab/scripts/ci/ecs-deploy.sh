#!/usr/bin/env bash
# Deploys an image BY DIGEST to an ECS service: new task-definition revision -> update-service -> wait -> check the rollout state.
# usage: ecs-deploy.sh <cluster> <service> <repository-uri-without-tag> <sha256:digest>
# ECS_WAIT=0 skips the wait (ONLY for the moto lab: the emulator runs no tasks, so the service never becomes stable).
set -Eeuo pipefail
cluster="${1:?cluster}"; service="${2:?service}"; repo="${3:?repository uri}"; digest="${4:?digest}"
[[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "::error::not a digest (a tag is mutable): $digest" >&2; exit 2; }
tmp="$(mktemp -d)"; trap 'rm -rf "${tmp:?}"' EXIT

current="$(aws ecs describe-services --cluster "$cluster" --services "$service" --query 'services[0].taskDefinition' --output text)"
echo "current task definition: ${current##*/}"
aws ecs describe-task-definition --task-definition "$current" --query 'taskDefinition' --output json > "$tmp/current.json"

# Keep everything, change ONLY the image; drop the read-only fields that register-task-definition rejects.
jq --arg image "$repo@$digest" '
  .containerDefinitions |= (.[0].image = $image | .)
  | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
  "$tmp/current.json" > "$tmp/new.json"

new="$(aws ecs register-task-definition --cli-input-json "file://$tmp/new.json" --query 'taskDefinition.taskDefinitionArn' --output text)"
echo "registered: ${new##*/}  image=$repo@${digest:0:19}..."
aws ecs update-service --cluster "$cluster" --service "$service" --task-definition "$new" >/dev/null

if [ "${ECS_WAIT:-1}" = "1" ]; then
  # exits non-zero if the service does not become stable (the deployment circuit breaker rolls back by itself)
  aws ecs wait services-stable --cluster "$cluster" --services "$service"
  # shellcheck disable=SC2016 # the backticks are JMESPath literals, not shell expansion
  state="$(aws ecs describe-services --cluster "$cluster" --services "$service" --query 'services[0].deployments[?status==`PRIMARY`].rolloutState | [0]' --output text)"
  echo "rollout state: $state"
  [ "$state" = "COMPLETED" ] || { echo "::error::deployment did not complete (state: $state): it was rolled back or is stalled" >&2; exit 1; }
fi
echo "deployed ${new##*/}"
