locals {
  emulated = var.emulator_endpoint != null
}

provider "aws" {
  region = var.region

  # Only when talking to the emulator: it has no real credentials or account checks.
  access_key                  = local.emulated ? "test" : null
  secret_key                  = local.emulated ? "test" : null
  skip_credentials_validation = local.emulated
  skip_requesting_account_id  = local.emulated
  skip_metadata_api_check     = local.emulated
  s3_use_path_style           = local.emulated

  dynamic "endpoints" {
    for_each = local.emulated ? [1] : []
    content {
      ec2            = var.emulator_endpoint
      ecr            = var.emulator_endpoint
      iam            = var.emulator_endpoint
      rds            = var.emulator_endpoint
      s3             = var.emulator_endpoint
      ssm            = var.emulator_endpoint
      sts            = var.emulator_endpoint
      secretsmanager = var.emulator_endpoint
    }
  }

  default_tags {
    tags = {
      project   = var.project
      managedby = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# AMI: Amazon Linux 2023 (arm64) published by AWS as a public SSM parameter
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

# trivy:ignore:AWS-0089 Accepted: access logs of a study bucket are not worth a second bucket
# trivy:ignore:AWS-0132 Accepted: AES256 (the default); a customer-managed key costs US$ 1 per month (P10)
resource "aws_s3_bucket" "deploy" {
  bucket        = "${var.project}-deploy-${data.aws_caller_identity.current.account_id}-${var.region}"
  force_destroy = var.force_destroy
}

resource "aws_s3_bucket_versioning" "deploy" {
  bucket = aws_s3_bucket.deploy.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "deploy" {
  bucket                  = aws_s3_bucket.deploy.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

module "registry" {
  source       = "../../modules/registry"
  name         = var.project
  force_delete = var.force_destroy
}

module "app_host" {
  source             = "../../modules/app_host"
  name               = var.project
  vpc_id             = data.aws_vpc.default.id
  subnet_id          = sort(data.aws_subnets.default.ids)[0]
  ami_id             = nonsensitive(data.aws_ssm_parameter.al2023.value)
  ecr_repository_arn = module.registry.repository_arn
}

module "database" {
  source                   = "../../modules/database"
  name                     = var.project
  vpc_id                   = data.aws_vpc.default.id
  subnet_ids               = data.aws_subnets.default.ids
  client_security_group_id = module.app_host.security_group_id
}

module "github_oidc" {
  source             = "../../modules/github_oidc"
  name               = "${var.project}-github-deploy"
  github_repository  = var.github_repository
  ecr_repository_arn = module.registry.repository_arn
  deploy_bucket_arn  = aws_s3_bucket.deploy.arn
}

resource "aws_ssm_parameter" "db_endpoint" {
  name  = "/${var.project}/db/endpoint"
  type  = "String"
  value = module.database.endpoint
}
