#!/usr/bin/env bash
# J10.07: prova que nada ficou. Sai com 1 se achar qualquer coisa do projeto.
source "$(dirname "$0")/env.sh"
found=0
check() {  # check "<descricao>" "<saida do comando>"
  if [[ -n "$2" && "$2" != "None" ]]; then echo "LEFTOVER $1: $2"; found=1; else echo "ok       $1"; fi
}
check "ec2 instances"   "$(aws ec2 describe-instances --filters "Name=tag:project,Values=${PROJECT}" Name=instance-state-name,Values=pending,running,stopping,stopped --query 'Reservations[].Instances[].InstanceId' --output text)"
check "ebs volumes"     "$(aws ec2 describe-volumes --filters "Name=tag:project,Values=${PROJECT}" --query 'Volumes[].VolumeId' --output text)"
check "elastic ips"     "$(aws ec2 describe-addresses --query 'Addresses[].PublicIp' --output text)"
check "snapshots"       "$(aws ec2 describe-snapshots --owner-ids self --filters "Name=tag:project,Values=${PROJECT}" --query 'Snapshots[].SnapshotId' --output text)"
check "rds instances"   "$(aws rds describe-db-instances --query "DBInstances[?DBInstanceIdentifier=='${DB_ID}'].DBInstanceIdentifier" --output text)"
check "rds snapshots"   "$(aws rds describe-db-snapshots --query "DBSnapshots[?DBInstanceIdentifier=='${DB_ID}'].DBSnapshotIdentifier" --output text)"
check "security groups" "$(aws ec2 describe-security-groups --filters "Name=tag:project,Values=${PROJECT}" --query 'SecurityGroups[].GroupId' --output text)"
check "s3 bucket"       "$(aws s3api list-buckets --query "Buckets[?Name=='${BUCKET}'].Name" --output text)"
check "ecr repository"  "$(aws ecr describe-repositories --query "repositories[?repositoryName=='${ECR_REPO}'].repositoryName" --output text 2>/dev/null || true)"
check "ssm parameters"  "$(aws ssm describe-parameters --parameter-filters Key=Name,Option=BeginsWith,Values=/payflow --query 'Parameters[].Name' --output text)"
check "iam roles"       "$(aws iam list-roles --query "Roles[?starts_with(RoleName, '${PROJECT}-')].RoleName" --output text)"
exit "$found"
