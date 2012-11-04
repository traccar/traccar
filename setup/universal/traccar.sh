#!/bin/sh

cp ../../target/tracker-server.jar ./
zip tracker-server.zip tracker-server.jar README.txt
rm tracker-server.jar
