terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "service_name" {
  type = string
}

variable "log_group_name" {
  type = string
}

variable "lb_arn_suffix" {
  type = string
}

variable "target_group_arn_suffix" {
  type = string
}

variable "dlq_names" {
  description = "Dead-letter queue names (any visible message = someone must look)"
  type        = map(string)
}

variable "queue_names" {
  type = map(string)
}

variable "db_identifier" {
  type = string
}

variable "db_replica_identifier" {
  type    = string
  default = null
}

variable "alert_email" {
  description = "null = no subscription (a subscription must be confirmed by the owner of the e-mail)"
  type        = string
  default     = null
}

variable "runbook_base_url" {
  type    = string
  default = "https://github.com/your-user/payflow/blob/main/docs/runbooks"
}

# trivy:ignore:AWS-0136 Accepted: same reason as the events topic (customer-managed key: P10)
resource "aws_sns_topic" "alerts" {
  name              = "${var.name}-alerts"
  kms_master_key_id = "alias/aws/sns"
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email == null ? 0 : 1
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# --- logs -> metric (the application writes ECS-format JSON, decision 136/137) ----------------
resource "aws_cloudwatch_log_metric_filter" "errors" {
  name           = "${var.name}-unexpected-errors"
  log_group_name = var.log_group_name
  pattern        = "{ $.log.level = \"ERROR\" }"

  metric_transformation {
    name          = "UnexpectedErrors"
    namespace     = "PayFlow"
    value         = "1"
    default_value = "0"
  }
}

locals {
  # Every alarm: severity + runbook are MANDATORY (decision 141); the test in tests/ proves it.
  simple_alarms = merge(
    {
      "ecs-cpu-high" = {
        severity    = "ticket"
        summary     = "ECS service CPU above 85% for 15 minutes (the autoscaler targets 60%: it may be at max_count)"
        namespace   = "AWS/ECS"
        metric      = "CPUUtilization"
        dimensions  = { ClusterName = var.cluster_name, ServiceName = var.service_name }
        statistic   = "Average"
        comparison  = "GreaterThanThreshold"
        threshold   = 85
        period      = 300
        evaluations = 3
        runbook     = "ecs-cpu-high.md"
      }
      "ecs-memory-high" = {
        severity    = "ticket"
        summary     = "ECS service memory above 85% for 15 minutes (JVM budget, P7.09)"
        namespace   = "AWS/ECS"
        metric      = "MemoryUtilization"
        dimensions  = { ClusterName = var.cluster_name, ServiceName = var.service_name }
        statistic   = "Average"
        comparison  = "GreaterThanThreshold"
        threshold   = 85
        period      = 300
        evaluations = 3
        runbook     = "ecs-memory-high.md"
      }
      "unhealthy-targets" = {
        severity    = "page"
        summary     = "At least one target is unhealthy for 3 minutes (readiness failing: database? startup?)"
        namespace   = "AWS/ApplicationELB"
        metric      = "UnHealthyHostCount"
        dimensions  = { TargetGroup = var.target_group_arn_suffix, LoadBalancer = var.lb_arn_suffix }
        statistic   = "Maximum"
        comparison  = "GreaterThanThreshold"
        threshold   = 0
        period      = 60
        evaluations = 3
        runbook     = "unhealthy-targets.md"
      }
      "target-latency-p95" = {
        severity           = "ticket"
        summary            = "p95 response time above 1 s for 10 minutes"
        namespace          = "AWS/ApplicationELB"
        metric             = "TargetResponseTime"
        dimensions         = { TargetGroup = var.target_group_arn_suffix, LoadBalancer = var.lb_arn_suffix }
        statistic          = null
        extended_statistic = "p95"
        comparison         = "GreaterThanThreshold"
        threshold          = 1
        period             = 300
        evaluations        = 2
        runbook            = "latency.md"
      }
      "unexpected-errors" = {
        severity    = "ticket"
        summary     = "ERROR-level log lines (unexpected failures; 404/405/400 are not ERROR, decision 144)"
        namespace   = "PayFlow"
        metric      = "UnexpectedErrors"
        dimensions  = {}
        statistic   = "Sum"
        comparison  = "GreaterThanThreshold"
        threshold   = 5
        period      = 300
        evaluations = 1
        runbook     = "unexpected-errors.md"
      }
      "rds-cpu-high" = {
        severity    = "ticket"
        summary     = "Database CPU above 80% for 15 minutes"
        namespace   = "AWS/RDS"
        metric      = "CPUUtilization"
        dimensions  = { DBInstanceIdentifier = var.db_identifier }
        statistic   = "Average"
        comparison  = "GreaterThanThreshold"
        threshold   = 80
        period      = 300
        evaluations = 3
        runbook     = "rds-cpu-high.md"
      }
      "rds-free-storage-low" = {
        severity    = "page"
        summary     = "Database free storage below 2 GiB"
        namespace   = "AWS/RDS"
        metric      = "FreeStorageSpace"
        dimensions  = { DBInstanceIdentifier = var.db_identifier }
        statistic   = "Minimum"
        comparison  = "LessThanThreshold"
        threshold   = 2147483648
        period      = 300
        evaluations = 1
        runbook     = "rds-storage.md"
      }
    },
    # one alarm per dead-letter queue: ANY visible message is an alert (nobody reads a DLQ by chance)
    { for k, q in var.dlq_names : "dlq-${k}-not-empty" => {
      severity    = "page"
      summary     = "Dead-letter queue ${q} has messages: a consumer is failing permanently"
      namespace   = "AWS/SQS"
      metric      = "ApproximateNumberOfMessagesVisible"
      dimensions  = { QueueName = q }
      statistic   = "Maximum"
      comparison  = "GreaterThanThreshold"
      threshold   = 0
      period      = 60
      evaluations = 1
      runbook     = "dlq.md"
    } },
    # backlog AGE of each main queue: a consumer that is slow or down shows up here first
    { for k, q in var.queue_names : "queue-${k}-old-message" => {
      severity    = "ticket"
      summary     = "Oldest message in ${q} is older than 5 minutes (consumer slow or down)"
      namespace   = "AWS/SQS"
      metric      = "ApproximateAgeOfOldestMessage"
      dimensions  = { QueueName = q }
      statistic   = "Maximum"
      comparison  = "GreaterThanThreshold"
      threshold   = 300
      period      = 60
      evaluations = 5
      runbook     = "queue-age.md"
    } },
    var.db_replica_identifier == null ? {} : {
      "rds-replica-lag" = {
        severity    = "ticket"
        summary     = "Read replica more than 10 minutes behind the primary (an IDLE PostgreSQL primary already reports up to 5 minutes: RDS docs)"
        namespace   = "AWS/RDS"
        metric      = "ReplicaLag"
        dimensions  = { DBInstanceIdentifier = var.db_replica_identifier }
        statistic   = "Maximum"
        comparison  = "GreaterThanThreshold"
        threshold   = 600
        period      = 60
        evaluations = 5
        runbook     = "replica-lag.md"
      }
    },
  )
}

resource "aws_cloudwatch_metric_alarm" "simple" {
  for_each            = local.simple_alarms
  alarm_name          = "${var.name}-${each.key}"
  alarm_description   = "[${each.value.severity}] ${each.value.summary} | runbook: ${var.runbook_base_url}/${each.value.runbook}"
  namespace           = each.value.namespace
  metric_name         = each.value.metric
  dimensions          = each.value.dimensions
  statistic           = try(each.value.statistic, null)
  extended_statistic  = try(each.value.extended_statistic, null)
  comparison_operator = each.value.comparison
  threshold           = each.value.threshold
  period              = each.value.period
  evaluation_periods  = each.value.evaluations
  treat_missing_data  = "notBreaching" # no data = no problem for THESE alarms; "the service went silent" (P6 PayflowNoMetrics) is NOT covered: exercise 3 of P9.07
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
}

# 5xx RATIO with a minimum traffic (metric math), the same idea as PayflowHighErrorRatio (decision 141)
resource "aws_cloudwatch_metric_alarm" "error_ratio" {
  alarm_name          = "${var.name}-http-5xx-ratio"
  alarm_description   = "[page] More than 5% of requests answered 5xx by the application for 10 minutes, with at least 30 requests per period (0.1 req/s) | runbook: ${var.runbook_base_url}/http-5xx.md"
  comparison_operator = "GreaterThanThreshold"
  threshold           = 5
  evaluation_periods  = 2
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  metric_query {
    id          = "ratio"
    expression  = "IF(requests >= 30, 100 * errors / requests, 0)"
    label       = "5xx percent (only with traffic)"
    return_data = true
  }

  metric_query {
    id = "errors"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "HTTPCode_Target_5XX_Count"
      period      = 300
      stat        = "Sum"
      dimensions  = { TargetGroup = var.target_group_arn_suffix, LoadBalancer = var.lb_arn_suffix }
    }
  }

  metric_query {
    id = "requests"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "RequestCount"
      period      = 300
      stat        = "Sum"
      dimensions  = { TargetGroup = var.target_group_arn_suffix, LoadBalancer = var.lb_arn_suffix }
    }
  }
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.name}-overview"
  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric", x = 0, y = 0, width = 12, height = 6
        properties = {
          title   = "Requests and 5xx (ALB)"
          region  = "us-east-1"
          stat    = "Sum"
          period  = 60
          metrics = [["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.lb_arn_suffix], [".", "HTTPCode_Target_5XX_Count", ".", "."]]
        }
      },
      {
        type = "metric", x = 12, y = 0, width = 12, height = 6
        properties = {
          title   = "p95 latency (s)"
          region  = "us-east-1"
          stat    = "p95"
          period  = 60
          metrics = [["AWS/ApplicationELB", "TargetResponseTime", "TargetGroup", var.target_group_arn_suffix, "LoadBalancer", var.lb_arn_suffix]]
        }
      },
      {
        type = "metric", x = 0, y = 6, width = 12, height = 6
        properties = {
          title   = "ECS CPU and memory"
          region  = "us-east-1"
          stat    = "Average"
          period  = 60
          metrics = [["AWS/ECS", "CPUUtilization", "ClusterName", var.cluster_name, "ServiceName", var.service_name], [".", "MemoryUtilization", ".", ".", ".", "."]]
        }
      },
    ]
  })
}

output "alerts_topic_arn" {
  value = aws_sns_topic.alerts.arn
}

output "alarm_names" {
  value = concat([for a in aws_cloudwatch_metric_alarm.simple : a.alarm_name], [aws_cloudwatch_metric_alarm.error_ratio.alarm_name])
}
