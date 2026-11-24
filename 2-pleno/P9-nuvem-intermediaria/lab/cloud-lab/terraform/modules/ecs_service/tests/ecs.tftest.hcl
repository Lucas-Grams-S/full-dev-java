mock_provider "aws" {
  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }
}

variables {
  name              = "t"
  subnet_ids        = ["subnet-1", "subnet-2"]
  security_group_id = "sg-1"
  target_group_arn  = "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/t/abc"
  task_policy_json  = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
  image             = "123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
}

run "a_mutable_tag_is_refused" {
  command = plan
  variables {
    image = "123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow:latest"
  }
  expect_failures = [var.image]
}

run "the_service_rolls_back_a_bad_deploy_and_never_drops_capacity" {
  command = plan
  assert {
    condition     = aws_ecs_service.app.deployment_circuit_breaker[0].enable && aws_ecs_service.app.deployment_circuit_breaker[0].rollback
    error_message = "the deployment circuit breaker with rollback must be on"
  }
  assert {
    condition     = aws_ecs_service.app.deployment_minimum_healthy_percent == 100
    error_message = "a deploy must not go below the current capacity"
  }
  assert {
    condition     = aws_ecs_service.app.network_configuration[0].assign_public_ip == false && aws_ecs_service.app.enable_execute_command == false
    error_message = "no public IP, no ECS Exec by default"
  }
}

run "the_container_is_locked_down" {
  command = plan
  assert {
    condition     = jsondecode(aws_ecs_task_definition.app.container_definitions)[0].readonlyRootFilesystem == true
    error_message = "read-only root file system (writable /tmp comes from the volume)"
  }
  assert {
    condition     = jsondecode(aws_ecs_task_definition.app.container_definitions)[0].user == "10001"
    error_message = "numeric non-root user (J9.02)"
  }
  assert {
    condition     = aws_ecs_task_definition.app.runtime_platform[0].cpu_architecture == "ARM64"
    error_message = "ARM64 platform: the image must be built for arm64"
  }
  assert {
    condition     = aws_iam_role.task.name != aws_iam_role.execution.name
    error_message = "the task role (your code) and the execution role (the ECS agent) are different roles"
  }
}

run "secrets_are_injected_by_reference_never_by_value" {
  command = plan
  variables {
    secrets = { PAYFLOW_SECURITY_JWT_SECRET = "arn:aws:secretsmanager:us-east-1:123456789012:secret:payflow/study/jwt-key-AbCdEf" }
  }
  assert {
    condition     = length([for s in jsondecode(aws_ecs_task_definition.app.container_definitions)[0].secrets : s if startswith(s.valueFrom, "arn:aws:secretsmanager:")]) == 1
    error_message = "the task definition must carry only the ARN of the secret"
  }
}
