#!/bin/sh

app='/opt/traccar'

rm -f -R out

mkdir out
mkdir out/bin
mkdir out/conf
mkdir out/data
mkdir out/lib
mkdir out/logs

cp wrapper/bin/wrapper out/bin
cp wrapper/src/bin/sh.script.in out/bin/traccar
cp wrapper/lib/libwrapper.so out/lib
cp wrapper/lib/wrapper.jar out/lib
cp wrapper/src/conf/wrapper.conf.in out/conf/wrapper.conf

cp tracker-server.jar out
cp lib/* out/lib
cp linux.cfg out/conf

chmod +x out/bin/traccar

sed -i 's/@app.name@/traccar/g' out/bin/traccar
sed -i 's/@app.long.name@/traccar/g' out/bin/traccar

sed -i '/wrapper.java.classpath.1/i\wrapper.java.classpath.2=../tracker-server.jar' out/conf/wrapper.conf
sed -i "/wrapper.app.parameter.1/i\wrapper.app.parameter.2=$app/conf/linux.cfg" out/conf/wrapper.conf
sed -i 's/<YourMainClass>/Main/g' out/conf/wrapper.conf
sed -i 's/@app.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.long.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.description@/traccar/g' out/conf/wrapper.conf

makeself out traccar.run "traccar" "mkdir /opt/traccar; cp -f -R * /opt/traccar; /opt/traccar/bin/traccar install"

rm -f -R out

