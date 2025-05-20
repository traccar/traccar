#!/bin/sh

# Créer le répertoire d'installation
mkdir -p /opt/traccar

# Copier les fichiers
cp -r * /opt/traccar
chmod -R go+rX /opt/traccar

# Configurer le service
cp /opt/traccar/traccar.service /etc/systemd/system
chmod 664 /etc/systemd/system/traccar.service

# Recharger systemd
systemctl daemon-reload
systemctl enable traccar.service

# Nettoyer
rm /opt/traccar/setup.sh
rm -r ../out

echo "Installation terminée. Pour démarrer Traccar, exécutez :"
echo "sudo systemctl start traccar.service" 