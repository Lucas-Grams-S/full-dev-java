#!/usr/bin/env bash
# Reproduces every Terraform experiment of P8.05-P8.07 against moto (AWS emulator; NO real account)
# and rewrites out-*.txt (the lessons quote these files).
# Needs: Docker (motoserver/moto, ghcr.io/aquasecurity/trivy), Terraform >= 1.14, AWS CLI. Takes ~5 minutes.
set -Eeuo pipefail
cd "$(dirname "$0")"
OUT="$PWD"; TF="$OUT/terraform"; STUDY="$TF/envs/study"; BOOT="$TF/bootstrap"
export TF_IN_AUTOMATION=1 TF_PLUGIN_CACHE_DIR="${TF_PLUGIN_CACHE_DIR:-$HOME/.cache/p8-tf-plugins}"
mkdir -p "$TF_PLUGIN_CACHE_DIR"
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 AWS_ENDPOINT_URL=http://localhost:5088 AWS_PAGER=
EP=(-var emulator_endpoint=http://localhost:5088)
TRIVY="ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb"
TRIVY_CACHE="${TRIVY_CACHE:-$HOME/.cache/p8-trivy}"; mkdir -p "$TRIVY_CACHE"; chmod 777 "$TRIVY_CACHE"
TMP="$(mktemp -d)"

wipe() {
  rm -rf "$STUDY"/{.terraform,tfplan,backend.tf,backend.hcl,import.tf,terraform.tfstate*,.terraform.lock.hcl} \
         "$BOOT"/{.terraform,terraform.tfstate*,.terraform.lock.hcl}
  find "$TF/modules" -name .terraform -prune -exec rm -rf {} + 2>/dev/null || true
  find "$TF/modules" -name .terraform.lock.hcl -delete 2>/dev/null || true
}
cleanup() { docker rm -f lab-p8-moto >/dev/null 2>&1 || true; wipe; rm -rf "$TMP"; }
wipe
trap cleanup EXIT
tf() { terraform "$@" -no-color; }   # NOTE: -no-color must come after the subcommand

docker rm -f lab-p8-moto >/dev/null 2>&1 || true
docker run -d --name lab-p8-moto -p 127.0.0.1:5088:5000 -e MOTO_IAM_LOAD_MANAGED_POLICIES=true motoserver/moto:latest >/dev/null
for _ in $(seq 1 40); do curl -s -o /dev/null http://localhost:5088/moto-api/ && break; sleep 0.5; done

s1() {
  echo '$ terraform fmt -recursive -check terraform/'
  terraform fmt -recursive -check "$TF" && echo "(clean)"
  cd "$STUDY"
  echo '$ terraform init'; tf init -input=false | grep -E "Installing|Success"
  echo '$ terraform validate'; tf validate
  echo '$ terraform plan -out=tfplan'
  tf plan -input=false "${EP[@]}" -out=tfplan | grep -E "^Plan:|will be created" | sed 's/^ *//' | sort
}
s2() {
  cd "$STUDY"
  echo '$ terraform apply tfplan'
  SECONDS=0; tf apply -input=false tfplan | grep -E "Apply complete|^(db_endpoint|deploy_role_arn|ecr_repository_url|instance_id)|Creation complete" | sed -E 's/\[id=[^]]*\]//'
  echo "(apply took ${SECONDS}s)"
  echo '$ terraform state list | wc -l'; tf state list | wc -l
  echo '$ terraform state list | grep -v data'; tf state list | grep -v '\.data\.'| grep -v '^data\.' | head -40
}
s3() {
  cd "$STUDY"
  echo '$ terraform plan   (second run, nothing changed in the code)'
  tf plan -input=false "${EP[@]}" | grep -E "^Plan:|No changes|will be updated|referenced_security_group_id|tags_all|^\s+[~+-] " | sed 's/^ *//' | head -12
  echo "# the 2 diffs above are emulator artifacts (moto does not echo tags on S3 buckets and prefixes the account id in the SG reference)."
  echo "# they are NOT a lesson about Terraform: on real AWS the second plan is 'No changes'."
}
s4() {
  cd "$STUDY"
  echo '$ aws ecr put-image-tag-mutability --repository-name payflow --image-tag-mutability MUTABLE   (someone clicks in the console)'
  aws ecr put-image-tag-mutability --repository-name payflow --image-tag-mutability MUTABLE >/dev/null
  echo '$ terraform plan'
  tf plan -input=false "${EP[@]}" | grep -A6 'aws_ecr_repository.this will be updated' | sed 's/^ *//'
  echo '$ terraform apply -auto-approve   (Terraform puts it back)'
  tf apply -auto-approve -input=false "${EP[@]}" | grep -E "Modifying|Apply complete"
  echo -n 'now: '; aws ecr describe-repositories --repository-names payflow --query 'repositories[0].imageTagMutability' --output text
}
s5() {
  cd "$STUDY"; cp main.tf "$TMP/main.bak"; cp outputs.tf "$TMP/outputs.bak"
  echo '# rename module.registry -> module.container_registry WITHOUT a moved block'
  sed -i 's/module "registry"/module "container_registry"/; s/module\.registry\./module.container_registry./g' main.tf outputs.tf; tf init -input=false >/dev/null
  tf plan -input=false "${EP[@]}" | grep -E "^Plan:|ecr_repository.this will be|ecr_lifecycle_policy.keep_last will be" | sed 's/^ *//'
  echo '# the same rename WITH a moved block'
  printf '\nmoved {\n  from = module.registry\n  to   = module.container_registry\n}\n' >> main.tf
  tf plan -input=false "${EP[@]}" | grep -E "^Plan:|has moved to" | sed 's/^ *//'
  cp "$TMP/main.bak" main.tf; cp "$TMP/outputs.bak" outputs.tf; tf init -input=false >/dev/null
}
s6() {
  cd "$BOOT"
  tf init -input=false >/dev/null
  echo '$ (bootstrap) terraform apply   -> the bucket that will hold the state'
  tf apply -auto-approve -input=false "${EP[@]}" | grep -E "Apply complete|state_bucket"
  cd "$STUDY"
  cp backend.tf.example backend.tf
  { sed 's/<ACCOUNT_ID>/123456789012/' backend.hcl.example
    echo 'endpoints = { s3 = "http://localhost:5088" }'
    echo 'use_path_style = true'
    echo 'skip_credentials_validation = true'
    echo 'skip_requesting_account_id = true'
    echo 'skip_metadata_api_check = true'; } > backend.hcl
  echo '$ terraform init -migrate-state -backend-config=backend.hcl'
  tf init -migrate-state -force-copy -input=false -backend-config=backend.hcl | grep -E "Successfully configured|successfully initialized"
  echo '$ aws s3 ls s3://payflow-tfstate-.../study/'
  aws s3 ls s3://payflow-tfstate-123456789012-us-east-1/study/ | awk '{print $3, $4}'
  echo '# terminal A: terraform apply waiting for "yes" (holds the lock); terminal B: terraform plan'
  (sleep 20 | terraform apply -no-color "${EP[@]}" > "$TMP/hold.txt" 2>&1 &)
  sleep 8
  echo '$ aws s3 ls (while A waits)'; aws s3 ls s3://payflow-tfstate-123456789012-us-east-1/study/ | awk '{print $3, $4}'
  echo '$ terraform plan   (terminal B)'
  tf plan -input=false -lock-timeout=3s "${EP[@]}" 2>&1 | grep -E "Error acquiring|StatusCode|PreconditionFailed|Operation:|Version:" | sed -E 's/RequestID: ?[A-Za-z0-9]*,?//; s/HostID: ?[^,]*,?//; s/Who: .*/Who:       <user>@<host>/' | sed 's/^ *//'
  sleep 14
  echo '$ aws s3 ls (after A gave up)'; aws s3 ls s3://payflow-tfstate-123456789012-us-east-1/study/ | awk '{print $3, $4}'
}
s7() {
  cd "$STUDY"
  echo '$ terraform state rm aws_ssm_parameter.db_endpoint    (the resource still exists in the cloud)'
  tf state rm aws_ssm_parameter.db_endpoint | tail -1
  echo '$ terraform plan'; tf plan -input=false "${EP[@]}" | grep -E "ssm_parameter.db_endpoint will be|^Plan:" | sed 's/^ *//'
  printf 'import {\n  to = aws_ssm_parameter.db_endpoint\n  id = "/payflow/db/endpoint"\n}\n' > import.tf
  echo '$ terraform plan   (with an import block)'; tf plan -input=false "${EP[@]}" | grep -E "will be imported|^Plan:" | sed 's/^ *//'
  tf apply -auto-approve -input=false "${EP[@]}" | grep -E "Import complete|Apply complete"
  rm -f import.tf
}
s7b() {
  cd "$STUDY"
  echo '# a harmless-looking change to the database module: db_name "payflow" -> "payflow2"'
  cp "$TF/modules/database/main.tf" "$TMP/db.bak"
  sed -i 's/db_name                     = "payflow"/db_name                     = "payflow2"/' "$TF/modules/database/main.tf"
  echo '$ terraform plan'
  tf plan -input=false "${EP[@]}" | grep -E "aws_db_instance.this must be replaced|forces replacement|^Plan:" | sed 's/^ *//'
  echo '# the safety net: prevent_destroy on the database'
  sed -i 's/  deletion_protection         = false # study environment only/  deletion_protection         = false # study environment only\n  lifecycle { prevent_destroy = true }/' "$TF/modules/database/main.tf"
  echo '$ terraform plan'
  tf plan -input=false "${EP[@]}" 2>&1 | grep -E "Instance cannot be destroyed|prevent_destroy" | head -2 | sed 's/^ *//'
  cp "$TMP/db.bak" "$TF/modules/database/main.tf"
}
s8() {
  for m in registry github_oidc; do
    echo "\$ (modules/$m) terraform test"
    ( cd "$TF/modules/$m"; tf init -input=false -backend=false >/dev/null; tf test | grep -E "run \"|Success|Failure" | sed 's/^ *//' )
  done
  echo '# MUTANTS: break the module on purpose; the tests must FAIL'
  cp "$TF/modules/registry/main.tf" "$TMP/reg.bak"; cp "$TF/modules/github_oidc/main.tf" "$TMP/oidc.bak"
  sed -i 's/"IMMUTABLE"/"MUTABLE"  /' "$TF/modules/registry/main.tf"
  python3 - "$TF/modules/github_oidc/main.tf" <<'PY'
import sys
p = sys.argv[1]; s = open(p).read()
s = s.replace('test     = "StringEquals"\n      variable = "token.actions.githubusercontent.com:sub"\n      values   = ["repo:${var.github_repository}:environment:${var.github_environment}"]',
              'test     = "StringLike"\n      variable = "token.actions.githubusercontent.com:sub"\n      values   = ["repo:acme/*"]')
open(p, "w").write(s)
PY
  for m in registry github_oidc; do
    echo "\$ (mutant of modules/$m) terraform test"
    ( cd "$TF/modules/$m"; tf test | grep -E "run \"|Success|Failure" | sed 's/^ *//' ) || true
  done
  cp "$TMP/reg.bak" "$TF/modules/registry/main.tf"; cp "$TMP/oidc.bak" "$TF/modules/github_oidc/main.tf"
}
s9() {
  cd "$STUDY"
  echo '$ terraform destroy'
  SECONDS=0; tf destroy -auto-approve -input=false "${EP[@]}" | grep -E "Destroy complete"; echo "(destroy took ${SECONDS}s)"
  echo '$ what is left in the account (should be only the state bucket)'
  echo -n 'ec2 not terminated: '; aws ec2 describe-instances --query 'length(Reservations[].Instances[?State.Name!=`terminated`][])' --output text
  echo -n 'rds: ';  aws rds describe-db-instances --query 'length(DBInstances)' --output text
  echo -n 'ecr: ';  aws ecr describe-repositories --query 'length(repositories)' --output text
  echo -n 'ssm /payflow: '; aws ssm describe-parameters --no-paginate --query 'length(Parameters[?starts_with(Name,`/payflow`)])' --output text
  echo -n 'iam roles payflow*: '; aws iam list-roles --no-paginate --query 'length(Roles[?starts_with(RoleName,`payflow`)])' --output text
  echo -n 'buckets: '; aws s3 ls | awk '{print $3}'
  echo '$ (bootstrap) terraform destroy   -> prevent_destroy protects the state bucket'
  cd "$BOOT"; tf destroy -auto-approve -input=false "${EP[@]}" 2>&1 | grep -E "Instance cannot be destroyed|prevent_destroy" | head -2 | sed 's/^ *//'
}
s10() {
  echo '$ trivy config terraform/   (with the justified inline ignores)'
  docker run --rm -v "$TRIVY_CACHE":/root/.cache/ -v "$TF":/src:ro "$TRIVY" config -q --severity MEDIUM,HIGH,CRITICAL --exit-code 1 /src >/dev/null 2>&1 && echo "exit 0: no finding above LOW"
  echo '$ the same scan WITHOUT the ignore comments'
  rm -rf "$TMP/raw"; cp -r "$TF" "$TMP/raw"; find "$TMP/raw" -name '*.tf' -exec sed -i '/trivy:ignore/d' {} +
  docker run --rm -v "$TRIVY_CACHE":/root/.cache/ -v "$TMP/raw":/src:ro "$TRIVY" config -q --format json /src 2>/dev/null | python3 -c "
import json, sys
d = json.load(sys.stdin)
rows = sorted({(r['Target'].split('/')[-2] + '/' + r['Target'].split('/')[-1], m['ID'], m['Severity']) for r in d['Results'] for m in r.get('Misconfigurations', []) if m['Status'] == 'FAIL'})
for t, i, s in rows: print(f'  {s:<8} {i:<9} {t}')
print(f'{len(rows)} findings (without ignores)')"
}

s1  > "$OUT/out-1-plan.txt" 2>&1 || true
s2  > "$OUT/out-2-apply.txt" 2>&1 || true
s3  > "$OUT/out-3-second-plan.txt" 2>&1 || true
s4  > "$OUT/out-4-drift.txt" 2>&1 || true
s5  > "$OUT/out-5-moved.txt" 2>&1 || true
s6  > "$OUT/out-6-remote-state-lock.txt" 2>&1 || true
s7  > "$OUT/out-7-import.txt" 2>&1 || true
s7b > "$OUT/out-7b-forces-replacement.txt" 2>&1 || true
s8  > "$OUT/out-8-tests.txt" 2>&1 || true
s9  > "$OUT/out-9-destroy.txt" 2>&1 || true
s10 > "$OUT/out-10-trivy-config.txt" 2>&1 || true
echo "done: $(ls "$OUT"/out-*.txt | wc -l) files"
