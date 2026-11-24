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

variable "subnet_id" {
  type = string
}

variable "ami_id" {
  description = "AMI id (resolved by the caller; see envs/study)"
  type        = string
}

variable "instance_type" {
  type    = string
  default = "t4g.small"
}

variable "ecr_repository_arn" {
  type = string
}

variable "user_data" {
  description = "Boot script of the instance (Docker + SSM agent: the user-data.sh of J10.06). null = none"
  type        = string
  default     = null
}

resource "aws_security_group" "app" {
  name        = "${var.name}-ec2-sg"
  description = "PayFlow application host: no inbound from the internet (access by SSM)"
  vpc_id      = var.vpc_id
}

# trivy:ignore:AWS-0104 Accepted: no NAT or VPC endpoints yet, the host reaches ECR and SSM over the internet (P9 restricts it)
resource "aws_vpc_security_group_egress_rule" "all_out" {
  description       = "Outbound to ECR, SSM and package mirrors (see the ignore above)"
  security_group_id = aws_security_group.app.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

data "aws_iam_policy_document" "ec2_trust" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${var.name}-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_trust.json
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "ecr_pull" {
  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
    resources = [var.ecr_repository_arn]
  }
}

resource "aws_iam_role_policy" "ecr_pull" {
  name   = "${var.name}-ecr-pull"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ecr_pull.json
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.name}-ec2-profile"
  role = aws_iam_role.ec2.name
}

resource "aws_instance" "app" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  user_data                   = var.user_data
  user_data_replace_on_change = true # a changed boot script means a NEW instance (nothing manual on the host)

  metadata_options {
    http_tokens = "required" # IMDSv2 only
  }

  root_block_device {
    encrypted   = true
    volume_size = 20
  }

  tags = {
    Name = "${var.name}-app"
  }
}

output "security_group_id" {
  value = aws_security_group.app.id
}

output "instance_id" {
  value = aws_instance.app.id
}
