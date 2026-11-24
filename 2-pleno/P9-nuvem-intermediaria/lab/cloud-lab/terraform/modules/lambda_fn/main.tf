terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "zip_path" {
  description = "Deployment package (a JAR/zip). Built by CI, never by hand"
  type        = string
}

variable "handler" {
  type    = string
  default = "com.payflow.exporter.StatementExporter::handleRequest"
}

variable "memory_mb" {
  description = "Lambda gives CPU in proportion to memory: more memory also means a faster cold start"
  type        = number
  default     = 1024
}

variable "schedule" {
  type    = string
  default = "cron(0 3 1 * ? *)" # 03:00 UTC on day 1 of each month
}

variable "snap_start" {
  description = "SnapStart: restore from a snapshot taken after init. Only for published versions (alias), arm64 or x86"
  type        = bool
  default     = true
}

data "aws_iam_policy_document" "lambda_trust" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "fn" {
  name               = "${var.name}-exporter"
  assume_role_policy = data.aws_iam_policy_document.lambda_trust.json
}

resource "aws_iam_role_policy_attachment" "logs" {
  role       = aws_iam_role.fn.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "exporter" {
  function_name    = "${var.name}-statement-exporter"
  role             = aws_iam_role.fn.arn
  runtime          = "java25"
  handler          = var.handler
  architectures    = ["arm64"]
  memory_size      = var.memory_mb
  timeout          = 60
  filename         = var.zip_path
  source_code_hash = filebase64sha256(var.zip_path)
  publish          = true # SnapStart works on PUBLISHED versions only, never on $LATEST

  dynamic "snap_start" {
    for_each = var.snap_start ? [1] : []
    content {
      apply_on = "PublishedVersions"
    }
  }
}

resource "aws_lambda_alias" "live" {
  name             = "live"
  function_name    = aws_lambda_function.exporter.function_name
  function_version = aws_lambda_function.exporter.version # the schedule calls the ALIAS, i.e. a snapshot-backed version
}

resource "aws_cloudwatch_event_rule" "schedule" {
  name                = "${var.name}-statement-exporter"
  schedule_expression = var.schedule
}

resource "aws_cloudwatch_event_target" "fn" {
  rule      = aws_cloudwatch_event_rule.schedule.name
  target_id = "exporter"
  arn       = aws_lambda_alias.live.arn
}

resource "aws_lambda_permission" "events" {
  statement_id  = "AllowSchedule"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.exporter.function_name
  qualifier     = aws_lambda_alias.live.name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.schedule.arn
}

output "alias_arn" {
  value = aws_lambda_alias.live.arn
}

output "function_name" {
  value = aws_lambda_function.exporter.function_name
}
