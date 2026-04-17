variable "name"           { type = string }
variable "vpc_id"         { type = string }
variable "subnet_ids"     { type = list(string) }
variable "node_type"      { type = string  default = "cache.t4g.small" }
variable "num_cache_clusters" { type = number  default = 2 }

resource "aws_security_group" "redis" {
  name   = "${var.name}-redis"
  vpc_id = var.vpc_id
  ingress { from_port = 6379  to_port = 6379  protocol = "tcp"  self = true }
  egress  { from_port = 0     to_port = 0     protocol = "-1"   cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.name}-subnets"
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id          = var.name
  description                   = "SNS Redis (cache + WS pub/sub + push outbox)"
  engine                        = "redis"
  engine_version                = "7.1"
  node_type                     = var.node_type
  num_cache_clusters            = var.num_cache_clusters
  automatic_failover_enabled    = true
  multi_az_enabled              = true
  parameter_group_name          = "default.redis7"
  subnet_group_name             = aws_elasticache_subnet_group.main.name
  security_group_ids            = [aws_security_group.redis.id]
  at_rest_encryption_enabled    = true
  transit_encryption_enabled    = true
  apply_immediately             = false
  snapshot_retention_limit      = 7
}

output "primary_endpoint" { value = aws_elasticache_replication_group.main.primary_endpoint_address }
output "port"             { value = aws_elasticache_replication_group.main.port }
output "security_group_id"{ value = aws_security_group.redis.id }
