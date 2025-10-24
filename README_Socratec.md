# [Traccar](https://www.traccar.org)

## Overview

Traccar is an open source GPS tracking system. This repository contains Java-based back-end service. It supports more than 200 GPS protocols and more than 2000 models of GPS tracking devices. Traccar can be used with any major SQL database system. It also provides easy to use [REST API](https://www.traccar.org/traccar-api/).

Other parts of Traccar solution include:

- [Traccar web app](https://github.com/traccar/traccar-web)
- [Traccar Manager Android app](https://github.com/traccar/traccar-manager-android)
- [Traccar Manager iOS app](https://github.com/traccar/traccar-manager-ios)

There is also a set of mobile apps that you can use for tracking mobile devices:

- [Traccar Client Android app](https://github.com/traccar/traccar-client-android)
- [Traccar Client iOS app](https://github.com/traccar/traccar-client-ios)

## Local Setup
1. Checkout Traccar repository

### Starting the Application
#### Method 1: Using Gradle (Recommended for Development)

```bash
./gradlew run
```

This will:

- Build the application automatically
- Start the server using the `debug.xml` configuration
- Use H2 database (file-based, no setup required)
- Enable debug mode and console logging

#### Method 2: Using Java directly

```bash
# First build the application
./gradlew build

# Then run with Java
java -jar target/traccar-6.7.1.jar debug.xml
```

#### Method 3: Using custom configuration

```bash
./gradlew run -Pargs="your-config.xml"
```

### Debug.xml configuration details

The gradle run task uses `debug.xml` by default, which configures results in having the supported services accessible at:

- __Web interface__: `./traccar-web/simple` (currently needs submodule initialization)
- __Database__: H2 file database at `./target/database` and Web admin interface enabled
- __Media storage__: `./target/media`

## Accessing the Services
Once started, the services will be accessible at:

| Service                   | URL                           | Note  |
| -------                   | ---                           | ---   |
| Web App (legacy app)      | http://localhost:8082         |       |
| Web App                   | http://localhost:3000         | Web app needs to be setup and started separately, see section further down.   |
| APIs                      | http://localhost:8082/api/    |       |
| Database                  | http://localhost:8082/console |       |

### Access H2 database
1. Open your browser and navigate to http://localhost:8082/console
1. Use these connection settings:
- __JDBC URL__: `jdbc:h2:./target/database`
- __User Name__: `sa`
- __Password__: (leave empty)
- __Driver Class__: `org.h2.Driver`

### Start and access Web App
1. Clone the repository traccar/traccar-web
2. The web-app is react based and can locally be hosted using nodejs. Therefor run `npm install` after you cloned the repo to fetch all required dependencies.
3. To start the web server use `start.sh` or respective `start.bat`. By default the web server is configured using `vite.config.js`. The config defines the APIs used by the web app to be available on `localhost:8082`.
4. After startup, the web app is locally available via http://localhost:3000/.