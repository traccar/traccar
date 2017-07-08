#!/bin/sh

if which java &>/dev/null
then
  if [ $(java -version 2>&1 | grep -i version | sed 's/.*version \"\(.*\)\.\(.*\)\..*\"/\1\2/; 1q') -lt 17 ]
  then
    echo 'Java 7 or higher required'
  else
    mkdir -p /opt/traccar
    cp -r * /opt/traccar
    rm -r ../out
    rm /opt/traccar/setup.sh
    chmod -r go+rX /opt/traccar
    /opt/traccar/bin/installDaemon.sh
  fi
else
  echo 'Java runtime is required'
fi
