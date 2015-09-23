#!/bin/bash

#
# Script to create installers
#

cd $(dirname $0)

if [[ $# -lt 1 ]]
then
  echo "USAGE: $0 <version>"
  exit 1
fi

VERSION=$1

check_requirement () {
  eval $1 &>/dev/null
  if ! eval $1 &>/dev/null
  then
    echo $2
    exit 1
  fi 
}

check_requirement "ls ../../ext-*" "Missing ../../ext-X.X.X (https://www.sencha.com/legal/GPL/)"
check_requirement "ls wrapper-delta-pack-*.tar.gz" "Missing wrapper-delta-pack-*.tar.gz (http://wrapper.tanukisoftware.com/doc/english/download.jsp)"
check_requirement "ls wrapper-windows-x86-64-*.zip" "Missing wrapper-windows-x86-64-*.zip (http://www.krenger.ch/blog/tag/java-service-wrapper/)"
check_requirement "ls isetup-*.exe" "Missing isetup-*.exe (http://www.jrsoftware.org/isdl.php)"
check_requirement "which sencha" "Missing sencha cmd package (https://www.sencha.com/products/extjs/cmd-download/)"
check_requirement "which wine" "Missing wine package"
check_requirement "which innoextract" "Missing innoextract package"
check_requirement "which makeself" "Missing makeself package"

prepare () {

  tar -xzf wrapper-delta-pack-*.tar.gz
  mv wrapper-delta-pack-*/ wrapper/

  ../tools/minify.sh

  innoextract isetup-*.exe
  echo "If you got any errors here try isetup version 5.5.5 (or check what versions are supported by 'innoextract -v')"
}

cleanup () {

  rm -rf wrapper/

  rm ../web/app.min.js

  rm -rf app/
}

prepare_windows_64 () {
  unzip wrapper-windows-x86-64-*.zip
  cp wrapper_*_src/bin/wrapper.exe wrapper/bin/wrapper-windows-x86-32.exe
  cp wrapper_*_src/lib/wrapper.dll wrapper/lib/wrapper-windows-x86-32.dll
  cp wrapper_*_src/lib/wrapper.jar wrapper/lib/wrapper.jar
  rm -rf wrapper_*_src
}

prepare_linux_32 () {
  cp unix/setup.sh out
  cp wrapper/bin/wrapper-linux-x86-32 out/bin/wrapper
  cp wrapper/lib/libwrapper-linux-x86-32.so out/lib/libwrapper.so
}

prepare_linux_64 () {
  cp unix/setup.sh out
  cp wrapper/bin/wrapper-linux-x86-64 out/bin/wrapper
  cp wrapper/lib/libwrapper-linux-x86-64.so out/lib/libwrapper.so
}

prepare_linux_arm () {
  cp unix/linux-arm-setup.sh out/setup.sh
  cp wrapper/bin/wrapper-linux-armel-32 out/bin
  cp wrapper/bin/wrapper-linux-armhf-32 out/bin
  cp wrapper/lib/libwrapper-linux-armel-32.so out/lib
  cp wrapper/lib/libwrapper-linux-armhf-32.so out/lib
}

prepare_macosx_64 () {
  cp unix/setup.sh out
  cp wrapper/bin/wrapper-macosx-universal-64 out/bin/wrapper
  cp wrapper/lib/libwrapper-macosx-universal-64.jnilib out/lib/libwrapper.jnilib
}

package_windows () {

  if [ "$#" -gt 1 ]
  then
    eval $2
  fi

  wine app/ISCC.exe windows/traccar.iss

  zip -j traccar-$1-$VERSION.zip windows/Output/setup.exe README.txt

  rm -rf windows/Output/
  rm -rf tmp/
}

package_unix () {

  mkdir -p out/{bin,conf,data,lib,logs,web}

  cp wrapper/src/bin/sh.script.in out/bin/traccar
  cp wrapper/lib/wrapper.jar out/lib
  cp wrapper/src/conf/wrapper.conf.in out/conf/wrapper.conf

  sed -i 's/tail -1/tail -n 1/g' out/bin/traccar
  chmod +x out/bin/traccar

  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp -r ../web/* out/web
  cp unix/traccar.xml out/conf

  sed -i 's/@app.name@/traccar/g' out/bin/traccar
  sed -i 's/@app.long.name@/traccar/g' out/bin/traccar

  sed -i '/wrapper.java.classpath.1/i\wrapper.java.classpath.2=../tracker-server.jar' out/conf/wrapper.conf
  sed -i '/wrapper.app.parameter.1/i\wrapper.app.parameter.2=../conf/traccar.xml' out/conf/wrapper.conf
  sed -i 's/wrapper.java.additional.1=/wrapper.java.additional.1=-Dfile.encoding=UTF-8/g' out/conf/wrapper.conf
  sed -i 's/<YourMainClass>/org.traccar.Main/g' out/conf/wrapper.conf
  sed -i 's/@app.name@/traccar/g' out/conf/wrapper.conf
  sed -i 's/@app.long.name@/traccar/g' out/conf/wrapper.conf
  sed -i 's/@app.description@/traccar/g' out/conf/wrapper.conf
  sed -i 's/wrapper.logfile=..\/logs\/wrapper.log/wrapper.logfile=..\/logs\/wrapper.log.YYYYMMDD\nwrapper.logfile.rollmode=DATE/g' out/conf/wrapper.conf

  eval $2

  makeself out traccar.run "traccar" "chmod +x setup.sh ; ./setup.sh"
  zip -j traccar-$1-$VERSION.zip traccar.run README.txt

  rm traccar.run
  rm -rf out/
}

package_universal () {

  mkdir -p out/{conf,data,lib,logs,web}

  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp -r ../web/* out/web
  cp windows/traccar.xml out/conf
  cp README.txt out

  cd out
  zip -r ../traccar-$1-$VERSION.zip * README.txt
  cd ..

  rm -rf out/
}

prepare

package_windows "windows-32"
package_windows "windows-64" "prepare_windows_64"
package_unix "linux-32" "prepare_linux_32"
package_unix "linux-64" "prepare_linux_64"
package_unix "linux-arm" "prepare_linux_arm"
package_unix "macosx-64" "prepare_macosx_64"
package_universal "other"

cleanup
