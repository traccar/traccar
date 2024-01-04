variable "loadbalancer_certificate_arn" {
  type        = string
  default     = "arn:aws:acm:us-east-1:925447205804:certificate/f48c082e-04ff-4586-818a-53ec8a51eecd"
  description = "Load Balancer SSL certificate ARN. The certificate must be present in AWS Certificate Manager"
}

variable "env_vars" {
  type        = map(string)
  default     = {
    CONFIG_USE_ENVIRONMENT_VARIABLES = "true"
    DATABASE_PASSWORD = ""
    EMAIL_LINK = "admin@fleetmap.io"
    DOMAIN_LINK = "traccar.fleetmap.pt"
    PORT = "8082"
  }
  description = "Map of custom ENV variables to be provided to the application running on Elastic Beanstalk, e.g. env_vars = { DB_USER = 'admin' DB_PASS = 'xxxxxx' }"
}
