#!/usr/bin/env bash
# Pede ao SSM que a instancia baixe o pacote do S3 e rode deploy.sh; espera e mostra a saida.
set -Eeuo pipefail
: "${BUCKET:?}" "${INSTANCE_ID:?}" "${TAG:?}"
cmd="aws s3 cp s3://${BUCKET}/deploy/${TAG}/ /opt/payflow/ --recursive && bash /opt/payflow/deploy.sh ${TAG}"
params="$(jq -cn --arg c "$cmd" '{commands: [$c]}')"
cmd_id="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --comment "payflow deploy ${TAG}" --parameters "$params" --query Command.CommandId --output text)"
echo "command id: $cmd_id"
status=0
aws ssm wait command-executed --command-id "$cmd_id" --instance-id "$INSTANCE_ID" || status=$?
aws ssm get-command-invocation --command-id "$cmd_id" --instance-id "$INSTANCE_ID" \
  --query '[Status, StandardOutputContent, StandardErrorContent]' --output text
exit "$status"
