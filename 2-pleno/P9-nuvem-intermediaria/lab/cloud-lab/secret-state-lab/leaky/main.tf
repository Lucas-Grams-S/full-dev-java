# The "obvious" way: a regular resource. The value is stored in the state, in clear text.
resource "random_password" "key" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "key" {
  name                    = "lab-p9/leaky-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "key" {
  secret_id     = aws_secretsmanager_secret.key.id
  secret_string = random_password.key.result
}
