#!/bin/bash

touch /opt/traccar/logs/tracker-server.log
cd /opt/traccar
java -jar traccar-server.jar traccar.xml