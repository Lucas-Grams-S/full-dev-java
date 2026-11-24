terraform {
  required_version = ">= 1.14"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

variable "name" {
  type = string
}

variable "cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "azs" {
  description = "Two Availability Zones (a failure of ONE data center must not take the service down)"
  type        = list(string)
  validation {
    condition     = length(var.azs) == 2
    error_message = "Use exactly 2 AZs in this module (the PayFlow design: ALB, tasks and database in 2 AZs)."
  }
}

variable "nat_gateways" {
  description = "0 = no internet path for the private subnets (use VPC endpoints); 1 = one shared NAT; 2 = one per AZ. NAT 💸 is billed per hour AND per GB."
  type        = number
  default     = 0
  validation {
    condition     = var.nat_gateways >= 0 && var.nat_gateways <= 2
    error_message = "nat_gateways must be 0, 1 or 2."
  }
}

variable "interface_endpoints" {
  description = "Interface VPC endpoints 💸 (billed per hour per AZ). Needed when there is no NAT."
  type        = list(string)
  default     = ["ecr.api", "ecr.dkr", "logs", "secretsmanager", "sqs", "sns"]
}

variable "endpoint_az_count" {
  description = "In how many AZs the interface endpoints get a network interface (1 = cheaper, not highly available)"
  type        = number
  default     = 2
}

data "aws_region" "current" {}

locals {
  public_cidrs  = [for i, _ in var.azs : cidrsubnet(var.cidr, 8, i)]      # 10.20.0.0/24, 10.20.1.0/24
  app_cidrs     = [for i, _ in var.azs : cidrsubnet(var.cidr, 8, 10 + i)] # 10.20.10.0/24, 10.20.11.0/24
  data_cidrs    = [for i, _ in var.azs : cidrsubnet(var.cidr, 8, 20 + i)] # 10.20.20.0/24, 10.20.21.0/24
  region        = data.aws_region.current.region
  endpoint_subs = slice(aws_subnet.app[*].id, 0, var.endpoint_az_count)
}

# trivy:ignore:AWS-0178 Accepted ONLY in the study environment: flow logs cost money (CloudWatch ingestion). Production: enable them (exercise of P9.01)
resource "aws_vpc" "this" {
  cidr_block           = var.cidr
  enable_dns_support   = true
  enable_dns_hostnames = true # required for the private DNS name of interface endpoints
  tags                 = { Name = "${var.name}-vpc" }
}

# The default security group of the VPC allows nothing: nobody should use it by accident.
resource "aws_default_security_group" "default" {
  vpc_id = aws_vpc.this.id
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-igw" }
}

resource "aws_subnet" "public" {
  count                   = length(var.azs)
  vpc_id                  = aws_vpc.this.id
  availability_zone       = var.azs[count.index]
  cidr_block              = local.public_cidrs[count.index]
  map_public_ip_on_launch = false # only load balancers and NAT live here; they get their own addresses
  tags                    = { Name = "${var.name}-public-${var.azs[count.index]}", tier = "public" }
}

resource "aws_subnet" "app" {
  count             = length(var.azs)
  vpc_id            = aws_vpc.this.id
  availability_zone = var.azs[count.index]
  cidr_block        = local.app_cidrs[count.index]
  tags              = { Name = "${var.name}-app-${var.azs[count.index]}", tier = "app" }
}

resource "aws_subnet" "data" {
  count             = length(var.azs)
  vpc_id            = aws_vpc.this.id
  availability_zone = var.azs[count.index]
  cidr_block        = local.data_cidrs[count.index]
  tags              = { Name = "${var.name}-data-${var.azs[count.index]}", tier = "data" }
}

# --- routes ---------------------------------------------------------------------------------
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-public" }
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this.id
}

resource "aws_route_table_association" "public" {
  count          = length(var.azs)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat" {
  count  = var.nat_gateways
  domain = "vpc"
  tags   = { Name = "${var.name}-nat-${count.index}" }
}

resource "aws_nat_gateway" "this" {
  count         = var.nat_gateways
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id # a NAT lives in a PUBLIC subnet
  tags          = { Name = "${var.name}-nat-${count.index}" }
  depends_on    = [aws_internet_gateway.this]
}

resource "aws_route_table" "app" {
  count  = length(var.azs)
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-app-${var.azs[count.index]}" }
}

