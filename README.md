# [Traccar](https://www.traccar.org)
[![Build Status](https://travis-ci.org/tananaev/traccar.svg?branch=master)](https://travis-ci.org/tananaev/traccar)

## Overview

Traccar is open source server for various GPS tracking devices. Project is written in Java and works on most platforms with installed Java Runtime Environment.

## Build

Please read [build from source documentation](https://www.traccar.org/build/) on the official website.

To build the Docker images first clone:

$ git clone git@github.com:tananaev/traccar.git
$ cd traccar

Now init submodules:

$ git submodule init
$ git submodule update  

Now you are able to build docker images:

$ chmod 755 ./setup/docker/build.sh
$ ./setup/docker/build.sh

Running docker container based on this images:

$ docker run --name traccar  -p 8082:8082 -p 5000-5150:5000-5150 tananaev/traccar:<TAG>

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
