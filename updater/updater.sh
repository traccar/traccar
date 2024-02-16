#!/bin/bash
#----------------------------------------
# OPTIONS
#----------------------------------------
BACKUP_PATH='/root/backup/'

# Create the backup folder
if [ ! -d $BACKUP_PATH ]; then
  mkdir -p $BACKUP_PATH
fi

# Download Traccar
wget https://github.com/traccar/traccar/releases/download/v5.12/traccar-linux-64-5.12.zip

# Unzip Traccar
unzip traccar-linux-*.zip

# Backup config files
sudo cp /etc/systemd/system/traccar.service /root/backup/traccar.service
sudo cp /opt/traccar/conf/default.xml /root/backup/default.xml
sudo cp /opt/traccar/conf/traccar.xml /root/backup/traccar.xml

# Remove Traccar
sudo systemctl stop traccar.service
sudo systemctl disable traccar.service
sudo rm /etc/systemd/system/traccar.service
sudo systemctl daemon-reload
sudo rm -R /opt/traccar


# Install Traccar

sudo ./traccar.run


# Restore config files
sudo cp /root/backup/traccar.service /etc/systemd/system/traccar.service
sudo systemctl daemon-reload
#sudo cp /root/backup/default.xml /opt/traccar/conf/default.xml
sudo cp /root/backup/traccar.xml /opt/traccar/conf/traccar.xml

# Run Traccar
sudo systemctl start traccar.service

# Clear
sudo rm -f traccar.run README.txt traccar-linux-*.zip