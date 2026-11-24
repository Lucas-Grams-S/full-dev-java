mock_provider "aws" {
  mock_resource "aws_sqs_queue" {
    defaults = {
      arn = "arn:aws:sqs:us-east-1:123456789012:mock-queue"
    }
  }
  mock_resource "aws_sns_topic" {
    defaults = {
      arn = "arn:aws:sns:us-east-1:123456789012:mock-topic"
    }
  }
  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }
}

variables {
  name = "t"
}

run "every_consumer_has_a_dlq_with_14_days_and_a_redrive_policy" {
  command = apply
  assert {
    condition     = alltrue([for k, q in aws_sqs_queue.dlq : q.message_retention_seconds == 1209600])
    error_message = "DLQs keep messages for the maximum (14 days): time to investigate"
  }
  assert {
    condition     = alltrue([for k, q in aws_sqs_queue.main : jsondecode(q.redrive_policy).maxReceiveCount == 5])
    error_message = "maxReceiveCount must be 5 for every main queue"
  }
}

run "subscriptions_are_raw_and_filtered" {
  command = apply
  assert {
    condition     = alltrue([for k, s in aws_sns_topic_subscription.queue : s.raw_message_delivery && s.filter_policy_scope == "MessageAttributes"])
    error_message = "raw delivery (no SNS envelope) and attribute filtering"
  }
}

run "a_zero_max_receive_count_is_refused" {
  command = plan
  variables {
    max_receive_count = 0
  }
  expect_failures = [var.max_receive_count]
}

run "a_visibility_timeout_over_12_hours_is_refused" {
  command = plan
  variables {
    visibility_timeout = 50000
  }
  expect_failures = [var.visibility_timeout]
}
