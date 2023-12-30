provider "aws" {
  profile    = "us.east.1"
  region     = "us-east-1"
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
}
