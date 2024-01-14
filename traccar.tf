terraform {
  cloud {
    organization = "awsuseast1"
    workspaces { name = "traccar" }
  }
}

provider "aws" {
  profile    = "us.east.1"
  region     = "us-east-1"
}

resource "aws_security_group" "traccar-sg" {
  name        = "traccar-sg"
  ingress {
    description = "devices tcp"
    from_port   = 5000
    to_port     = 5500
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "devices udp"
    from_port   = 5000
    to_port     = 5500
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "api"
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "nginx proxy"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "nginx proxy https"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "ssh"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "traccar-sg"
  }
}

resource "aws_elastic_beanstalk_application" "traccar" {
  name        = "traccar-tf"
}

resource "aws_elastic_beanstalk_environment" "traccar-env" {
  name                = "traccar-env"
  application         = aws_elastic_beanstalk_application.traccar.name
  solution_stack_name = "64bit Amazon Linux 2023 v4.1.2 running Corretto 11"
  setting {
        namespace = "aws:autoscaling:launchconfiguration"
        name = "IamInstanceProfile"
        value = "aws-elasticbeanstalk-ec2-role"
  }
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name = "SecurityGroups"
    value = aws_security_group.traccar-sg.name
  }
  setting {
    namespace = "aws:elasticbeanstalk:cloudwatch:logs"
    name      = "StreamLogs"
    value     = "true"
  }
  setting {
    namespace = "aws:ec2:instances"
    name      = "InstanceTypes"
    value     = "t3.medium"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "SingleInstance"
  }

  # Add environment variables if provided
  dynamic "setting" {
    for_each = var.env_vars
    content {
      namespace = "aws:elasticbeanstalk:application:environment"
      name      = setting.key
      value     = setting.value
      resource  = ""
    }
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "CONFIG_USE_ENVIRONMENT_VARIABLES"
    value     = "true"
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DATABASE_PASSWORD"
    value     = var.db_password
  }
}

resource "aws_ses_domain_identity" "ses_gpsmanager" {
  domain = "gpsmanager.io"
}

resource "aws_sesv2_contact_list" "default" {
  contact_list_name = "default"
  topic {
    default_subscription_status = "OPT_IN"
    description                 = "Fleetmap Alerts"
    display_name                = "Alerts"
    topic_name                  = "Alerts"
  }
  topic {
    default_subscription_status = "OPT_IN"
    description                 = "Fleetmap Reports"
    display_name                = "Reporting"
    topic_name                  = "Reporting"
  }
}
