#!/bin/bash

sed -i 's/GPSTRACK/'"$SETUP_TITLE"'/g' /opt/traccar/web/release.html
sed -i 's/ATTRIB_NAME/'"$ATTRIB_NAME"'/g' /opt/traccar/web/release.html
sed -i 's#ATTRIB_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/release.html
sed -i 's#PLAY_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/release.html

##Set for debug mode

sed -i 's/GPSTRACK/'"$SETUP_TITLE"'/g' /opt/traccar/web/debug.html
sed -i 's/ATTRIB_NAME/'"$ATTRIB_NAME"'/g' /opt/traccar/web/debug.html
sed -i 's#ATTRIB_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/debug.html
sed -i 's#PLAY_LINK#'"$ATTRIB_LINK"'#g' /opt/traccar/web/debug.html


##Set login page logo
sed -i 's/122/'"$FRONT_LOGO_WIDTH"'/g' /opt/traccar/web/app/view/dialog/Login.js
sed -i 's/43/'"$FRONT_LOGO_HEIGHT"'/g' /opt/traccar/web/app/view/dialog/Login.js
sed -i 's/width:122/'"width:$FRONT_LOGO_WIDTH"'/g' /opt/traccar/web/app.min.js
sed -i 's/height:43/'"height:$FRONT_LOGO_HEIGHT"'/g' /opt/traccar/web/app.min.js

##Set Loggedin logo
sed -i 's/82px/'"$BACK_LOGO_WIDTH px"'/g' /opt/traccar/web/app.min.js
sed -i 's/27px/'"$BACK_LOGO_HEIGHT px"'/g' /opt/traccar/web/app.min.js

exec "$@"

######################################
##Main Excecution####
################################
exec java -Xms4096m -Xmx22024m -Djava.net.preferIPv4Stack=true -jar /opt/traccar/tracker-server.jar /opt/traccar/conf/traccar.xml


