variable "region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "payflow"
}

variable "environment" {
  type    = string
  default = "study"
}

variable "emulated" {
  description = "true = talk to the emulator (moto) pointed to by AWS_ENDPOINT_URL; false = real AWS"
  type        = bool
  default     = false
}

variable "image" {
  description = "Image by digest. Placeholder: the real one comes from the release workflow (P8.02/P8.03)"
  type        = string
  default     = "000000000000.dkr.ecr.us-east-1.amazonaws.com/payflow@sha256:887615a1d994e9df2c68812c77b4cc394b6c5356babfa5c9d1cc671662881897"
}

variable "nat_gateways" {
  type    = number
  default = 0
}

variable "endpoint_az_count" {
  type    = number
  default = 1
}

variable "multi_az" {
  type    = bool
  default = false
}

variable "create_replica" {
  type    = bool
  default = true
}

variable "production" {
  type    = bool
  default = false
}

variable "enable_eks" {
  description = "💸 an EKS control plane is billed per hour while it exists (P9.03 only shows the shape)"
  type        = bool
  default     = false
}

variable "enable_lambda" {
  description = "The scheduled exporter (P9.05). Needs lambda_zip"
  type        = bool
  default     = false
}

variable "lambda_zip" {
  type    = string
  default = "../../../../lambda-lab/build/exporter.zip"
}
