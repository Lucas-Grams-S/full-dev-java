# Plan-level tests with a MOCK provider: no cloud, no credentials, runs in seconds.
mock_provider "aws" {}

variables {
  name = "payflow"
}

run "tags_are_immutable_and_scanned" {
  command = plan

  assert {
    condition     = aws_ecr_repository.this.image_tag_mutability == "IMMUTABLE"
    error_message = "tags must be immutable (P7.01: a tag never changes)"
  }

  assert {
    condition     = aws_ecr_repository.this.image_scanning_configuration[0].scan_on_push
    error_message = "scan on push must be on"
  }
}

run "lifecycle_keeps_requested_history" {
  command = plan

  variables {
    keep_images = 30
  }

  assert {
    condition     = jsondecode(aws_ecr_lifecycle_policy.keep_last.policy).rules[0].selection.countNumber == 30
    error_message = "the lifecycle policy must follow keep_images"
  }
}

run "rejects_too_little_history" {
  command = plan

  variables {
    keep_images = 2
  }

  expect_failures = [var.keep_images]
}
