locals {
  custom_rules = [
    {
      source = "/<*>"
      status = "404"
      target = "/index.html"
    },
    {
      source = "/api/reports/<*>"
      target = "https://${aws_api_gateway_rest_api.alb-reports.id}.execute-api.us-east-1.amazonaws.com/Prod/api/reports/<*>"
      status = "200"
    },
    {
      source = "/backend/api"
      target = "https://${aws_api_gateway_rest_api.pinme-backend.id}.execute-api.us-east-1.amazonaws.com/Prod/backend"
      status = "200"
      condition = null
    },
    {
      source = "/pinmeapi/<*>"
      target = "https://${aws_api_gateway_rest_api.pinme-backend.id}.execute-api.us-east-1.amazonaws.com/Prod/pinmeapi/pinmeapi/<*>"
      status = "200"
    },
    {
      source = "/api/<*>"
      target = "https://traccar.fleetmap.pt/api/<*>"
      status = "200"
    },
    {
      source = "/mobile"
      target = "https://fleetmap.io/mobile"
      status = "302"
    },
    {
      source = "/reports/<*>"
      target = "https://master.d3r7j5pqky53ek.amplifyapp.com/<*>"
      status = "200"
    },
    {
      source = "/poi-images/<*>"
      target = "https://reports-backend-responsesbucket-1pz6dzs2zfuh9.s3.us-east-1.amazonaws.com/<*>"
      status = "200"
    },
    {
      source = "/mlogin"
      target = "https://account.fleetmap.io"
      status = "302"
      condition = null
    }
  ]
}



import {
  to = aws_amplify_app.traccar_vue_element
  id = "dd8lr4405gc5h"
}

import {
  to = aws_amplify_app.traccar_vue_element_eu_west-3
  id = "d3a65lpfj4mzy1"
}


resource "aws_amplify_app" "traccar_vue_element" {
  name       = var.amp_app_name
  repository = var.amp_rep
  custom_rule {
source = "/manual"
target = "https://fleetmap.notion.site/Manual-de-Utilizador-da-Plataforma-Wuizy-0946dcd4ac6649d6913c63a91dc0e6fc"
status = "302"
condition = null
}
  custom_rule {
source = "/waze/<*>"
target = "https://embed.waze.com/row-rtserver/web/TGeoRSS/<*>"
status = "200"
condition = null
}
custom_rule {
  source = "/livemap/<*>"
  target = "https://www.waze.com/livemap/<*>"
  status = "200"
  condition = null
}
  custom_rule {
source = "/dashboard/<*>"
target = "https://master.d3r7j5pqky53ek.amplifyapp.com/<*>"
status = "200"
condition = null
}



}

resource "aws_amplify_app" "traccar_vue_element_eu_west-3" {
  provider = aws.eu-west-3
  name       = var.amp_app_name
  repository = var.amp_rep
}
