variable "region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "payflow"
}

variable "github_repository" {
  description = "owner/repo allowed to deploy (OIDC)"
  type        = string
  default     = "your-user/payflow"
}

variable "emulator_endpoint" {
  description = "Set to http://localhost:5088 to run against moto; null = real AWS"
  type        = string
  default     = null
}

variable "force_destroy" {
  description = "Study environment: allow destroying buckets/repos that still have content"
  type        = bool
  default     = true
}
