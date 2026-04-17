variable "domain"             { type = string }
variable "subject_alt_names"   { type = list(string)  default = [] }
variable "zone_id"             { type = string }

resource "aws_acm_certificate" "main" {
  domain_name               = var.domain
  subject_alternative_names = var.subject_alt_names
  validation_method         = "DNS"
  lifecycle { create_before_destroy = true }
}

resource "aws_route53_record" "validation" {
  for_each = {
    for dvo in aws_acm_certificate.main.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }
  allow_overwrite = true
  name            = each.value.name
  type            = each.value.type
  ttl             = 60
  records         = [each.value.record]
  zone_id         = var.zone_id
}

resource "aws_acm_certificate_validation" "main" {
  certificate_arn         = aws_acm_certificate.main.arn
  validation_record_fqdns = [for r in aws_route53_record.validation : r.fqdn]
}

output "certificate_arn" { value = aws_acm_certificate_validation.main.certificate_arn }
