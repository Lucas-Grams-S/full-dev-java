terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  description = "ECR repository name"
  type        = string
}

variable "keep_images" {
  description = "How many images the lifecycle policy keeps"
  type        = number
  default     = 20

  validation {
    condition     = var.keep_images >= 5
    error_message = "keep_images must be at least 5 (a rollback needs history)."
  }
}

variable "force_delete" {
  description = "Delete the repository even if it still has images (study environments only)"
  type        = bool
  default     = false
}

# trivy:ignore:AWS-0033 Accepted: AES256 (the default); a customer-managed key is a decision for P10
resource "aws_ecr_repository" "this" {
  name                 = var.name
  image_tag_mutability = "IMMUTABLE" # the P7.01 rule: a tag never changes
  force_delete         = var.force_delete

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "keep_last" {
  repository = aws_ecr_repository.this.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "keep the last ${var.keep_images} images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.keep_images
      }
      action = { type = "expire" }
    }]
  })
}

output "repository_url" {
  value = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  value = aws_ecr_repository.this.arn
}
