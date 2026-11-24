#!/usr/bin/env bash
# J10.05: security groups. A instancia NAO tem regra de entrada; o banco so aceita a instancia.
source "$(dirname "$0")/env.sh"
VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
[[ "$VPC_ID" != "None" ]] || { echo "no default VPC in ${AWS_REGION}" >&2; exit 1; }

tagspec() { echo "ResourceType=security-group,Tags=[{Key=project,Value=${PROJECT}},{Key=Name,Value=$1}]"; }
EC2_SG_ID="$(aws ec2 create-security-group --group-name "$EC2_SG" --description "payflow app (no inbound)" \
  --vpc-id "$VPC_ID" --tag-specifications "$(tagspec "$EC2_SG")" --query GroupId --output text)"
RDS_SG_ID="$(aws ec2 create-security-group --group-name "$RDS_SG" --description "payflow db (5432 from app only)" \
  --vpc-id "$VPC_ID" --tag-specifications "$(tagspec "$RDS_SG")" --query GroupId --output text)"
aws ec2 authorize-security-group-ingress --group-id "$RDS_SG_ID" --protocol tcp --port 5432 --source-group "$EC2_SG_ID" >/dev/null
echo "vpc=${VPC_ID} ec2_sg=${EC2_SG_ID} rds_sg=${RDS_SG_ID}"
