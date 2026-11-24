mock_provider "aws" {}

variables {
  name                    = "t"
  cluster_name            = "c"
  service_name            = "s"
  log_group_name          = "/ecs/t"
  lb_arn_suffix           = "app/t/abc"
  target_group_arn_suffix = "targetgroup/t/abc"
  dlq_names               = { notifications = "t-notifications-dlq", antifraud = "t-antifraud-dlq" }
  queue_names             = { notifications = "t-notifications", antifraud = "t-antifraud" }
  db_identifier           = "t-db"
}

run "every_alarm_has_severity_runbook_and_an_action" {
  command = plan
  assert {
    condition = alltrue([for k, a in aws_cloudwatch_metric_alarm.simple :
    can(regex("^\\[(page|ticket)\\] .+ \\| runbook: https://.+\\.md$", a.alarm_description))])
    error_message = "every alarm needs [page|ticket] + summary + runbook (decision 141)"
  }
  assert {
    condition     = alltrue([for k, a in aws_cloudwatch_metric_alarm.simple : length(a.alarm_actions) == 1])
    error_message = "an alarm without an action is a dashboard nobody looks at"
  }
}

run "every_dead_letter_queue_has_its_alarm" {
  command = plan
  assert {
    condition     = contains(keys(aws_cloudwatch_metric_alarm.simple), "dlq-notifications-not-empty") && contains(keys(aws_cloudwatch_metric_alarm.simple), "dlq-antifraud-not-empty")
    error_message = "a DLQ with no alarm fills up silently"
  }
}

run "the_replica_alarm_exists_only_when_there_is_a_replica" {
  command = plan
  assert {
    condition     = !contains(keys(aws_cloudwatch_metric_alarm.simple), "rds-replica-lag")
    error_message = "no replica, no replica-lag alarm"
  }
}

run "with_a_replica_the_lag_alarm_appears" {
  command = plan
  variables {
    db_replica_identifier = "t-db-replica"
  }
  assert {
    condition     = contains(keys(aws_cloudwatch_metric_alarm.simple), "rds-replica-lag")
    error_message = "a replica needs a lag alarm"
  }
}
