# The mock invents a random string for every computed attribute; an IAM policy needs a
# valid JSON, so we give the data source a fixed one. The assertions look at the
# statement's own arguments (the part that is ours), not at the generated JSON.
mock_provider "aws" {
  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }
}

variables {
  name               = "payflow-github-deploy"
  github_repository  = "acme/payflow"
  ecr_repository_arn = "arn:aws:ecr:us-east-1:123456789012:repository/payflow"
  deploy_bucket_arn  = "arn:aws:s3:::payflow-deploy"
}

run "trust_is_pinned_to_one_repo_and_environment" {
  command = plan

  assert {
    condition = contains(
      flatten([for c in data.aws_iam_policy_document.trust.statement[0].condition : c.values]),
      "repo:acme/payflow:environment:production"
    )
    error_message = "the sub claim must name the repository AND the environment"
  }

  assert {
    condition = alltrue([
      for c in data.aws_iam_policy_document.trust.statement[0].condition :
      c.test == "StringEquals"
    ])
    error_message = "StringLike with a wildcard would let any repository of the owner deploy"
  }
}

run "rejects_a_malformed_repository" {
  command = plan

  variables {
    github_repository = "acme"
  }

  expect_failures = [var.github_repository]
}
