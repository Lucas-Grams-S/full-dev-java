output "ecr_repository_url" {
  value = module.registry.repository_url
}

output "deploy_role_arn" {
  description = "Goes into the GitHub variable AWS_ROLE_ARN"
  value       = module.github_oidc.role_arn
}

output "instance_id" {
  value = module.app_host.instance_id
}

output "db_endpoint" {
  value = module.database.endpoint
}

output "db_master_secret_arn" {
  value = module.database.master_secret_arn
}
