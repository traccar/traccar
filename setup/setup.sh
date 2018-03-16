#!/bin/sh

if which java &>/dev/null
then
  if java -jar test.jar
  then
    mkdir -p /opt/traccar
    cp -r * /opt/traccar
    rm -r ../out
    rm /opt/traccar/setup.sh
    chmod -R go+rX /opt/traccar
    cd /opt/traccar/bin/
    /opt/traccar/bin/installDaemon.sh
  else
    echo 'Java 7 or higher is required'
  fi
else
  echo 'Java runtime is required'
fi
