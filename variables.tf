variable "db_password" {
  description = "Database administrator password"
  type        = string
}

variable "amp_rep" {
  type = string
  default = "https://github.com/fleetmap-io/traccar-vue-element"
}

variable "amp_app_name" {
  type = string
  default = "traccar-vue-element"
}
