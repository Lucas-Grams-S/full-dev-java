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

variable "public_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "certificate_arn" {
  description = "ACM certificate. null = HTTP only (study, no domain). Production: set it; port 80 then only redirects."
  type        = string
  default     = null
}

variable "deletion_protection" {
  type    = bool
  default = false
}

# trivy:ignore:AWS-0053 Accepted: this internet-facing load balancer IS the single public entry point (by design)
resource "aws_lb" "this" {
  name                       = "${var.name}-alb"
  load_balancer_type         = "application"
  internal                   = false
  subnets                    = var.public_subnet_ids
  security_groups            = [var.security_group_id]
  drop_invalid_header_fields = true
  enable_deletion_protection = var.deletion_protection
  idle_timeout               = 60
}

# The target group points at TASK IP addresses (awsvpc network mode: every Fargate task has its own network interface).
resource "aws_lb_target_group" "app" {
  name                 = "${var.name}-tg"
  vpc_id               = var.vpc_id
  target_type          = "ip"
  protocol             = "HTTP"
  port                 = 8080
  deregistration_delay = 30 # default is 300 s: a deploy would wait 5 min for each old task (shutdown phase is 20 s, decision 73)

  health_check {
    protocol            = "HTTP"
    port                = "8081" # the management port (decision 138); the SG lets ONLY the ALB in
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# trivy:ignore:AWS-0054 Accepted ONLY in the study environment (no domain, no certificate); with certificate_arn set this rule is a redirect
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = var.certificate_arn == null ? "forward" : "redirect"
    target_group_arn = var.certificate_arn == null ? aws_lb_target_group.app.arn : null

    dynamic "redirect" {
      for_each = var.certificate_arn == null ? [] : [1]
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  }
}

resource "aws_lb_listener" "https" {
  count             = var.certificate_arn == null ? 0 : 1
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "target_group_arn_suffix" {
  value = aws_lb_target_group.app.arn_suffix
}

output "lb_arn_suffix" {
  value = aws_lb.this.arn_suffix
}

output "dns_name" {
  value = aws_lb.this.dns_name
}
