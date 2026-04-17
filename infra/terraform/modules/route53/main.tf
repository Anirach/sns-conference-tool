variable "zone_name" { type = string }
variable "records"   {
  type = list(object({
    name = string
    type = string
    ttl  = number
    records = list(string)
  }))
  default = []
}

data "aws_route53_zone" "main" {
  name         = var.zone_name
  private_zone = false
}

resource "aws_route53_record" "main" {
  for_each = { for r in var.records : "${r.type}-${r.name}" => r }
  zone_id  = data.aws_route53_zone.main.zone_id
  name     = each.value.name
  type     = each.value.type
  ttl      = each.value.ttl
  records  = each.value.records
}

output "zone_id" { value = data.aws_route53_zone.main.zone_id }
