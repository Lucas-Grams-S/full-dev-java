#!/usr/bin/env bash
# Reproduces the experiments of P9.01-P9.08 against moto (AWS emulator; NO real account) and rewrites out-s*.txt.
# Needs: Docker (motoserver/moto, trivy image), Terraform >= 1.14, AWS CLI, JDK 25. Takes ~8 minutes.
set -Eeuo pipefail
cd "$(dirname "$0")"
OUT="$PWD"; TF="$OUT/terraform"; STUDY="$TF/envs/study"; LAB="$OUT/.."
PORT=5089
export TF_IN_AUTOMATION=1 TF_PLUGIN_CACHE_DIR="${TF_PLUGIN_CACHE_DIR:-$HOME/.cache/p8-tf-plugins}"
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 AWS_ENDPOINT_URL=http://localhost:$PORT AWS_PAGER=
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
VARS=(-var emulated=true -var create_replica=false -var enable_lambda=true)
TRIVY="ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb"
TRIVY_CACHE="${TRIVY_CACHE:-$HOME/.cache/p8-trivy}"
TMP="$(mktemp -d)"
tf() { terraform "$@" -no-color; }     # NOTE: -no-color must come after the subcommand

wipe() {
  rm -rf "${STUDY:?}"/.terraform "${STUDY:?}"/tfplan "${STUDY:?}"/.terraform.lock.hcl "${STUDY:?}"/terraform.tfstate "${STUDY:?}"/terraform.tfstate.backup
  for d in ephemeral leaky; do
    rm -rf "${OUT:?}/secret-state-lab/$d/.terraform" "${OUT:?}/secret-state-lab/$d/.terraform.lock.hcl" \
           "${OUT:?}/secret-state-lab/$d/terraform.tfstate" "${OUT:?}/secret-state-lab/$d/terraform.tfstate.backup"
  done
  find "${TF:?}/modules" -name .terraform -prune -exec rm -rf {} + 2>/dev/null || true
  find "${TF:?}/modules" -name .terraform.lock.hcl -delete 2>/dev/null || true
}
cleanup() { docker rm -f lab-p9-moto >/dev/null 2>&1 || true; wipe; rm -rf "${TMP:?}"; }
wipe; trap cleanup EXIT
docker rm -f lab-p9-moto >/dev/null 2>&1 || true
docker run -d --name lab-p9-moto -p 127.0.0.1:$PORT:5000 -e MOTO_IAM_LOAD_MANAGED_POLICIES=true motoserver/moto:latest >/dev/null
for _ in $(seq 1 40); do curl -s -o /dev/null http://localhost:$PORT/moto-api/ && break; sleep 0.5; done
mkdir -p "$LAB/lambda-lab/build/classes"
JARS="${LAMBDA_CORE_JAR:-$HOME/.m2/repository/com/amazonaws/aws-lambda-java-core/1.4.1/aws-lambda-java-core-1.4.1.jar}"
[ -f "$JARS" ] || { mkdir -p "$(dirname "$JARS")"; curl -s -o "$JARS" https://repo1.maven.org/maven2/com/amazonaws/aws-lambda-java-core/1.4.1/aws-lambda-java-core-1.4.1.jar; }
"$JAVA_HOME/bin/javac" -cp "$JARS" -d "$LAB/lambda-lab/build/classes" "$LAB/lambda-lab/handler/com/payflow/exporter/StatementExporter.java"
( cd "$LAB/lambda-lab/build/classes" && "$JAVA_HOME/bin/jar" cf ../exporter.zip . )

s1() {   # static checks + module tests + mutants
  echo '$ terraform fmt -recursive -check terraform/'; terraform fmt -recursive -check "$TF" && echo "(clean)"
  for m in network ecs_service observability messaging database; do
    echo "\$ (cd modules/$m && terraform test)"
    ( cd "$TF/modules/$m" && tf init -input=false >/dev/null && tf test | grep -E '^\s+run |Success|Failure' )
  done
  echo; echo '# MUTANT: an alarm without a runbook must make the observability test FAIL'
  cp -r "$TF/modules/observability" "$TMP/obs"; rm -rf "${TMP:?}/obs/.terraform" "${TMP:?}/obs/.terraform.lock.hcl"
  sed -i 's/ | runbook: \${var.runbook_base_url}\/\${each.value.runbook}//' "$TMP/obs/main.tf"
  ( cd "$TMP/obs" && tf init -input=false >/dev/null && tf test 2>&1 | grep -E '^\s+run |Success|Failure|^(every|the) ' | head -8 ) || true
  echo; echo '# MUTANT: the database rule opens 5432 to 0.0.0.0/0 (a CIDR instead of a security-group reference)'
  cp -r "$TF/modules/network" "$TMP/net"; rm -rf "${TMP:?}/net/.terraform" "${TMP:?}/net/.terraform.lock.hcl"
  python3 - "$TMP/net/main.tf" <<'PY'
import sys
p = sys.argv[1]
t = open(p).read()
old = '''  description                  = "PostgreSQL from the application security group only"
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.app.id'''
new = '''  description                  = "PostgreSQL from anywhere"
  security_group_id            = aws_security_group.db.id
  cidr_ipv4                    = "0.0.0.0/0"'''
assert old in t, "mutation target not found"
open(p, "w").write(t.replace(old, new))
PY
  ( cd "$TMP/net" && tf init -input=false >/dev/null && tf test 2>&1 | grep -E '^\s+run |Success|Failure|^the database' | head -10 ) || true
  echo; echo '# MUTANT: production forgets deletion protection (deletion_protection = false) -> the database test must FAIL'
  cp -r "$TF/modules/database" "$TMP/db"; rm -rf "${TMP:?}/db/.terraform" "${TMP:?}/db/.terraform.lock.hcl"
  sed -i 's/deletion_protection         = var.production/deletion_protection         = false/' "$TMP/db/main.tf"
  grep -q 'deletion_protection         = false' "$TMP/db/main.tf" || echo "(mutation did not apply)"
  ( cd "$TMP/db" && tf init -input=false >/dev/null && tf test 2>&1 | grep -E '^\s+run |Success|Failure|^production' | head -8 ) || true
}
s2() {   # plan + apply
  cd "$STUDY"
  echo '$ terraform init'; tf init -input=false | grep -E "Installing|Success"
  echo '$ terraform validate'; tf validate
  echo '$ terraform plan -out=tfplan   (emulated, no replica, with the Lambda)'
  tf plan -input=false "${VARS[@]}" -out=tfplan | grep -E "^Plan:"
  echo '$ terraform show -json tfplan | jq   (resources by type)'
  tf show -json tfplan | jq -r '.resource_changes[] | select(.change.actions==["create"]) | .type' | sort | uniq -c | sort -rn | head -40
  echo '$ terraform apply tfplan'
  SECONDS=0; tf apply -input=false tfplan | grep -E "Apply complete|^Error:" || true; echo "(apply took ${SECONDS}s)"
  echo '$ terraform output'; tf output | sed -E 's/(secret:[^"]*)-[A-Za-z0-9]{6}"/\1-xxxxxx"/'
}
s3() {   # network
  cd "$STUDY"; VPC="$(tf output -raw vpc_id)"
  echo '$ aws ec2 describe-subnets (name, CIDR, free IPs, public IP on launch)'
  aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC" --query 'sort_by(Subnets,&CidrBlock)[].[Tags[?Key==`Name`]|[0].Value,CidrBlock,AvailableIpAddressCount,MapPublicIpOnLaunch]' --output text | column -t
  echo '$ non-local routes of each route table (destination -> target)'
  for rt in $(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$VPC" --query 'RouteTables[].RouteTableId' --output text); do
    n="$(aws ec2 describe-route-tables --route-table-ids "$rt" --query 'RouteTables[0].Tags[?Key==`Name`].Value|[0]' --output text)"; [ "$n" = None ] && n="(main table, unused)"
    r="$(aws ec2 describe-route-tables --route-table-ids "$rt" --query 'RouteTables[0].Routes[?DestinationCidrBlock!=`10.20.0.0/16`].[DestinationCidrBlock,GatewayId,NatGatewayId]' --output text | sed 's/\tNone//g; s/\t/ -> /; s/^None -> /(S3 prefix list) -> /')"
    printf '%-24s %s\n' "$n" "${r:-(only the local VPC route)}"
  done | sort
  echo '$ aws ec2 describe-vpc-endpoints (service, type, network interfaces)'
  aws ec2 describe-vpc-endpoints --filters "Name=vpc-id,Values=$VPC" --query 'sort_by(VpcEndpoints,&ServiceName)[].[ServiceName,VpcEndpointType,length(SubnetIds)]' --output text | sed 's/com.amazonaws.us-east-1.//' | column -t
  echo -n 'NAT gateways in this VPC: '; aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=$VPC" --query 'length(NatGateways)' --output text
  echo '$ egress rules of the application security group'
  APPSG="$(aws ec2 describe-security-groups --filters "Name=vpc-id,Values=$VPC" "Name=group-name,Values=payflow-app-sg" --query 'SecurityGroups[0].GroupId' --output text)"
  aws ec2 describe-security-group-rules --filters "Name=group-id,Values=$APPSG" --query 'SecurityGroupRules[?IsEgress].[IpProtocol,FromPort,ToPort,CidrIpv4||ReferencedGroupInfo.GroupId||PrefixListId]' --output text | sed -E 's/sg-[0-9a-f]+/<sg>/; s/pl-[0-9a-f]+/<s3-prefix-list>/' | sort | column -t
  echo; echo '$ check-network.sh   (the real network, not the code)'; "$OUT/check-network.sh" "$VPC"
  echo '# someone "fixes" a deploy problem in the console: a route from the app route table to the internet gateway'
  IGW="$(aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=$VPC" --query 'InternetGateways[0].InternetGatewayId' --output text)"
  RT="$(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$VPC" "Name=tag:Name,Values=payflow-app-us-east-1a" --query 'RouteTables[0].RouteTableId' --output text)"
  aws ec2 create-route --route-table-id "$RT" --destination-cidr-block 0.0.0.0/0 --gateway-id "$IGW" >/dev/null
  echo '$ check-network.sh'; "$OUT/check-network.sh" "$VPC" || echo "(exit $?)"
    aws ec2 delete-route --route-table-id "$RT" --destination-cidr-block 0.0.0.0/0
  echo '# route removed again'; "$OUT/check-network.sh" "$VPC"
  echo; echo '$ terraform console   cidrsubnet("10.20.0.0/16", 8, 10)'; echo 'cidrsubnet("10.20.0.0/16", 8, 10)' | tf console
}
s4() {   # ECS / ALB / IAM
  cd "$STUDY"
  echo '$ aws ecs describe-services'
  aws ecs describe-services --cluster payflow-cluster --services payflow --query 'services[0].{status:status,desired:desiredCount,launchType:launchType,net:networkConfiguration.awsvpcConfiguration}' --output json | jq -c '.'
  echo '$ aws ecs describe-task-definition (what the container receives)'
  aws ecs describe-task-definition --task-definition payflow --query 'taskDefinition' --output json | jq '{family,cpu,memory,networkMode,runtimePlatform,taskRole:(.taskRoleArn|split("/")[-1]),executionRole:(.executionRoleArn|split("/")[-1]),container:(.containerDefinitions[0]|{image:(.image|split("@")|.[0]+"@"+(.[1][0:19])+"..."),readonlyRootFilesystem,user,stopTimeout,environment:[.environment[]|.name],secrets:([.secrets[]|{(.name):(.valueFrom|sub("-[A-Za-z0-9]{6}(?<t>:|$)";"-xxxxxx\(.t)"))}]|add)})}'
  echo '$ IAM: attached and inline policies of the execution role vs the task role'
  for r in payflow-ecs-execution payflow-ecs-task; do
    echo "[$r] attached: $(aws iam list-attached-role-policies --role-name $r --query 'AttachedPolicies[].PolicyName' --output text)  inline: $(aws iam list-role-policies --role-name $r --query 'PolicyNames' --output text)"
  done
  echo '$ task role policy (what YOUR code may do)'
  aws iam get-role-policy --role-name payflow-ecs-task --policy-name application --query 'PolicyDocument.Statement[].[Sid,Action,Resource]' --output json | jq -c '.[]' | sed -E 's/arn:aws:(sns|sqs):us-east-1:123456789012:/<\1>:/g'
  echo '$ aws elbv2 describe-target-groups (health check)'
  aws elbv2 describe-target-groups --names payflow-tg --query 'TargetGroups[0].{type:TargetType,port:Port,hcPort:HealthCheckPort,hcPath:HealthCheckPath,matcher:Matcher.HttpCode,interval:HealthCheckIntervalSeconds,healthy:HealthyThresholdCount,unhealthy:UnhealthyThresholdCount}' --output json | jq -c '.'
  echo -n 'deregistration_delay.timeout_seconds: '
  aws elbv2 describe-target-group-attributes --target-group-arn "$(aws elbv2 describe-target-groups --names payflow-tg --query 'TargetGroups[0].TargetGroupArn' --output text)" --query "Attributes[?Key=='deregistration_delay.timeout_seconds'].Value" --output text
  echo '$ scripts/ci/ecs-deploy.sh <cluster> <service> <repo> sha256:... (ECS_WAIT=0: moto runs no tasks, so the service never "becomes stable")'
  NEWDIGEST="sha256:$(printf 'payflow-v2' | sha256sum | cut -d' ' -f1)"
  ECS_WAIT=0 bash "$LAB/pipeline-lab/scripts/ci/ecs-deploy.sh" payflow-cluster payflow 000000000000.dkr.ecr.us-east-1.amazonaws.com/payflow "$NEWDIGEST"
  echo -n 'the service now points to: '; aws ecs describe-services --cluster payflow-cluster --services payflow --query 'services[0].taskDefinition' --output text | sed 's|.*/||'
  echo -n 'image of that revision:    '; aws ecs describe-task-definition --task-definition payflow --query 'taskDefinition.containerDefinitions[0].image' --output text | sed -E 's/(sha256:.{12}).*/\1.../'
  echo '$ the same script with a TAG instead of a digest'
  ECS_WAIT=0 bash "$LAB/pipeline-lab/scripts/ci/ecs-deploy.sh" payflow-cluster payflow 000000000000.dkr.ecr.us-east-1.amazonaws.com/payflow latest || echo "(exit $?)"
  echo '$ terraform plan after the pipeline moved the service to revision 2: what would Terraform change on the ECS service?'
  ( cd "$STUDY" && tf plan -input=false "${VARS[@]}" -out="$TMP/after-deploy" >/dev/null && tf show -json "$TMP/after-deploy" \
    | jq -r '.resource_changes[] | select(.type=="aws_ecs_service") | .change as $c | "\(.address): actions=\($c.actions|join(",")) attributes=\([($c.after|keys[]) as $k | select(($c.before[$k]) != ($c.after[$k])) | $k]|join(","))"' )
  echo '# task_definition is NOT in the list: the pipeline owns it (ignore_changes). The attribute listed is an emulator artifact (moto does not echo health_check_grace_period_seconds).'
  echo '$ aws logs describe-log-groups (retention in days)'
  aws logs describe-log-groups --log-group-name-prefix /ecs/payflow --query 'logGroups[].[logGroupName,retentionInDays]' --output text
}
s5() {   # messaging (SNS -> SQS fan-out with the infrastructure Terraform created)
  cd "$STUDY"; TOPIC="$(tf output -raw topic_arn)"
  QN="$(aws sqs get-queue-url --queue-name payflow-notifications --query QueueUrl --output text)"; QA="$(aws sqs get-queue-url --queue-name payflow-antifraud --query QueueUrl --output text)"
  DN="$(aws sqs get-queue-url --queue-name payflow-notifications-dlq --query QueueUrl --output text)"
  n() { aws sqs get-queue-attributes --queue-url "$1" --attribute-names ApproximateNumberOfMessages --query Attributes.ApproximateNumberOfMessages --output text; }
  echo '$ aws sns publish x3 (TransferCompleted, TransferFailed, TransferCompleted) with the eventType message attribute'
  for t in TransferCompleted TransferFailed TransferCompleted; do
    aws sns publish --topic-arn "$TOPIC" --message "{\"type\":\"$t\",\"amount\":\"10.00\"}" --message-attributes "{\"eventType\":{\"DataType\":\"String\",\"StringValue\":\"$t\"}}" --query MessageId --output text >/dev/null
  done
  echo "notifications queue: $(n "$QN") messages   (filter: TransferCompleted + TransferFailed)"
  echo "antifraud queue:     $(n "$QA") messages   (filter: TransferCompleted)"
  echo '$ aws sns publish WITHOUT the eventType attribute'
  aws sns publish --topic-arn "$TOPIC" --message '{"type":"NoAttribute"}' --query MessageId --output text >/dev/null
  echo "notifications queue: $(n "$QN")   antifraud queue: $(n "$QA")   <- unchanged: a filter policy drops the message silently"
  echo '$ aws sqs receive-message (raw delivery: the body is exactly the event)'
  aws sqs receive-message --queue-url "$QA" --max-number-of-messages 1 --query 'Messages[0].Body' --output text
  echo '$ queue attributes that matter'
  aws sqs get-queue-attributes --queue-url "$QN" --attribute-names VisibilityTimeout ReceiveMessageWaitTimeSeconds RedrivePolicy MessageRetentionPeriod SqsManagedSseEnabled --query Attributes --output json | jq -c '.' | sed -E 's/arn:aws:sqs:us-east-1:123456789012:/<sqs>:/'
  aws sqs get-queue-attributes --queue-url "$DN" --attribute-names MessageRetentionPeriod --query Attributes --output json | jq -c '{dlq_retention:.MessageRetentionPeriod}'
  echo; echo '$ EventBridge: put-events on the custom bus (MonthEnd from payflow.reporting) and one with another source'
  RQ="$(tf output -raw reporting_queue_url)"
  aws events put-events --entries '[{"EventBusName":"payflow","Source":"payflow.reporting","DetailType":"MonthEnd","Detail":"{\"month\":\"2026-10\"}"},{"EventBusName":"payflow","Source":"payflow.other","DetailType":"MonthEnd","Detail":"{}"}]' --query 'FailedEntryCount' --output text | sed 's/^/FailedEntryCount: /'
  echo "reporting queue: $(n "$RQ") message(s)  (only the matching event arrived)"
  aws sqs receive-message --queue-url "$RQ" --query 'Messages[0].Body' --output text | jq -c '{source,"detail-type",detail}'
}
s6() {   # secrets: where does the value live?
  cd "$STUDY"
  echo '$ aws secretsmanager list-secrets (names only)'; aws secretsmanager list-secrets --query 'sort_by(SecretList,&Name)[].Name' --output text | tr '\t' '\n'
  echo '$ aws ssm get-parameters-by-path --path /payflow/study --recursive'
  aws ssm get-parameters-by-path --path /payflow/study --recursive --query 'sort_by(Parameters,&Name)[].[Name,Type,Value]' --output text | column -t
  VAL="$(aws secretsmanager get-secret-value --secret-id payflow/study/jwt-signing-key --query SecretString --output text)"
  echo "secret length in Secrets Manager: ${#VAL} characters (the value itself is not printed)"
  echo '$ grep the Terraform state and plan for that value (module: ephemeral random_password + secret_string_wo)'
  echo "occurrences in terraform.tfstate: $(grep -c -F "$VAL" "$STUDY/terraform.tfstate" || true)"
  echo "occurrences in the saved plan   : $(tf show -json "$STUDY/tfplan" 2>/dev/null | grep -c -F "$VAL" || true)"
  echo; echo '# the contrast: the same secret created the "obvious" way (regular random_password + secret_string)'
  for d in ephemeral leaky; do
    ( cd "$OUT/secret-state-lab/$d" && tf init -input=false >/dev/null && tf apply -input=false -auto-approve >/dev/null 2>&1 )
    V="$(aws secretsmanager get-secret-value --secret-id "lab-p9/$d-key" --query SecretString --output text)"
    echo "$d: value length ${#V}, occurrences of the value in its terraform.tfstate = $(grep -c -F "$V" "$OUT/secret-state-lab/$d/terraform.tfstate" || true)"
    ( cd "$OUT/secret-state-lab/$d" && tf destroy -input=false -auto-approve >/dev/null 2>&1 )
  done
  echo; echo '# RDS-managed master secret (manage_master_user_password): the JSON that ECS reads with ":username::" and ":password::"'
  DBS="$(aws rds describe-db-instances --db-instance-identifier payflow-db --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)"
  echo "master secret ARN present on the instance: $([ "$DBS" != None ] && echo yes || echo no)"
  aws secretsmanager get-secret-value --secret-id "$DBS" --query SecretString --output text | jq -c 'keys'
}
s7() {   # CloudWatch
  cd "$STUDY"
  echo '$ aws cloudwatch describe-alarms (name, state, description without the runbook URL)'
  aws cloudwatch describe-alarms --alarm-name-prefix payflow- --query 'sort_by(MetricAlarms,&AlarmName)[].[AlarmName,StateValue,AlarmDescription]' --output text | sed -E 's/ \| runbook: .*\.md$//' | cut -c1-150
  echo "total alarms: $(aws cloudwatch describe-alarms --alarm-name-prefix payflow- --query 'length(MetricAlarms)' --output text)"
  echo '$ alarms grouped by (number of actions, has runbook link):'
  aws cloudwatch describe-alarms --alarm-name-prefix payflow- --query 'MetricAlarms[].[length(AlarmActions),contains(AlarmDescription,`runbook: https://`)]' --output text | sort | uniq -c
  echo '$ aws logs describe-metric-filters'
  aws logs describe-metric-filters --log-group-name /ecs/payflow --query 'metricFilters[].[filterName,filterPattern,metricTransformations[0].metricName,metricTransformations[0].metricNamespace]' --output text
  echo '# NOT validated here: moto accepts the JSON metric-filter pattern but does not evaluate it (filter-log-events returned every line, WARN included).'
  echo '# The pattern follows the documented syntax ($.property = "value") and the nested shape of the real application log (P6: "log":{"level":...}).'
  echo '$ force an alarm into ALARM and read it back'
  aws cloudwatch set-alarm-state --alarm-name payflow-dlq-notifications-not-empty --state-value ALARM --state-reason "lab: DLQ has 1 message"
  aws cloudwatch describe-alarms --alarm-names payflow-dlq-notifications-not-empty --query 'MetricAlarms[0].[StateValue,StateReason]' --output text
  echo '$ metric math 5xx ratio alarm: its queries'
  aws cloudwatch describe-alarms --alarm-names payflow-http-5xx-ratio --query 'MetricAlarms[0].Metrics[].[Id,Expression||MetricStat.Metric.MetricName]' --output text
}
s8() {   # RDS (CLI; Terraform replica fails against moto) + EKS shape
  cd "$STUDY"
  echo '$ aws rds describe-db-instances (primary created by Terraform)'
  aws rds describe-db-instances --db-instance-identifier payflow-db --query 'DBInstances[0].[DBInstanceIdentifier,Engine,EngineVersion,MultiAZ,PubliclyAccessible,StorageEncrypted,DeletionProtection,BackupRetentionPeriod,DBParameterGroups[0].DBParameterGroupName]' --output text
  echo '$ aws rds create-db-instance-read-replica / promote-read-replica (the Terraform replica does not complete in moto: see the lesson)'
  aws rds create-db-instance-read-replica --db-instance-identifier payflow-db-replica --source-db-instance-identifier payflow-db --query 'DBInstance.[DBInstanceIdentifier,ReadReplicaSourceDBInstanceIdentifier]' --output text
  aws rds promote-read-replica --db-instance-identifier payflow-db-replica --query 'DBInstance.[DBInstanceIdentifier,ReadReplicaSourceDBInstanceIdentifier]' --output text
  aws rds delete-db-instance --db-instance-identifier payflow-db-replica --skip-final-snapshot >/dev/null
  echo; echo '$ terraform apply -var enable_eks=true   (shape only; the control plane is billed per hour on real AWS)'
  tf apply -input=false -auto-approve "${VARS[@]}" -var enable_eks=true | grep -E "Apply complete"
  echo '$ aws eks describe-cluster'
  aws eks describe-cluster --name payflow-eks --query 'cluster.[name,status,resourcesVpcConfig.endpointPublicAccess,resourcesVpcConfig.endpointPrivateAccess]' --output text
  echo '$ aws eks update-kubeconfig --name payflow-eks   (the placeholder of deploy.yml, P8)'
  aws eks update-kubeconfig --name payflow-eks --kubeconfig "$TMP/kubeconfig" | sed "s|$TMP|<tmp>|"
  grep -E "^  name:|server:|command:|^      - |apiVersion: client" "$TMP/kubeconfig" | sed 's/^ *//' | head -12
  tf apply -input=false -auto-approve "${VARS[@]}" -var enable_eks=false | grep -E "Apply complete"
}
s9() {   # Lambda
  cd "$STUDY"
  echo '$ aws lambda get-function-configuration (the Terraform-created function)'
  aws lambda get-function-configuration --function-name payflow-statement-exporter --query '{runtime:Runtime,handler:Handler,arch:Architectures,memory:MemorySize,timeout:Timeout,snapStart:SnapStart}' --output json | jq -c '.'
  echo '$ aws lambda list-aliases'; aws lambda list-aliases --function-name payflow-statement-exporter --query 'Aliases[].[Name,FunctionVersion]' --output text
  echo "(SnapStart restore is NOT emulated by moto: this only proves the configuration shape.)"
}
s10() {  # second plan and destroy
  cd "$STUDY"
  echo '$ terraform plan -out=second   (second run, nothing changed in the code)'
  tf plan -input=false "${VARS[@]}" -out="$TMP/second" | grep -E "^Plan:|No changes"
  echo '$ attributes that differ, per resource (all of them are emulator artifacts: see the lesson)'
  tf show -json "$TMP/second" | jq -r '.resource_changes[] | select(.change.actions==["update"]) | .address as $a | (.change.before as $b | .change.after as $f | [($f|keys[]) as $k | select(($b[$k]) != ($f[$k])) | $k] | join(",")) as $d | "\($a)  ->  \($d)"' | sed -E 's/module\.//; s/\[0\]//'
  echo '$ terraform destroy'
  SECONDS=0; tf destroy -input=false -auto-approve "${VARS[@]}" | grep -E "Destroy complete|^Error:" || true; echo "(destroy took ${SECONDS}s)"
  echo '$ what is left in the emulator?'
  echo "vpcs (non-default): $(aws ec2 describe-vpcs --query 'length(Vpcs[?IsDefault==`false`])' --output text)  queues: $(aws sqs list-queues --query 'length(Queues||`[]`)' --output text)  topics: $(aws sns list-topics --query 'length(Topics)' --output text)  alarms: $(aws cloudwatch describe-alarms --query 'length(MetricAlarms)' --output text)  db instances: $(aws rds describe-db-instances --query 'length(DBInstances)' --output text)"
}
s11() {  # trivy config on the Terraform
  echo '$ trivy config terraform/   (summary per file, MEDIUM and above; ignores are inline with the reason)'
  docker run --rm -v "$TF:/src:ro" -v "$TRIVY_CACHE:/root/.cache/trivy" "$TRIVY" config --severity MEDIUM,HIGH,CRITICAL --quiet --skip-version-check --format json /src 2>/dev/null \
    | jq -r '[.Results[]? | select((.Misconfigurations // []) | length > 0) | {t:.Target,n:(.Misconfigurations|length),ids:([.Misconfigurations[].ID]|unique|join(","))}] | if length==0 then "no findings" else .[] | "\(.t)  \(.n)  \(.ids)" end' || true
  echo '$ trivy config (the SAME code with the inline ignores removed: what the ignores hide)'
  rm -rf "${TMP:?}/tfraw"; cp -r "$TF" "$TMP/tfraw"; find "$TMP/tfraw" -name .terraform -prune -exec rm -rf {} + 2>/dev/null || true
  find "$TMP/tfraw" -name '*.tf' -exec sed -i '/^# trivy:ignore/d' {} +
  docker run --rm -v "$TMP/tfraw:/src:ro" -v "$TRIVY_CACHE:/root/.cache/trivy" "$TRIVY" config --severity MEDIUM,HIGH,CRITICAL --quiet --skip-version-check --format json /src 2>/dev/null \
    | jq -r '[.Results[]? | select((.Misconfigurations // []) | length > 0) | {t:.Target,n:(.Misconfigurations|length),ids:([.Misconfigurations[].ID]|unique|join(","))}] | if length==0 then "no findings" else .[] | "\(.t)  \(.n)  \(.ids)" end' || true
}
for s in ${ONLY:-s1 s2 s3 s4 s5 s6 s7 s8 s9 s10 s11}; do   # ONLY=s1 reruns a single section
  echo "== $s"; ( set +e; "$s" ) > "$OUT/out-$s.txt" 2>&1 || true
done
echo "done: $(ls "$OUT"/out-s*.txt | wc -l) files"
