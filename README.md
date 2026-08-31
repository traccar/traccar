# [Traccar](https://www.traccar.org)

## Overview

Traccar is a free, open source GPS tracking platform. This repository contains the Java-based back-end server, which supports more than 200 GPS protocols and 2000+ models of GPS tracking devices out of the box. Traccar works with any major SQL database and provides an easy to use [REST API](https://www.traccar.org/traccar-api/).

Traccar is built for anyone who needs to track vehicles, assets, or people: fleet operators, GPS tracking resellers running their own white-label platform, and individuals tracking their own devices. You can [self-host it for free](https://www.traccar.org/install-vps/), or use [official managed hosting](https://www.traccar.org/pricing/) if you'd rather not run a server yourself.

Other parts of the Traccar platform:

- [Traccar web app](https://github.com/traccar/traccar-web) - the browser-based tracking dashboard
- [Traccar Manager app](https://github.com/traccar/traccar-manager) - mobile app for viewing your tracked devices

There is also a set of mobile apps for tracking mobile devices themselves:

- [Traccar Client app](https://github.com/traccar/traccar-client)

## Quick Start

Run Traccar with a production-grade MySQL database using Docker Compose:

```shell
curl -o compose.yaml https://raw.githubusercontent.com/traccar/traccar/master/docker/compose/traccar-mysql.yaml
docker compose up -d
```

Traccar will be available on port `8082`. See the [Docker documentation](https://www.traccar.org/docker/) for other configuration options, or [try the live demo](https://www.traccar.org/demo-server/) without installing anything.

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

Please read the [build from source documentation](https://www.traccar.org/build/) on the official website.

## Community

- [Forums](https://www.traccar.org/forums/)
- [Documentation](https://www.traccar.org/documentation/)

## Team

- Anton Tananaev ([anton@traccar.org](mailto:anton@traccar.org))
- Andrey Kunitsyn ([andrey@traccar.org](mailto:andrey@traccar.org))

## License

Apache License, Version 2.0. See [LICENSE.txt](https://github.com/traccar/traccar/blob/master/LICENSE.txt) for details.
