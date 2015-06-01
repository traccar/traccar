# [Traccar](https://www.traccar.org)
[![Build Status](https://travis-ci.org/tananaev/traccar.svg?branch=master)](https://travis-ci.org/tananaev/traccar)

## Contacts

Author - Anton Tananaev ([anton.tananaev@gmail.com](mailto:anton.tananaev@gmail.com))

Website - [https://www.traccar.org](https://www.traccar.org)

## Overview

Traccar is open source server for various GPS tracking devices. Project is written in Java and works on most platforms with installed Java Runtime Environment.

## Build

Traccar is a Maven project. You need Java SDK version 6 or higher to build the project.

### NetBeans (recommended)

NetBeans comes pre-packaged with Maven plugin, so you don't need to install it separately.

If your NetBeans doesn't have Maven plugin, then you need to download (`Tools > Plugins`) and configure (`Tools > Options > Miscellaneous > Maven`) it.

To import project select `File > Open Project`, browse to the location of the project folder and click `Open Project`. To compile the project right click on the project and select `Build`.

### Eclipse

Eclipse is not recommended because of the number of reported problems with Eclipse Maven plugin.

### IntelliJ IDEA

Follow official instructions for <a href="https://www.jetbrains.com/idea/help/importing-project-from-maven-model.html">Importing Project from Maven Model</a>.

### Command Line

Make sure you have Maven and JDK installed. To generate binary files execute `mvn package` command in the terminal.

### Create Installer

Execute `setup/package.sh` shell script in the terminal.

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
