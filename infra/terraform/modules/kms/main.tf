variable "alias"       { type = string }
variable "description" { type = string }

resource "aws_kms_key" "main" {
  description             = var.description
  deletion_window_in_days = 30
  enable_key_rotation     = true
}

resource "aws_kms_alias" "main" {
  name          = "alias/${var.alias}"
  target_key_id = aws_kms_key.main.key_id
}

output "key_arn" { value = aws_kms_key.main.arn }
output "key_id"  { value = aws_kms_key.main.key_id }
