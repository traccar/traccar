#!/bin/sh

mkdir -p /opt/traccar
cp -vr * /opt/traccar
chmod -vR go+rX /opt/traccar

mv -v /opt/traccar/traccar.service /etc/systemd/system
chmod -v 664 /etc/systemd/system/traccar.service

systemctl daemon-reload
systemctl enable traccar.service

rm /opt/traccar/setup.sh
rm -r ../out
