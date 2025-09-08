#!/bin/bash

# Script para actualizar solo las categorías en el servidor
# Reemplaza con tu información del servidor
SERVER="root@146.190.12.218"
WEB_PATH="/opt/traccar/web"

echo "Copiando archivos de categorías actualizadas..."

# Archivos principales con las nuevas categorías
scp traccar-web/build/index.html $SERVER:$WEB_PATH/
scp traccar-web/build/assets/index-MDB9pfTk.js $SERVER:$WEB_PATH/assets/
scp traccar-web/build/assets/index-B7ioX4Rl.css $SERVER:$WEB_PATH/assets/

# Service Worker files
scp traccar-web/build/sw.js $SERVER:$WEB_PATH/
scp traccar-web/build/workbox-e3490c72.js $SERVER:$WEB_PATH/
scp traccar-web/build/assets/workbox-window.prod.es5-B9K5rw8f.js $SERVER:$WEB_PATH/assets/

# Manifest
scp traccar-web/build/manifest.webmanifest $SERVER:$WEB_PATH/

echo "Archivos copiados. Reinicia el servicio Traccar en el servidor:"
echo "sudo systemctl restart traccar"
