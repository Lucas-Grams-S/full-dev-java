# shellcheck shell=bash
# Carregado (source) pelos outros scripts. Falha alto se faltar algo.
set -Eeuo pipefail
: "${AWS_REGION:=us-east-1}"
export AWS_REGION AWS_DEFAULT_REGION="$AWS_REGION"
export AWS_PAGER=""                      # sem "less" no meio dos scripts
PROJECT=payflow
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET="${PROJECT}-deploy-${ACCOUNT_ID}-${AWS_REGION}"
ECR_REPO="$PROJECT"
EC2_ROLE="${PROJECT}-ec2-role"
EC2_PROFILE="${PROJECT}-ec2-profile"
GH_ROLE="${PROJECT}-github-deploy"
EC2_SG="${PROJECT}-ec2-sg"
RDS_SG="${PROJECT}-rds-sg"
DB_ID="${PROJECT}-db"
DB_SUBNETS="${PROJECT}-db-subnets"
INSTANCE_NAME="${PROJECT}-app"
OIDC_URL="token.actions.githubusercontent.com"
TAGS=("Key=project,Value=${PROJECT}")
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/../../deploy" && pwd)"

# Preenche os marcadores __X__ de um modelo JSON
render() {
  sed -e "s|__REGION__|${AWS_REGION}|g" -e "s|__ACCOUNT_ID__|${ACCOUNT_ID}|g" \
      -e "s|__BUCKET__|${BUCKET}|g" -e "s|__ECR_REPO__|${ECR_REPO}|g" -e "s|__SUB__|${GH_SUB:-}|g" "$1"
}
