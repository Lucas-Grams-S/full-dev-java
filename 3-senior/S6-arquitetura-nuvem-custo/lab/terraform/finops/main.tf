# Custos do PayFlow por conta: tags padrão, ativação das tags de custo, orçamento e detecção de anomalia.
# 💸 Budgets e Cost Anomaly Detection: confira a página de preços antes de aplicar. Desfazer: terraform destroy.
terraform {
  required_version = ">= 1.11"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.73" }
  }
}

variable "region" { type = string }
variable "env" {
  type = string
  validation {
    condition     = contains(["prod", "staging", "study", "shared"], var.env)
    error_message = "env deve ser prod, staging, study ou shared (a mesma lista da política de tags)."
  }
}
variable "owner" { type = string }
variable "cost_center" {
  type = string
  validation {
    condition     = can(regex("^CC-[0-9]{4}$", var.cost_center))
    error_message = "cost_center no formato CC-NNNN."
  }
}
variable "monthly_budget_usd" { type = number }
variable "alert_email" { type = string }
variable "anomaly_min_impact_usd" {
  type    = number
  default = 50
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      app           = "payflow"
      env           = var.env
      owner         = var.owner
      "cost-center" = var.cost_center
    }
  }
}

# A tag só aparece no Cost Explorer depois de ativada (até 24 h para aparecer e mais até 24 h para ativar).
resource "aws_ce_cost_allocation_tag" "keys" {
  for_each = toset(["app", "env", "owner", "cost-center"])
  tag_key  = each.value
  status   = "Active"
}

resource "aws_budgets_budget" "monthly" {
  name         = "payflow-${var.env}-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.monthly_budget_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # Previsão: avisa antes de gastar. Real: confirma que gastou.
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alert_email]
  }
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }
}

# Um monitor por serviço: no estudo de caso o total da conta NÃO viu o salto de logs de 10/10 (out-s606-detect.txt).
resource "aws_ce_anomaly_monitor" "by_service" {
  name              = "payflow-${var.env}-by-service"
  monitor_type      = "DIMENSIONAL"
  monitor_dimension = "SERVICE"
}

resource "aws_ce_anomaly_subscription" "daily" {
  name             = "payflow-${var.env}-anomalies"
  frequency        = "DAILY"
  monitor_arn_list = [aws_ce_anomaly_monitor.by_service.arn]

  subscriber {
    type    = "EMAIL"
    address = var.alert_email
  }

  threshold_expression {
    dimension {
      key           = "ANOMALY_TOTAL_IMPACT_ABSOLUTE"
      match_options = ["GREATER_THAN_OR_EQUAL"]
      values        = [tostring(var.anomaly_min_impact_usd)]
    }
  }
}
