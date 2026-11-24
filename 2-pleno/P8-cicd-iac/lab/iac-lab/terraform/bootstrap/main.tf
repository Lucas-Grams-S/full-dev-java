# Creates ONLY the bucket that will hold the remote state of the other configurations.
# It is the chicken-and-egg step: this one keeps a LOCAL state (small, rarely changes).
terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "region" {
  type    = string
  default = "us-east-1"
}

variable "emulator_endpoint" {
  type    = string
  default = null
}

locals {
  emulated = var.emulator_endpoint != null
}

provider "aws" {
  region                      = var.region
  access_key                  = local.emulated ? "test" : null
  secret_key                  = local.emulated ? "test" : null
  skip_credentials_validation = local.emulated
  skip_requesting_account_id  = local.emulated
  skip_metadata_api_check     = local.emulated
  s3_use_path_style           = local.emulated

  dynamic "endpoints" {
    for_each = local.emulated ? [1] : []
    content {
      s3  = var.emulator_endpoint
      sts = var.emulator_endpoint
    }
  }
}

data "aws_caller_identity" "current" {}

# trivy:ignore:AWS-0089 Accepted: no access logs for the state bucket of a study account
resource "aws_s3_bucket" "state" {
  bucket = "payflow-tfstate-${data.aws_caller_identity.current.account_id}-${var.region}"

  lifecycle {
    prevent_destroy = true # losing the state = losing the map of what exists
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled" # every version of the state is kept: you can go back
  }
}

# trivy:ignore:AWS-0132 Accepted: AES256; a customer-managed key is a decision for P10
resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

output "state_bucket" {
  value = aws_s3_bucket.state.bucket
}
