#!/bin/sh

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
