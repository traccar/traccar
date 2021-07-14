#!/bin/bash

## Set for production mode
sed -i 's/GPSTRACK/'"$SETUP_TITLE"'/g' /opt/traccar/web/release.html
sed -i 's/ATTRIB_NAME/'"$ATTRIB_NAME"'/g' /opt/traccar/web/release.html
sed -i 's#ATTRIB_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/release.html

## Set for debug mode
sed -i 's/GPSTRACK/'"$SETUP_TITLE"'/g' /opt/traccar/web/debug.html
sed -i 's/ATTRIB_NAME/'"$ATTRIB_NAME"'/g' /opt/traccar/web/debug.html
sed -i 's#ATTRIB_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/debug.html

## Set for styles..
sed -i 's/808080/'"$SETUP_COLOR"'/g' /opt/traccar/web/assets/mod.css

## Set for address search
sed -i 's#https://geocoder.bhn.ng#'"$GEOCODE_LINK"'#g' /opt/traccar/web/assets/address.ol.js

##Set for play store link
sed -i 's#PLAY_LINK#'"$PLAY_LINK"'#g' /opt/traccar/web/release.html
sed -i 's#PLAY_LINK#'"$PLAY_LINK"'#g' /opt/traccar/web/debug.html

## Set login page logo
sed -i 's/122/'"$FRONT_LOGO_WIDTH"'/g' /opt/traccar/web/app/view/dialog/Login.js
sed -i 's/43/'"$FRONT_LOGO_HEIGHT"'/g' /opt/traccar/web/app/view/dialog/Login.js
sed -i 's/width:122/'"width:$FRONT_LOGO_WIDTH"'/g' /opt/traccar/web/app.min.js
sed -i 's/height:43/'"height:$FRONT_LOGO_HEIGHT"'/g' /opt/traccar/web/app.min.js

## Set Google Mapi API key
sed -i 's/GOOGLE_API_KEY/'"$GOOGLE_API_KEY"'/g' /opt/traccar/web/app/Style.js

## Set Loggedin logo
sed -i 's/82px/'"$BACK_LOGO_WIDTH px"'/g' /opt/traccar/web/app.min.js
sed -i 's/27px/'"$BACK_LOGO_HEIGHT px"'/g' /opt/traccar/web/app.min.js

exec "$@"

################################
##Main Excecution ##############
################################
exec java -Xms512m -Xmx4096m -Djava.net.preferIPv4Stack=true -jar /opt/traccar/tracker-server.jar /opt/traccar/conf/traccar.xml
