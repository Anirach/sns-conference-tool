variable "name"               { type = string }
variable "vpc_id"             { type = string }
variable "subnet_ids"         { type = list(string) }
variable "instance_class"     { type = string  default = "db.t4g.medium" }
variable "allocated_storage"  { type = number  default = 50 }
variable "engine_version"     { type = string  default = "15.6" }
variable "kms_key_arn"        { type = string }

resource "aws_security_group" "db" {
  name   = "${var.name}-db"
  vpc_id = var.vpc_id

  ingress {
    from_port = 5432
    to_port   = 5432
    protocol  = "tcp"
    self      = true
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.name}-subnets"
  subnet_ids = var.subnet_ids
}

resource "random_password" "master" {
  length  = 32
  special = false
}

resource "aws_db_instance" "main" {
  identifier              = var.name
  engine                  = "postgres"
  engine_version          = var.engine_version
  instance_class          = var.instance_class
  allocated_storage       = var.allocated_storage
  storage_type            = "gp3"
  storage_encrypted       = true
  kms_key_id              = var.kms_key_arn
  username                = "sns"
  password                = random_password.master.result
  db_name                 = "sns"
  db_subnet_group_name    = aws_db_subnet_group.main.name
  vpc_security_group_ids  = [aws_security_group.db.id]
  multi_az                = true
  backup_retention_period = 14
  deletion_protection     = true
  apply_immediately       = false
  skip_final_snapshot     = false
  final_snapshot_identifier = "${var.name}-final"

  # PostGIS extension is created by the application's V1 Flyway migration.
}

output "endpoint"           { value = aws_db_instance.main.address }
output "port"               { value = aws_db_instance.main.port }
output "security_group_id"  { value = aws_security_group.db.id }
output "master_password_arn" {
  value     = aws_db_instance.main.master_user_secret[0].secret_arn
  sensitive = true
}
