terraform {
  required_version = ">= 1.14"
  required_providers {
    aws    = { source = "hashicorp/aws", version = "~> 6.0" }
    random = { source = "hashicorp/random", version = "~> 3.7" }
  }
}

variable "name" {
  type = string
}

variable "environment" {
  type = string
}

variable "recovery_window_days" {
  description = "Days a deleted secret can still be restored (0 = delete now; study only)"
  type        = number
  default     = 7
}

# EPHEMERAL: exists only during the plan/apply. It is NOT written to the plan file or to the state.
ephemeral "random_password" "jwt_signing_key" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.name}/${var.environment}/jwt-signing-key"
  description             = "HMAC key that signs the PayFlow access tokens (rotated by replacing the version)"
  recovery_window_in_days = var.recovery_window_days
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id                = aws_secretsmanager_secret.jwt.id
  secret_string_wo         = ephemeral.random_password.jwt_signing_key.result # WRITE-ONLY: sent to AWS, never stored in the state
  secret_string_wo_version = 1                                                # bump to 2 to ROTATE (write-only values have no diff to detect)
}

# Non-secret configuration: Parameter Store (standard tier has no extra charge).
resource "aws_ssm_parameter" "outbox_batch_size" {
  name  = "/${var.name}/${var.environment}/outbox/batch-size"
  type  = "String"
  value = "50"
}

resource "aws_ssm_parameter" "feature_webhook" {
  name  = "/${var.name}/${var.environment}/features/webhook-dispatch"
  type  = "String"
  value = "false"
}

output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt.arn
}

output "parameter_prefix" {
  value = "/${var.name}/${var.environment}"
}
