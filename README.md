# [Traccar](https://www.traccar.org)

## Overview

Traccar is an open source GPS tracking system. This repository contains Java-based back-end service. It supports more than 200 GPS protocols and more than 2000 models of GPS tracking devices. Traccar can be used with any major SQL database system. It also provides easy to use [REST API](https://www.traccar.org/traccar-api/).

Other parts of Traccar solution include:

- [Traccar web app](https://github.com/traccar/traccar-web)
- [Traccar Manager app](https://github.com/traccar/traccar-manager)

There is also a set of mobile apps that you can use for tracking mobile devices:

- [Traccar Client app](https://github.com/traccar/traccar-client)

## Features

Some of the available features include:

- Real-time GPS tracking
- Driver behaviour monitoring
- Detailed and summary reports
- Geofencing functionality
- Alarms and notifications
- Account and device management
- Email and SMS support

## Build

Please read [build from source documentation](https://www.traccar.org/build/) on the official website.

## Team

- Anton Tananaev ([anton@traccar.org](mailto:anton@traccar.org))
- Andrey Kunitsyn ([andrey@traccar.org](mailto:andrey@traccar.org))

## License

    Apache License, Version 2.0

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


## HOW TO
Front:
wget http://localhost:8082/
http://localhost:8082/register
email: sweetword
password: sweetword

Positions: select protocol,deviceid,valid,latitude,longitude,speed,accuracy,network,geofenceids from tc_positions;
User: select name,email,hashedpassword  from tc_users;
Device: select name,positionid,status,lastupdate from tc_devices;

## Test
make test-topin-protocole
(imei 358655600695588)

make send-h02-trail
(imei 358655600007040)

# Open port so far
ho2: 5013
ZhongXun Topin (tipin protocole): 5199
istartek (positrex protocole):5252
Frontend: 8082

# LOG
make log (tracker log)
make logs (docker comopose log)

# Remove database
sudo rm -rf /opt/traccar/data