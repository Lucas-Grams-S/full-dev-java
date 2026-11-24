terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "subnet_ids" {
  description = "DATA subnets (no route to the internet), at least 2 AZs"
  type        = list(string)
}

variable "security_group_id" {
  description = "Created by the network module: ingress 5432 only from the application"
  type        = string
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "multi_az" {
  description = "Standby in another AZ with synchronous replication (high availability, NOT read scaling). Production: true 💸"
  type        = bool
  default     = false
}

variable "create_replica" {
  description = "Read replica (asynchronous) for reporting reads 💸"
  type        = bool
  default     = false
}

variable "production" {
  description = "true = deletion protection + final snapshot; false = study environment (must be destroyable)"
  type        = bool
  default     = false
}

variable "backup_days" {
  type    = number
  default = 7
}

variable "allocated_storage" {
  type    = number
  default = 20
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db-subnets"
  subnet_ids = var.subnet_ids
}

resource "aws_db_parameter_group" "pg18" {
  name   = "${var.name}-pg18"
  family = "postgres18"

  parameter {
    name  = "log_min_duration_statement"
    value = "500" # log statements slower than 500 ms (P2.04)
  }
}

# trivy:ignore:AWS-0176 Accepted: the app uses the Secrets Manager password; IAM authentication is a P10 topic
# trivy:ignore:AWS-0177 Accepted ONLY while var.production = false (the study environment must be destroyable)
resource "aws_db_instance" "this" {
  identifier                  = "${var.name}-db"
  engine                      = "postgres"
  engine_version              = "18"
  instance_class              = var.instance_class
  allocated_storage           = var.allocated_storage
  db_name                     = "payflow"
  username                    = "payflow_admin"
  manage_master_user_password = true # RDS creates the secret in Secrets Manager and rotates it (every 7 days by default)
  parameter_group_name        = aws_db_parameter_group.pg18.name
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [var.security_group_id]
  publicly_accessible         = false
  storage_encrypted           = true
  multi_az                    = var.multi_az
  backup_retention_period     = var.backup_days # also required for read replicas of this instance
  deletion_protection         = var.production
  skip_final_snapshot         = !var.production
  final_snapshot_identifier   = var.production ? "${var.name}-db-final" : null
  apply_immediately           = !var.production
}

# trivy:ignore:AWS-0176 Accepted: same reason as the primary
# trivy:ignore:AWS-0177 Accepted ONLY while var.production = false
resource "aws_db_instance" "replica" {
  count                  = var.create_replica ? 1 : 0
  identifier             = "${var.name}-db-replica"
  replicate_source_db    = aws_db_instance.this.identifier
  instance_class         = var.instance_class
  parameter_group_name   = aws_db_parameter_group.pg18.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  storage_encrypted      = true
  skip_final_snapshot    = true # a replica holds no data of its own: the primary has the backups
}

output "identifier" {
  value = aws_db_instance.this.identifier
}

output "replica_identifier" {
  value = var.create_replica ? aws_db_instance.replica[0].identifier : null
}

output "endpoint" {
  value = aws_db_instance.this.address
}

output "master_secret_arn" {
  description = "Secret that RDS itself creates and rotates"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
