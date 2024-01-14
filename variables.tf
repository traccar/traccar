variable "db_password" {
  description = "Database administrator password"
  type        = string
  sensitive   = true
}

variable "env_vars" {
  type        = map(string)
  default     = {
    EMAIL_LINK = "admin@fleetmap.io"
    DOMAIN_LINK = "traccar.fleetmap.pt"
    PORT = "8082"
  }
  description = "Map of custom ENV variables to be provided to the application running on Elastic Beanstalk, e.g. env_vars = { DB_USER = 'admin' DB_PASS = 'xxxxxx' }"
}
