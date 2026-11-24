# Cheatsheet: nuvem intermediária na AWS (P9)

> Referência rápida. **Executado** só contra o **moto** (emulador); veja as aulas P9.01–P9.08 para o que **não** foi executado. 💸 = cobra: destrua no mesmo dia.

## Rede
```bash
terraform console <<< 'cidrsubnet("10.20.0.0/16", 8, 10)'        # "10.20.10.0/24"
aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC    # quem tem rota 0.0.0.0/0 -> igw-?
aws ec2 describe-vpc-endpoints --filters Name=vpc-id,Values=$VPC --query 'VpcEndpoints[].[ServiceName,VpcEndpointType]'
```
Sub-rede: 5 IPs reservados (/24 = 251 úteis) · ALB: /27+ com 8 IPs livres · endpoint de gateway (S3): sem cobrança · NAT e endpoint de interface: 💸 por hora (+ GB).

## ECS
```bash
aws ecs describe-services --cluster C --services S --query 'services[0].[desiredCount,runningCount,deployments]'
aws ecs describe-task-definition --task-definition S --query 'taskDefinition.containerDefinitions[0].[image,secrets]'
aws ecs update-service --cluster C --service S --force-new-deployment     # o que fazer depois de rotacionar um segredo
aws ecs wait services-stable --cluster C --services S
```
Fargate 512 CPU = 1 a 4 GB · `stopTimeout` ≤ 120 s · segredo injetado **só na partida** · `valueFrom`: `ARN:chave-json:estágio:versão` (mantenha os `:`).

## SQS / SNS / EventBridge
```bash
aws sns publish --topic-arn T --message M --message-attributes '{"eventType":{"DataType":"String","StringValue":"TransferCompleted"}}'
aws sqs receive-message --queue-url Q --wait-time-seconds 20 --attribute-names ApproximateReceiveCount
aws sqs get-queue-attributes --queue-url Q --attribute-names ApproximateNumberOfMessages ApproximateAgeOfOldestMessage
aws events put-events --entries '[{"EventBusName":"payflow","Source":"payflow.reporting","DetailType":"MonthEnd","Detail":"{}"}]'
```
Padrão: *at-least-once*, ordem "melhor esforço" · visibility 30 s (padrão) a 12 h · retenção 4 a 14 dias · 1 MiB · DLQ conta a retenção do **envio original**.

## Segredos
```bash
aws secretsmanager get-secret-value --secret-id S --query SecretString --output text
aws secretsmanager put-secret-value --secret-id S --secret-string V      # nova versão (AWSCURRENT); a anterior vira AWSPREVIOUS
aws ssm get-parameters-by-path --path /payflow/study --recursive
grep -c -F "$VALOR" terraform.tfstate                                       # a prova: 0 com ephemeral + secret_string_wo
```

## CloudWatch
```bash
aws cloudwatch describe-alarms --alarm-name-prefix payflow- --query 'MetricAlarms[].[AlarmName,StateValue]'
aws cloudwatch set-alarm-state --alarm-name A --state-value ALARM --state-reason "teste"
aws logs describe-log-groups --query 'logGroups[].[logGroupName,retentionInDays]'     # null = indefinido (paga para sempre)
```

## RDS
```bash
aws rds describe-db-instances --db-instance-identifier I --query 'DBInstances[0].[MultiAZ,DeletionProtection,BackupRetentionPeriod]'
aws rds create-db-instance-read-replica --db-instance-identifier R --source-db-instance-identifier I
```
Multi-AZ: *standby* não lê, *failover* 60–120 s (DNS muda) · réplica: assíncrona (`ReplicaLag`; primário ocioso reporta até 5 min) · `networkaddress.cache.ttl` ≤ 60 s · pool: `tarefas × (maximum_percent/100) × pool ≤ max_connections`.

## Terraform (o que aprendi neste módulo)
`mock_provider` + `command = plan` (use `apply` quando o valor é calculado) · `validation` para recusar valores ruins · `ephemeral` + `*_wo` para segredos · `ignore_changes` para o que outro dono controla · **leia o `plan`** antes de aplicar (`destroy`, `forces replacement`).
