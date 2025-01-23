#!/bin/sh

PRESERVECONFIG=0
if [ -f /opt/digitalegiz/conf/digitalegiz.xml ]
then
    cp /opt/digitalegiz/conf/digitalegiz.xml /opt/digitalegiz/conf/digitalegiz.xml.saved
    PRESERVECONFIG=1
fi

mkdir -p /opt/digitalegiz
cp -r * /opt/digitalegiz
chmod -R go+rX /opt/digitalegiz

if [ ${PRESERVECONFIG} -eq 1 ] && [ -f /opt/digitalegiz/conf/digitalegiz.xml.saved ]
then
    mv -f /opt/digitalegiz/conf/digitalegiz.xml.saved /opt/digitalegiz/conf/digitalegiz.xml
fi

mv /opt/digitalegiz/digitalegiz.service /etc/systemd/system
chmod 664 /etc/systemd/system/digitalegiz.service

systemctl daemon-reload
systemctl enable digitalegiz.service

rm /opt/digitalegiz/setup.sh
rm -r ../out
