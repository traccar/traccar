#!/bin/bash

#
# Shell script to create installers
#

cd $(dirname $0)

if [[ $# -lt 1 ]]
then
  echo "USAGE: $0 <version>"
  exit 1
fi

# GENERAL REQUIREMENTS

# Check wrapper
if ! ls wrapper-delta-pack-*.tar.gz &>/dev/null
then
  echo "Missing wrapper-delta-pack-*.tar.gz (http://wrapper.tanukisoftware.com/doc/english/download.jsp)"
  exit 1
fi

# Check Windows x64 wrapper
if ! ls wrapper-windows-x86-64-*.zip &>/dev/null
then
  echo "Missing wrapper-windows-x86-64-*.zip (http://www.krenger.ch/blog/tag/java-service-wrapper/)"
  exit 1
fi

# WINDOWS REQUIREMENTS

# Check inno setup
if ! ls isetup-*.exe &>/dev/null
then
  echo "Missing isetup-*.exe (http://www.jrsoftware.org/isdl.php)"
  exit 1
fi

# Check wine
if ! which wine &>/dev/null
then
  echo "Missing wine package"
  exit 1
fi

# Check innoextract
if ! which innoextract &>/dev/null
then
  echo "Missing innoextract package"
  exit 1
fi

# LINUX REQUIREMENTS

# Check makeself
if ! which makeself &>/dev/null
then
  echo "Install makeself package"
  exit 1
fi

# GENERAL PREPARATION

tar -xzf wrapper-delta-pack-*.tar.gz
mv wrapper-delta-pack-*/ wrapper/

../tools/minify.sh

# UNIVERSAL PACKAGE

zip -j tracker-server-$1.zip ../target/tracker-server.jar universal/README.txt

# WINDOWS PACKAGE

innoextract isetup-*.exe
echo "NOTE: if you got any errors here try isetup version 5.5.5 (or check what versions are supported by 'innoextract -v')"

# windows 32

wine app/ISCC.exe windows/traccar.iss

zip -j traccar-windows-32-$1.zip windows/Output/setup.exe windows/README.txt

rm -rf windows/Output/
rm -rf tmp/

# windows 64

unzip wrapper-windows-x86-64-*.zip
cp wrapper_*_src/bin/wrapper.exe wrapper/bin/wrapper-windows-x86-32.exe
cp wrapper_*_src/lib/wrapper.dll wrapper/lib/wrapper-windows-x86-32.dll
cp wrapper_*_src/lib/wrapper.jar wrapper/lib/wrapper.jar
rm -rf wrapper_*_src

wine app/ISCC.exe windows/traccar.iss

zip -j traccar-windows-64-$1.zip windows/Output/setup.exe windows/README.txt

rm -rf windows/Output/
rm -rf tmp/

rm -rf app/

# LINIX PACKAGE

app='/opt/traccar'

rm -rf out

mkdir out
mkdir out/bin
mkdir out/conf
mkdir out/data
mkdir out/lib
mkdir out/logs
mkdir out/web

cp wrapper/src/bin/sh.script.in out/bin/traccar
cp wrapper/lib/wrapper.jar out/lib
cp wrapper/src/conf/wrapper.conf.in out/conf/wrapper.conf

sed -i 's/tail -1/tail -n 1/g' out/bin/traccar
chmod +x out/bin/traccar

cp ../target/tracker-server.jar out
cp ../target/lib/* out/lib
cp -r ../web/* out/web
cp linux/traccar.xml out/conf

sed -i 's/@app.name@/traccar/g' out/bin/traccar
sed -i 's/@app.long.name@/traccar/g' out/bin/traccar

sed -i '/wrapper.java.classpath.1/i\wrapper.java.classpath.2=../tracker-server.jar' out/conf/wrapper.conf
sed -i "/wrapper.app.parameter.1/i\wrapper.app.parameter.2=$app/conf/traccar.xml" out/conf/wrapper.conf
sed -i 's/<YourMainClass>/org.traccar.Main/g' out/conf/wrapper.conf
sed -i 's/@app.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.long.name@/traccar/g' out/conf/wrapper.conf
sed -i 's/@app.description@/traccar/g' out/conf/wrapper.conf
sed -i 's/wrapper.logfile=..\/logs\/wrapper.log/wrapper.logfile=..\/logs\/wrapper.log.YYYYMMDD\nwrapper.logfile.rollmode=DATE/g' out/conf/wrapper.conf

# linux 32

cp wrapper/bin/wrapper-linux-x86-32 out/bin/wrapper
cp wrapper/lib/libwrapper-linux-x86-32.so out/lib/libwrapper.so

makeself out traccar.run "traccar" "if [ $(java -version 2>&1 | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]; then echo "Please install Java version 7 or higher"; else mkdir $app; cp -rf * $app; $app/bin/traccar install; fi"
zip -j traccar-linux-32-$1.zip traccar.run linux/README.txt

# linux 64

cp wrapper/bin/wrapper-linux-x86-64 out/bin/wrapper
cp wrapper/lib/libwrapper-linux-x86-64.so out/lib/libwrapper.so

makeself out traccar.run "traccar" "if [ $(java -version 2>&1 | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]; then echo "Please install Java version 7 or higher"; else mkdir $app; cp -rf * $app; $app/bin/traccar install; fi"
zip -j traccar-linux-64-$1.zip traccar.run linux/README.txt

# linux arm

rm out/bin/wrapper
rm out/lib/libwrapper.so

cp wrapper/bin/wrapper-linux-armel-32 out/bin/
cp wrapper/bin/wrapper-linux-armhf-32 out/bin/
cp wrapper/lib/libwrapper-linux-armel-32.so out/lib/
cp wrapper/lib/libwrapper-linux-armhf-32.so out/lib/

makeself out traccar.run "traccar" "if [ $(java -version 2>&1 | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]; then echo "Please install Java version 7 or higher"; else mkdir $app; cp -rf * $app; if [ -z "`readelf -A /proc/self/exe | grep Tag_ABI_VFP_args`" ]; then mv $app/bin/wrapper-linux-armel-32 $app/bin/wrapper; mv $app/lib/libwrapper-linux-armel-32.so $app/lib/libwrapper.so; else mv $app/bin/wrapper-linux-armhf-32 $app/bin/wrapper; mv $app/lib/libwrapper-linux-armhf-32.so $app/lib/libwrapper.so; fi; $app/bin/traccar install; fi"
zip -j traccar-linux-arm-$1.zip traccar.run linux/README.txt

# MACOSX PACKAGE

rm out/conf/traccar.xml
rm out/bin/wrapper-linux-armel-32
rm out/bin/wrapper-linux-armhf-32
rm out/lib/libwrapper-linux-armel-32.so
rm out/lib/libwrapper-linux-armhf-32.so

cp macosx/traccar.xml out/conf

cp wrapper/bin/wrapper-macosx-universal-64 out/bin/wrapper
cp wrapper/lib/libwrapper-macosx-universal-64.jnilib out/lib/libwrapper.jnilib

makeself out traccar.run "traccar" "mkdir -p $app; cp -rf * $app; $app/bin/traccar install"
zip -j traccar-macosx-64-$1.zip traccar.run macosx/README.txt

rm traccar.run
rm -rf out

# GENERAL CLEANUP

rm -rf wrapper/

rm ../web/app.min.js

exit 0
