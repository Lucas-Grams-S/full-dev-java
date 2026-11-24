#!/usr/bin/env bash
# J10.05: RDS PostgreSQL + segredos no Parameter Store
source "$(dirname "$0")/env.sh"
VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
RDS_SG_ID="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=${RDS_SG}" "Name=vpc-id,Values=${VPC_ID}" --query 'SecurityGroups[0].GroupId' --output text)"
read -r -a SUBNETS <<< "$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=${VPC_ID}" Name=default-for-az,Values=true --query 'Subnets[].SubnetId' --output text)"
(( ${#SUBNETS[@]} >= 2 )) || { echo "RDS needs subnets in at least 2 AZs" >&2; exit 1; }
aws rds create-db-subnet-group --db-subnet-group-name "$DB_SUBNETS" --db-subnet-group-description "payflow" \
  --subnet-ids "${SUBNETS[@]}" --tags "${TAGS[@]}" >/dev/null

DB_PASSWORD="$(openssl rand -base64 32 | tr -d '/+=\n' | cut -c1-32)"
JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
aws rds create-db-instance --db-instance-identifier "$DB_ID" \
  --engine postgres --engine-version "${DB_ENGINE_VERSION:-18}" \
  --db-instance-class "${DB_CLASS:-db.t4g.micro}" --allocated-storage 20 --storage-type gp3 --storage-encrypted \
  --db-name payflow --master-username payflow --master-user-password "$DB_PASSWORD" \
  --db-subnet-group-name "$DB_SUBNETS" --vpc-security-group-ids "$RDS_SG_ID" \
  --no-publicly-accessible --no-multi-az --backup-retention-period 1 --no-deletion-protection \
  --tags "${TAGS[@]}" >/dev/null
aws ssm put-parameter --name /payflow/db_password --type SecureString --value "$DB_PASSWORD" --tags "${TAGS[@]}" >/dev/null
aws ssm put-parameter --name /payflow/jwt_secret  --type SecureString --value "$JWT_SECRET"  --tags "${TAGS[@]}" >/dev/null
unset DB_PASSWORD JWT_SECRET

echo "waiting for the database (10 to 15 minutes)..."
aws rds wait db-instance-available --db-instance-identifier "$DB_ID"
ENDPOINT="$(aws rds describe-db-instances --db-instance-identifier "$DB_ID" --query 'DBInstances[0].Endpoint.Address' --output text)"
aws ssm put-parameter --name /payflow/db_host --type String --value "$ENDPOINT" --tags "${TAGS[@]}" >/dev/null
echo "db endpoint: ${ENDPOINT}"
