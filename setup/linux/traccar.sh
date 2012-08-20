#!/bin/sh

app='/opt/traccar'

tar -xzf wrapper-delta-pack-*.tar.gz
mv wrapper-delta-pack-*/ wrapper/

rm -rf out

mkdir out
mkdir out/bin
mkdir out/conf
mkdir out/data
mkdir out/lib
mkdir out/logs

cp wrapper/src/bin/sh.script.in out/bin/traccar
cp wrapper/lib/wrapper.jar out/lib
cp wrapper/src/conf/wrapper.conf.in out/conf/wrapper.conf

cp ../../target/tracker-server.jar out
cp ../../target/lib/* out/lib
cp linux.cfg out/conf

sed -i 's/@app.name@/traccar/g' out/bin/traccar
sed -i 's/@app.long.name@/traccar/g' out/bin/traccar

sed -i '/wrapper.java.classpath.1/i\wrapper.java.classpath.2=../tracker-server.jar' out/conf/wrapper.conf
sed -i "/wrapper.app.parameter.1/i\wrapper.app.parameter.2=$app/conf/linux.cfg" out/conf/wrapper.conf
sed -i 's/<YourMainClass>/org.traccar.Main/g' out/conf/wrapper.conf
sed -i 's/@app.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.long.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.description@/traccar/g' out/conf/wrapper.conf

# linux 32

cp wrapper/bin/wrapper-linux-x86-32 out/bin/wrapper
cp wrapper/lib/libwrapper-linux-x86-32.so out/lib/libwrapper.so
chmod +x out/bin/traccar

makeself out traccar.run "traccar" "mkdir $app; cp -rf * $app; $app/bin/traccar install"
zip traccar-linux-32.zip traccar.run README.txt

# linux 64

cp wrapper/bin/wrapper-linux-x86-64 out/bin/wrapper
cp wrapper/lib/libwrapper-linux-x86-64.so out/lib/libwrapper.so
chmod +x out/bin/traccar

makeself out traccar.run "traccar" "mkdir $app; cp -rf * $app; $app/bin/traccar install"
zip traccar-linux-64.zip traccar.run README.txt

rm traccar.run
rm -rf out
rm -rf wrapper

