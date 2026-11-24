terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "github_repository" {
  description = "owner/repo allowed to assume the role"
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$", var.github_repository))
    error_message = "github_repository must look like owner/repo."
  }
}

variable "github_environment" {
  description = "GitHub Environment allowed to assume the role (the 'sub' claim)"
  type        = string
  default     = "production"
}

variable "ecr_repository_arn" {
  type = string
}

variable "deploy_bucket_arn" {
  type = string
}

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "trust" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:environment:${var.github_environment}"]
    }
  }
}

data "aws_iam_policy_document" "permissions" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"] # this action does not accept a resource
  }
  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability", "ecr:InitiateLayerUpload", "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload", "ecr:PutImage", "ecr:BatchGetImage",
    ]
    resources = [var.ecr_repository_arn]
  }
  statement {
    sid       = "DeployBundle"
    actions   = ["s3:PutObject", "s3:GetObject"]
    resources = ["${var.deploy_bucket_arn}/deploy/*"]
  }
}

resource "aws_iam_role" "deploy" {
  name               = var.name
  assume_role_policy = data.aws_iam_policy_document.trust.json
}

resource "aws_iam_role_policy" "deploy" {
  name   = "${var.name}-permissions"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.permissions.json
}

output "role_arn" {
  value = aws_iam_role.deploy.arn
}
