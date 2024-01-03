variable "loadbalancer_certificate_arn" {
  type        = string
  default     = "arn:aws:acm:us-east-1:925447205804:certificate/f48c082e-04ff-4586-818a-53ec8a51eecd"
  description = "Load Balancer SSL certificate ARN. The certificate must be present in AWS Certificate Manager"
}
