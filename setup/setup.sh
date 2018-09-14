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

    mv /opt/traccar/traccar.service /etc/systemd/system
    chmod 664 /etc/systemd/system/traccar.service

    systemctl daemon-reload
    systemctl enable traccar.service
    systemctl start traccar.service
  else
    echo 'Java 7 or higher is required'
  fi
else
  echo 'Java runtime is required'
fi
