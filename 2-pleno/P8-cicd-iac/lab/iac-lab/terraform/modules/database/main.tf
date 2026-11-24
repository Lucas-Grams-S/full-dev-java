terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "client_security_group_id" {
  description = "Only this security group may reach the database"
  type        = string
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "backup_days" {
  type    = number
  default = 7
}

variable "allocated_storage" {
  type    = number
  default = 20
}

resource "aws_security_group" "db" {
  name        = "${var.name}-rds-sg"
  description = "PostgreSQL only from the application"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "from_app" {
  description                  = "PostgreSQL from the application security group only"
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = var.client_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db-subnets"
  subnet_ids = var.subnet_ids
}

# trivy:ignore:AWS-0176 Accepted: the app uses the Secrets Manager password; IAM authentication is a P10 topic
# trivy:ignore:AWS-0177 Accepted ONLY in the study environment (we must be able to destroy it); production: true
# trivy:ignore:AWS-0133 Accepted: Performance Insights is not offered for db.t4g.micro
resource "aws_db_instance" "this" {
  identifier                  = "${var.name}-db"
  engine                      = "postgres"
  engine_version              = "18"
  instance_class              = var.instance_class
  allocated_storage           = var.allocated_storage
  db_name                     = "payflow"
  username                    = "payflow_admin"
  manage_master_user_password = true # the password lives in Secrets Manager, not in the code
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false
  storage_encrypted           = true
  multi_az                    = false # 💸 study environment; production: true (P9)
  backup_retention_period     = var.backup_days
  skip_final_snapshot         = true  # study environment only
  deletion_protection         = false # study environment only
}

output "endpoint" {
  value = aws_db_instance.this.address
}

output "master_secret_arn" {
  value = aws_db_instance.this.master_user_secret[0].secret_arn
}
