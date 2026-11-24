# Write-only: the value is generated and sent to Secrets Manager during the apply and is NOT stored in the state.
ephemeral "random_password" "key" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "key" {
  name                    = "lab-p9/ephemeral-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "key" {
  secret_id                = aws_secretsmanager_secret.key.id
  secret_string_wo         = ephemeral.random_password.key.result
  secret_string_wo_version = 1
}
