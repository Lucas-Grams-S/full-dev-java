#!/usr/bin/env bash
# J10.05: a instancia EC2 (Amazon Linux 2023, IMDSv2 obrigatorio, sem chave SSH, sem porta aberta)
source "$(dirname "$0")/env.sh"
VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
EC2_SG_ID="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=${EC2_SG}" "Name=vpc-id,Values=${VPC_ID}" --query 'SecurityGroups[0].GroupId' --output text)"
INSTANCE_ID="$(aws ec2 run-instances \
  --image-id "resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64" \
  --instance-type "${INSTANCE_TYPE:-t3.small}" \
  --iam-instance-profile "Name=${EC2_PROFILE}" \
  --security-group-ids "$EC2_SG_ID" \
  --metadata-options "HttpTokens=required,HttpEndpoint=enabled,HttpPutResponseHopLimit=1" \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3,Encrypted=true,DeleteOnTermination=true}' \
  --user-data "file://${DEPLOY_DIR}/user-data.sh" \
  --tag-specifications "ResourceType=instance,Tags=[{Key=project,Value=${PROJECT}},{Key=Name,Value=${INSTANCE_NAME}}]" \
                       "ResourceType=volume,Tags=[{Key=project,Value=${PROJECT}}]" \
  --query 'Instances[0].InstanceId' --output text)"
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
echo "INSTANCE_ID=${INSTANCE_ID}"
