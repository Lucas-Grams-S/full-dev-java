#!/usr/bin/env bash
# J10.05: bucket privado do pacote de deploy e repositorio ECR
source "$(dirname "$0")/env.sh"

if [[ "$AWS_REGION" == "us-east-1" ]]; then
  aws s3api create-bucket --bucket "$BUCKET" >/dev/null
else
  aws s3api create-bucket --bucket "$BUCKET" --create-bucket-configuration "LocationConstraint=${AWS_REGION}" >/dev/null
fi
aws s3api put-public-access-block --bucket "$BUCKET" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
aws s3api put-bucket-encryption --bucket "$BUCKET" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
aws s3api put-bucket-lifecycle-configuration --bucket "$BUCKET" --lifecycle-configuration \
  '{"Rules":[{"ID":"expire-deploy-bundles","Status":"Enabled","Filter":{"Prefix":"deploy/"},"Expiration":{"Days":7}}]}'
aws s3api put-bucket-tagging --bucket "$BUCKET" --tagging "TagSet=[{Key=project,Value=${PROJECT}}]"

aws ecr create-repository --repository-name "$ECR_REPO" --image-tag-mutability IMMUTABLE \
  --image-scanning-configuration scanOnPush=true --tags "${TAGS[@]}" >/dev/null
echo "bucket: ${BUCKET}"
echo "ecr:    ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
