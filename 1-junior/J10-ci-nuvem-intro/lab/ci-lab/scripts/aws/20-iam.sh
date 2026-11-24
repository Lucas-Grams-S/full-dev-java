#!/usr/bin/env bash
# J10.05/06: papel da instancia (EC2) e papel do GitHub Actions (OIDC). Uso: GH_REPO=dono/payflow bash 20-iam.sh
source "$(dirname "$0")/env.sh"
: "${GH_REPO:?set GH_REPO=owner/repo}"
GH_SUB="${GH_SUB:-repo:${GH_REPO}:environment:production}"   # confira o formato real da claim sub (J10.06)

# --- papel da instancia: le segredos do SSM, baixa imagem do ECR e pacote do S3
aws iam create-role --role-name "$EC2_ROLE" --assume-role-policy-document "file://${DEPLOY_DIR}/iam/ec2-trust.json" --tags "${TAGS[@]}" >/dev/null
aws iam attach-role-policy --role-name "$EC2_ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam put-role-policy --role-name "$EC2_ROLE" --policy-name payflow-runtime --policy-document "$(render "${DEPLOY_DIR}/iam/ec2-policy.json")"
aws iam create-instance-profile --instance-profile-name "$EC2_PROFILE" --tags "${TAGS[@]}" >/dev/null
aws iam add-role-to-instance-profile --instance-profile-name "$EC2_PROFILE" --role-name "$EC2_ROLE"

# --- GitHub Actions sem chave de acesso guardada: provedor OIDC + papel que so o seu repositorio assume
aws iam create-open-id-connect-provider --url "https://${OIDC_URL}" --client-id-list sts.amazonaws.com --tags "${TAGS[@]}" >/dev/null
aws iam create-role --role-name "$GH_ROLE" --assume-role-policy-document "$(render "${DEPLOY_DIR}/iam/github-trust.json")" --tags "${TAGS[@]}" >/dev/null
aws iam put-role-policy --role-name "$GH_ROLE" --policy-name payflow-deploy --policy-document "$(render "${DEPLOY_DIR}/iam/github-policy.json")"
echo "AWS_ROLE_ARN=arn:aws:iam::${ACCOUNT_ID}:role/${GH_ROLE}"
