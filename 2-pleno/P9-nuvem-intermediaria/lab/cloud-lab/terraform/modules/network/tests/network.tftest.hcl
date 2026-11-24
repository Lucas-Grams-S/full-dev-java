mock_provider "aws" {}

variables {
  name = "t"
  azs  = ["us-east-1a", "us-east-1b"]
}

run "no_nat_means_private_subnets_have_no_default_route" {
  command = plan
  assert {
    condition     = length(aws_route.app_nat) == 0 && length(aws_nat_gateway.this) == 0
    error_message = "with nat_gateways = 0 there must be no NAT and no default route in the app route tables"
  }
  assert {
    condition     = length([for s in aws_subnet.app : s if s.map_public_ip_on_launch == true]) == 0
    error_message = "private subnets must not hand out public IPs"
  }
}

run "one_nat_is_shared_by_both_app_route_tables" {
  command = plan
  variables {
    nat_gateways = 1
  }
  assert {
    condition     = length(aws_nat_gateway.this) == 1 && length(aws_route.app_nat) == 2
    error_message = "1 NAT, 2 default routes (one per app route table)"
  }
}

run "cidr_plan_is_stable" {
  command = plan
  assert {
    condition = (
      aws_subnet.public[0].cidr_block == "10.20.0.0/24" &&
      aws_subnet.app[0].cidr_block == "10.20.10.0/24" &&
      aws_subnet.app[1].cidr_block == "10.20.11.0/24" &&
      aws_subnet.data[1].cidr_block == "10.20.21.0/24"
    )
    error_message = "the documented CIDR plan changed (P9.01): renumbering a subnet recreates everything inside it"
  }
}

run "the_database_is_reachable_only_from_the_application" {
  command = plan
  assert {
    condition     = aws_vpc_security_group_ingress_rule.db_from_app.from_port == 5432 && aws_vpc_security_group_ingress_rule.db_from_app.cidr_ipv4 == null
    error_message = "the database rule must be a security-group reference, never a CIDR"
  }
}

run "three_azs_are_refused" {
  command = plan
  variables {
    azs = ["a", "b", "c"]
  }
  expect_failures = [var.azs]
}

run "three_nats_are_refused" {
  command = plan
  variables {
    nat_gateways = 3
  }
  expect_failures = [var.nat_gateways]
}
