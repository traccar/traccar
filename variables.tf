variable "amp_rep" {
  type = string
  default = "https://github.com/fleetmap-io/traccar-vue-element"
}

variable "amp_app_name" {
  type = string
  default = "traccar-vue-element"
}

data "aws_secretsmanager_secret_version" "FONTAWESOME_NPM_AUTH_TOKEN" {
  secret_id = "fontawesome-token"
}

data "aws_secretsmanager_secret_version" "traccar" {
  secret_id = "traccar"
}
