#!/usr/bin/env bash
# J10.07: destroi TUDO que os scripts criaram, na ordem inversa. Uso: CONFIRM=payflow bash 90-teardown.sh
source "$(dirname "$0")/env.sh"
[[ "${CONFIRM:-}" == "$PROJECT" ]] || { echo "this deletes the ${PROJECT} stack in ${AWS_REGION}; run with CONFIRM=${PROJECT}" >&2; exit 1; }
try() { "$@" || echo "  (skipped: $*)" >&2; }     # idempotente: o que ja nao existe nao para o resto

echo ">> EC2"
ids="$(aws ec2 describe-instances --filters "Name=tag:project,Values=${PROJECT}" Name=instance-state-name,Values=pending,running,stopping,stopped \
  --query 'Reservations[].Instances[].InstanceId' --output text)"
if [[ -n "$ids" ]]; then
  # shellcheck disable=SC2086
  aws ec2 terminate-instances --instance-ids $ids >/dev/null
  # shellcheck disable=SC2086
  aws ec2 wait instance-terminated --instance-ids $ids
fi

echo ">> RDS (sem snapshot final: o banco de estudo nao guarda dados reais)"
if aws rds describe-db-instances --db-instance-identifier "$DB_ID" >/dev/null 2>&1; then
  aws rds delete-db-instance --db-instance-identifier "$DB_ID" --skip-final-snapshot --delete-automated-backups >/dev/null
  aws rds wait db-instance-deleted --db-instance-identifier "$DB_ID"
fi
try aws rds delete-db-subnet-group --db-subnet-group-name "$DB_SUBNETS"

echo ">> security groups (o do banco primeiro: ele referencia o da instancia)"
for sg in "$RDS_SG" "$EC2_SG"; do
  gid="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=${sg}" --query 'SecurityGroups[0].GroupId' --output text)"
  [[ "$gid" == "None" ]] || try aws ec2 delete-security-group --group-id "$gid"
done

echo ">> ECR e S3"
try aws ecr delete-repository --repository-name "$ECR_REPO" --force >/dev/null
if aws s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  aws s3 rm "s3://${BUCKET}" --recursive >/dev/null
  aws s3api delete-bucket --bucket "$BUCKET"
fi

echo ">> parametros do SSM"
try aws ssm delete-parameters --names /payflow/db_password /payflow/jwt_secret /payflow/db_host >/dev/null

echo ">> IAM"
try aws iam remove-role-from-instance-profile --instance-profile-name "$EC2_PROFILE" --role-name "$EC2_ROLE"
try aws iam delete-instance-profile --instance-profile-name "$EC2_PROFILE"
try aws iam detach-role-policy --role-name "$EC2_ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
try aws iam delete-role-policy --role-name "$EC2_ROLE" --policy-name payflow-runtime
try aws iam delete-role --role-name "$EC2_ROLE"
try aws iam delete-role-policy --role-name "$GH_ROLE" --policy-name payflow-deploy
try aws iam delete-role --role-name "$GH_ROLE"
try aws iam delete-open-id-connect-provider --open-id-connect-provider-arn "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${OIDC_URL}"

echo ">> budget (opcional: ele nao cobra; apague se quiser)"
echo "done. Now run 91-verify-empty.sh"
