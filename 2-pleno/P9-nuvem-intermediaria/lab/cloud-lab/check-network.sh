#!/usr/bin/env bash
# Verifies the REAL (emulated) network, not the Terraform code: nobody may add a route from a private subnet to the internet gateway.
# exit 0 = ok, exit 1 = violation. Needs the AWS CLI and AWS_ENDPOINT_URL for the emulator.
set -Eeuo pipefail
VPC="${1:?usage: check-network.sh <vpc-id>}"
bad=0
igw="$(aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=$VPC" --query 'InternetGateways[0].InternetGatewayId' --output text)"
for tier in app data; do
  for subnet in $(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC" "Name=tag:tier,Values=$tier" --query 'Subnets[].SubnetId' --output text); do
    rt="$(aws ec2 describe-route-tables --filters "Name=association.subnet-id,Values=$subnet" --query 'RouteTables[0].RouteTableId' --output text)"
    to_igw="$(aws ec2 describe-route-tables --route-table-ids "$rt" --query "RouteTables[0].Routes[?GatewayId=='$igw'] | length(@)" --output text)"
    if [ "$to_igw" != "0" ]; then echo "VIOLATION: $tier subnet $subnet (route table $rt) has a route to the internet gateway $igw"; bad=1; fi
  done
done
[ "$bad" = 0 ] && echo "ok: no app/data subnet routes to the internet gateway ($igw)"
exit "$bad"
