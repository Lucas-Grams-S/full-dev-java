output "alb_dns_name" {
  value = module.alb.dns_name
}

output "topic_arn" {
  value = module.messaging.topic_arn
}

output "queue_urls" {
  value = module.messaging.queue_urls
}

output "reporting_queue_url" {
  value = module.messaging.reporting_queue_url
}

output "event_bus_name" {
  value = module.messaging.event_bus_name
}

output "jwt_secret_arn" {
  value = module.secrets.jwt_secret_arn
}

output "cluster_name" {
  value = module.app.cluster_name
}

output "service_name" {
  value = module.app.service_name
}

output "db_identifier" {
  value = module.database.identifier
}

output "replica_identifier" {
  value = module.database.replica_identifier
}

output "alarm_names" {
  value = module.observability.alarm_names
}

output "vpc_id" {
  value = module.network.vpc_id
}
