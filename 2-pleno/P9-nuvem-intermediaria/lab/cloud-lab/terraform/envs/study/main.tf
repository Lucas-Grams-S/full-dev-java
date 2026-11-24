provider "aws" {
  region = var.region

  # Only against the emulator (it has no real credentials/account); the endpoint itself comes from AWS_ENDPOINT_URL.
  access_key                  = var.emulated ? "test" : null
  secret_key                  = var.emulated ? "test" : null
  skip_credentials_validation = var.emulated
  skip_requesting_account_id  = var.emulated
  skip_metadata_api_check     = var.emulated
  s3_use_path_style           = var.emulated

  default_tags {
    tags = {
      project     = var.project
      environment = var.environment
      managedby   = "terraform"
    }
  }
}

locals {
  name = var.project
  azs  = ["${var.region}a", "${var.region}b"]
}

module "network" {
  source              = "../../modules/network"
  name                = local.name
  azs                 = local.azs
  nat_gateways        = var.nat_gateways
  endpoint_az_count   = var.endpoint_az_count
  interface_endpoints = ["ecr.api", "ecr.dkr", "logs", "secretsmanager", "sqs", "sns"]
}

module "alb" {
  source            = "../../modules/alb"
  name              = local.name
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  security_group_id = module.network.alb_security_group_id
}

module "messaging" {
  source               = "../../modules/messaging"
  name                 = local.name
  restrict_dlq_sources = !var.emulated
}

module "secrets" {
  source               = "../../modules/secrets"
  name                 = local.name
  environment          = var.environment
  recovery_window_days = var.production ? 7 : 0
}

module "database" {
  source            = "../../modules/database"
  name              = local.name
  subnet_ids        = module.network.data_subnet_ids
  security_group_id = module.network.db_security_group_id
  multi_az          = var.multi_az
  create_replica    = var.create_replica
  production        = var.production
}

module "app" {
  source            = "../../modules/ecs_service"
  name              = local.name
  image             = var.image
  subnet_ids        = module.network.app_subnet_ids
  security_group_id = module.network.app_security_group_id
  target_group_arn  = module.alb.target_group_arn
  task_policy_json  = module.messaging.app_policy_json
  autoscaling       = !var.emulated

  environment = {
    SPRING_DATASOURCE_URL    = "jdbc:postgresql://${module.database.endpoint}:5432/payflow?sslmode=require"
    PAYFLOW_EVENTS_TOPIC_ARN = module.messaging.topic_arn
    PAYFLOW_QUEUE_NOTIFS_URL = module.messaging.queue_urls["notifications"]
    PAYFLOW_QUEUE_ANTIFR_URL = module.messaging.queue_urls["antifraud"]
    PAYFLOW_PARAM_PREFIX     = module.secrets.parameter_prefix
  }

  # The two ways to hand a secret to ECS: valueFrom with the ARN (+ ":json-key::" for RDS' JSON secret)
  secrets = {
    SPRING_DATASOURCE_USERNAME  = "${module.database.master_secret_arn}:username::"
    SPRING_DATASOURCE_PASSWORD  = "${module.database.master_secret_arn}:password::"
    PAYFLOW_SECURITY_JWT_SECRET = module.secrets.jwt_secret_arn
  }
  secret_arns = [module.database.master_secret_arn, module.secrets.jwt_secret_arn]
}

module "observability" {
  source                  = "../../modules/observability"
  name                    = local.name
  cluster_name            = module.app.cluster_name
  service_name            = module.app.service_name
  log_group_name          = module.app.log_group_name
  lb_arn_suffix           = module.alb.lb_arn_suffix
  target_group_arn_suffix = module.alb.target_group_arn_suffix
  dlq_names               = module.messaging.dlq_names
  queue_names             = module.messaging.queue_names
  db_identifier           = module.database.identifier
  db_replica_identifier   = module.database.replica_identifier
}

module "eks" {
  count      = var.enable_eks ? 1 : 0
  source     = "../../modules/eks"
  name       = local.name
  subnet_ids = module.network.app_subnet_ids
}

module "exporter" {
  count    = var.enable_lambda ? 1 : 0
  source   = "../../modules/lambda_fn"
  name     = local.name
  zip_path = var.lambda_zip
}
