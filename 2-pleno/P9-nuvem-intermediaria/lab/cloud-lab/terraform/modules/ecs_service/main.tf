terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "image" {
  description = "Image by DIGEST (repository@sha256:...), decision 158: what was scanned is what runs"
  type        = string
  validation {
    condition     = can(regex("@sha256:[0-9a-f]{64}$", var.image))
    error_message = "Use an image reference by digest (repo@sha256:<64 hex>), never a mutable tag."
  }
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "target_group_arn" {
  type = string
}

variable "cpu" {
  description = "Fargate CPU units (1024 = 1 vCPU). Valid pairs with memory are listed in the Fargate documentation."
  type        = number
  default     = 512
}

variable "memory" {
  description = "MiB. JVM budget of P7.02: MaxRAMPercentage=70 of this value"
  type        = number
  default     = 1024
}

variable "desired_count" {
  type    = number
  default = 2
}

variable "min_count" {
  type    = number
  default = 2
}

variable "max_count" {
  type    = number
  default = 6
}

variable "cpu_target_percent" {
  type    = number
  default = 60
}

variable "environment" {
  type    = map(string)
  default = {}
}

variable "secrets" {
  description = "ENV_NAME => valueFrom (Secrets Manager ARN, optionally with ':json-key::', or Parameter Store ARN)"
  type        = map(string)
  default     = {}
}

variable "secret_arns" {
  description = "ARNs the EXECUTION role may read (it injects the secrets at task start)"
  type        = list(string)
  default     = []
}

variable "task_policy_json" {
  description = "Permissions of the application itself (task role): least privilege, only what the code calls"
  type        = string
}

variable "autoscaling" {
  description = "Target tracking on CPU. The moto emulator never completes aws_appautoscaling_target (the provider waits until timeout), so the lab turns it off there."
  type        = bool
  default     = true
}

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "stop_timeout" {
  description = "Seconds between SIGTERM and SIGKILL (P7: terminationGracePeriodSeconds 45; Fargate maximum is 120)"
  type        = number
  default     = 45
}

variable "health_check_grace_period" {
  description = "Seconds during which ALB health checks cannot kill a starting task (Spring Boot + Flyway startup)"
  type        = number
  default     = 90
}

data "aws_region" "current" {}

locals {
  region = data.aws_region.current.region
  java_options = join(" ", [
    "-XX:+UseG1GC",
    "-XX:MaxRAMPercentage=70",
    "-XX:+ExitOnOutOfMemoryError",
  ])
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name}"
  retention_in_days = var.log_retention_days # without it logs are kept FOREVER (and billed forever)
}

data "aws_iam_policy_document" "ecs_tasks_trust" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# EXECUTION role: used by the ECS agent BEFORE your code starts (pull image, write logs, read the secrets to inject).
resource "aws_iam_role" "execution" {
  name               = "${var.name}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_trust.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "execution_secrets" {
  count = length(var.secret_arns) > 0 ? 1 : 0
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = var.secret_arns
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  count  = length(var.secret_arns) > 0 ? 1 : 0
  name   = "read-injected-secrets"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_secrets[0].json
}

# TASK role: used by YOUR code (the AWS SDK inside the container). Separate on purpose.
resource "aws_iam_role" "task" {
  name               = "${var.name}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_trust.json
}

resource "aws_iam_role_policy" "task" {
  name   = "application"
  role   = aws_iam_role.task.id
  policy = var.task_policy_json
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name}-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = var.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64" # same architecture as the J10 host (t4g/Graviton); the image must be built for arm64
  }

  volume {
    name = "tmp" # readOnlyRootFilesystem needs a writable /tmp (same idea as the emptyDir of P7.03)
  }

  container_definitions = jsonencode([
    {
      name                   = "payflow"
      image                  = var.image
      essential              = true
      readonlyRootFilesystem = true
      user                   = "10001"
      stopTimeout            = var.stop_timeout
      portMappings = [
        { containerPort = 8080, protocol = "tcp" },
        { containerPort = 8081, protocol = "tcp" },
      ]
      mountPoints = [{ sourceVolume = "tmp", containerPath = "/tmp", readOnly = false }]
      linuxParameters = {
        initProcessEnabled = true
      }
      environment = concat(
        [{ name = "JAVA_TOOL_OPTIONS", value = local.java_options }],
        [for k, v in var.environment : { name = k, value = v }],
      )
      secrets = [for k, v in var.secrets : { name = k, valueFrom = v }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = local.region
          "awslogs-stream-prefix" = "app"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "app" {
  name                               = var.name
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.app.arn
  desired_count                      = var.desired_count
  launch_type                        = "FARGATE"
  deployment_minimum_healthy_percent = 100 # never go below the current capacity during a deploy (like maxUnavailable: 0, P7.03)
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = var.health_check_grace_period
  enable_execute_command             = false # ECS Exec = a shell inside production; turn on only for an investigation

  deployment_circuit_breaker {
    enable   = true # a deploy whose tasks never become healthy STOPS...
    rollback = true # ...and goes back to the last COMPLETED deployment (like helm --rollback-on-failure, P8.03)
  }

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "payflow"
    container_port   = 8080
  }

  lifecycle {
    # desired_count: the autoscaler owns it (same rule as omitting `replicas` with an HPA, decision 152).
    # task_definition: Terraform creates revision 1; the DEPLOY pipeline registers the next ones (new image digest). One owner per thing.
    ignore_changes = [desired_count, task_definition]
  }
}

resource "aws_appautoscaling_target" "app" {
  count              = var.autoscaling ? 1 : 0
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.min_count
  max_capacity       = var.max_count # 6 tasks x pool 10 < max_connections (decision 152)
}

resource "aws_appautoscaling_policy" "cpu" {
  count              = var.autoscaling ? 1 : 0
  name               = "${var.name}-cpu-target"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.app[0].service_namespace
  resource_id        = aws_appautoscaling_target.app[0].resource_id
  scalable_dimension = aws_appautoscaling_target.app[0].scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value       = var.cpu_target_percent
    scale_out_cooldown = 60
    scale_in_cooldown  = 300
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "service_name" {
  value = aws_ecs_service.app.name
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}

output "execution_role_arn" {
  value = aws_iam_role.execution.arn
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "task_definition_arn" {
  value = aws_ecs_task_definition.app.arn
}