# The ONLY default route of a private subnet is a NAT (never the internet gateway), and only if you asked for one.
resource "aws_route" "app_nat" {
  count                  = var.nat_gateways > 0 ? length(var.azs) : 0
  route_table_id         = aws_route_table.app[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.this[min(count.index, var.nat_gateways - 1)].id
}

resource "aws_route_table_association" "app" {
  count          = length(var.azs)
  subnet_id      = aws_subnet.app[count.index].id
  route_table_id = aws_route_table.app[count.index].id
}

# Data subnets: NO default route at all (only the local VPC route exists).
resource "aws_route_table" "data" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-data" }
}

resource "aws_route_table_association" "data" {
  count          = length(var.azs)
  subnet_id      = aws_subnet.data[count.index].id
  route_table_id = aws_route_table.data.id
}

# --- security groups (rules are separate resources: no dependency cycles) -------------------
resource "aws_security_group" "alb" {
  name        = "${var.name}-alb-sg"
  description = "Public load balancer"
  vpc_id      = aws_vpc.this.id
}

resource "aws_security_group" "app" {
  name        = "${var.name}-app-sg"
  description = "PayFlow tasks (private subnets)"
  vpc_id      = aws_vpc.this.id
}

resource "aws_security_group" "db" {
  name        = "${var.name}-db-sg"
  description = "PostgreSQL (data subnets)"
  vpc_id      = aws_vpc.this.id
}

resource "aws_security_group" "endpoints" {
  name        = "${var.name}-endpoints-sg"
  description = "Interface VPC endpoints"
  vpc_id      = aws_vpc.this.id
}

# trivy:ignore:AWS-0107 Accepted: a public load balancer MUST accept the internet on 80/443 (it is the only public door)
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  description       = "HTTP from the internet (study: no certificate yet; production redirects to 443)"
  security_group_id = aws_security_group.alb.id
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_app_http" {
  description                  = "Traffic to the tasks (application port)"
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
}

resource "aws_vpc_security_group_egress_rule" "alb_to_app_mgmt" {
  description                  = "Health checks only (management port 8081)"
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 8081
  to_port                      = 8081
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_http" {
  description                  = "Application port from the load balancer only"
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_mgmt" {
  description                  = "Health check (8081) from the load balancer only"
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = 8081
  to_port                      = 8081
}

# Egress of the tasks: ONLY the interface endpoints, S3 (gateway endpoint prefix list) and the database.
resource "aws_vpc_security_group_egress_rule" "app_to_endpoints" {
  description                  = "AWS APIs through the interface endpoints (443)"
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.endpoints.id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}

resource "aws_vpc_security_group_egress_rule" "app_to_s3" {
  description       = "S3 through the gateway endpoint (ECR image layers live in S3)"
  security_group_id = aws_security_group.app.id
  prefix_list_id    = aws_vpc_endpoint.s3.prefix_list_id
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "app_to_db" {
  description                  = "PostgreSQL"
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.db.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  description                  = "PostgreSQL from the application security group only"
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_vpc_security_group_ingress_rule" "endpoints_from_app" {
  description                  = "HTTPS from the tasks"
  security_group_id            = aws_security_group.endpoints.id
  referenced_security_group_id = aws_security_group.app.id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}

# --- VPC endpoints --------------------------------------------------------------------------
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${local.region}.s3"
  vpc_endpoint_type = "Gateway" # free: a route-table entry, no network interface
  route_table_ids   = concat(aws_route_table.app[*].id, [aws_route_table.data.id])
  tags              = { Name = "${var.name}-s3" }
}

resource "aws_vpc_endpoint" "interface" {
  for_each            = toset(var.interface_endpoints)
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${local.region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = local.endpoint_subs
  security_group_ids  = [aws_security_group.endpoints.id]
  private_dns_enabled = true # the SDK keeps using the normal name (sqs.us-east-1.amazonaws.com) and lands on the endpoint
  tags                = { Name = "${var.name}-${each.key}" }
}

output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "app_subnet_ids" {
  value = aws_subnet.app[*].id
}

output "data_subnet_ids" {
  value = aws_subnet.data[*].id
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "app_security_group_id" {
  value = aws_security_group.app.id
}

output "db_security_group_id" {
  value = aws_security_group.db.id
}

output "app_route_table_ids" {
  value = aws_route_table.app[*].id
}
