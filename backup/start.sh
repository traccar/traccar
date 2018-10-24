#!/bin/bash

cd $(dirname $0)

java -jar ../target/tracker-server.jar ./debug.xml
