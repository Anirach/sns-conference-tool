terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws    = { source = "hashicorp/aws", version = "~> 5.65" }
    random = { source = "hashicorp/random", version = "~> 3.6" }
  }
  backend "s3" {
    bucket = "sns-tfstate-prod"
    key    = "infra/sns/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = { app = "sns", env = "prod", iac = "terraform" }
  }
}

module "kms_app" {
  source      = "../../modules/kms"
  alias       = "sns-prod-app"
  description = "App-level secrets (JWT signing keys, SNS token crypto)"
}

module "kms_data" {
  source      = "../../modules/kms"
  alias       = "sns-prod-data"
  description = "RDS + S3 + ElastiCache encryption"
}

module "vpc" {
  source = "../../modules/vpc"
  name   = "sns-prod"
  cidr   = "10.30.0.0/16"
  azs    = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

module "rds" {
  source            = "../../modules/rds-postgis"
  name              = "sns-prod"
  vpc_id            = module.vpc.vpc_id
  subnet_ids        = module.vpc.private_subnet_ids
  instance_class    = "db.m7g.large"
  allocated_storage = 200
  kms_key_arn       = module.kms_data.key_arn
}

module "redis" {
  source              = "../../modules/elasticache-redis"
  name                = "sns-prod"
  vpc_id              = module.vpc.vpc_id
  subnet_ids          = module.vpc.private_subnet_ids
  node_type           = "cache.m7g.large"
  num_cache_clusters  = 3
}

module "articles_bucket" {
  source       = "../../modules/s3"
  name         = "sns-prod-articles"
  kms_key_arn  = module.kms_data.key_arn
}

module "route53" {
  source    = "../../modules/route53"
  zone_name = "sns.example.com"
}

module "acm_api" {
  source            = "../../modules/acm"
  domain            = "api.sns.example.com"
  subject_alt_names = []
  zone_id           = module.route53.zone_id
}

output "rds_endpoint"    { value = module.rds.endpoint }
output "redis_endpoint"  { value = module.redis.primary_endpoint }
output "articles_bucket" { value = module.articles_bucket.bucket }
output "api_certificate" { value = module.acm_api.certificate_arn }
