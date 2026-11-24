terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "consumers" {
  description = "One queue (+ its dead-letter queue) per consumer, all subscribed to the same topic (fan-out)"
  type        = map(object({ event_types = list(string) }))
  default = {
    notifications = { event_types = ["TransferCompleted", "TransferFailed"] }
    antifraud     = { event_types = ["TransferCompleted"] }
  }
}

variable "max_receive_count" {
  description = "Deliveries before a message goes to the dead-letter queue"
  type        = number
  default     = 5
  validation {
    condition     = var.max_receive_count >= 1
    error_message = "max_receive_count must be at least 1 (0 would never deliver, or never dead-letter)."
  }
}

variable "restrict_dlq_sources" {
  description = "Only the paired main queue may use a DLQ (redrive allow policy). The moto emulator never returns this attribute, so the provider waits forever: the lab turns it off there."
  type        = bool
  default     = true
}

variable "visibility_timeout" {
  description = "Seconds. Must be GREATER than the worst-case processing time of one message"
  type        = number
  default     = 60
  validation {
    condition     = var.visibility_timeout >= 0 && var.visibility_timeout <= 43200
    error_message = "SQS accepts 0 to 43200 seconds (12 hours) of visibility timeout."
  }
}

# trivy:ignore:AWS-0136 Accepted: the AWS-managed key (alias/aws/sns) encrypts the topic; a customer-managed key is a P10 topic
resource "aws_sns_topic" "events" {
  name              = "${var.name}-transfers"
  kms_master_key_id = "alias/aws/sns" # encrypted at rest (AWS managed key; a customer key is P10)
}

# Dead-letter queues first: the main queues point at them.
resource "aws_sqs_queue" "dlq" {
  for_each                  = var.consumers
  name                      = "${var.name}-${each.key}-dlq"
  message_retention_seconds = 1209600 # 14 days (the maximum): time to investigate and redrive
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "main" {
  for_each                   = var.consumers
  name                       = "${var.name}-${each.key}"
  visibility_timeout_seconds = var.visibility_timeout
  receive_wait_time_seconds  = 20 # long polling: fewer empty receives
  message_retention_seconds  = 345600
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "dlq" {
  for_each  = var.restrict_dlq_sources ? var.consumers : {}
  queue_url = aws_sqs_queue.dlq[each.key].id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.main[each.key].arn]
  })
}

data "aws_iam_policy_document" "from_sns" {
  for_each = var.consumers
  statement {
    sid       = "AllowTopicToSend"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.main[each.key].arn]
    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.events.arn] # ONLY this topic, not any SNS topic in the world
    }
  }
}

resource "aws_sqs_queue_policy" "from_sns" {
  for_each  = var.consumers
  queue_url = aws_sqs_queue.main[each.key].id
  policy    = data.aws_iam_policy_document.from_sns[each.key].json
}

resource "aws_sns_topic_subscription" "queue" {
  for_each             = var.consumers
  topic_arn            = aws_sns_topic.events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.main[each.key].arn
  raw_message_delivery = true # the queue receives exactly the event JSON, without the SNS envelope
  filter_policy_scope  = "MessageAttributes"
  filter_policy        = jsonencode({ eventType = each.value.event_types })
}

# --- EventBridge: scheduled / business events that are NOT the transfer stream ---------------
resource "aws_cloudwatch_event_bus" "payflow" {
  name = var.name
}

resource "aws_sqs_queue" "reporting" {
  name                    = "${var.name}-reporting"
  sqs_managed_sse_enabled = true
}

resource "aws_cloudwatch_event_rule" "month_end" {
  name           = "${var.name}-month-end"
  event_bus_name = aws_cloudwatch_event_bus.payflow.name
  event_pattern = jsonencode({
    source        = ["payflow.reporting"]
    "detail-type" = ["MonthEnd"]
  })
}

resource "aws_cloudwatch_event_target" "month_end_to_queue" {
  rule           = aws_cloudwatch_event_rule.month_end.name
  event_bus_name = aws_cloudwatch_event_bus.payflow.name
  target_id      = "reporting-queue"
  arn            = aws_sqs_queue.reporting.arn
}

data "aws_iam_policy_document" "from_events" {
  statement {
    sid       = "AllowRuleToSend"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.reporting.arn]
    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.month_end.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "from_events" {
  queue_url = aws_sqs_queue.reporting.id
  policy    = data.aws_iam_policy_document.from_events.json
}

# Least-privilege policy for the APPLICATION (task role): publish to the topic, consume ITS queues. Nothing else.
data "aws_iam_policy_document" "app" {
  statement {
    sid       = "PublishTransferEvents"
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.events.arn]
  }
  statement {
    sid       = "ConsumeOwnQueues"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:ChangeMessageVisibility", "sqs:GetQueueAttributes", "sqs:GetQueueUrl"]
    resources = [for q in aws_sqs_queue.main : q.arn]
  }
}

output "topic_arn" {
  value = aws_sns_topic.events.arn
}

output "queue_urls" {
  value = { for k, q in aws_sqs_queue.main : k => q.url }
}

output "queue_arns" {
  value = { for k, q in aws_sqs_queue.main : k => q.arn }
}

output "dlq_names" {
  value = { for k, q in aws_sqs_queue.dlq : k => q.name }
}

output "queue_names" {
  value = { for k, q in aws_sqs_queue.main : k => q.name }
}

output "reporting_queue_url" {
  value = aws_sqs_queue.reporting.url
}

output "event_bus_name" {
  value = aws_cloudwatch_event_bus.payflow.name
}

output "app_policy_json" {
  value = data.aws_iam_policy_document.app.json
}
