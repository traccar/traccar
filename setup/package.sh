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
  if ! eval $1 &>/dev/null
  then
    echo $2
    exit 1
  fi 
}

check_requirement "ls ../../ext-6.2.0" "Missing ExtJS (https://www.sencha.com/legal/GPL/)"
check_requirement "ls innosetup-*.exe" "Missing Inno Setup (http://www.jrsoftware.org/isdl.php)"
check_requirement "ls java-*.windows.x86_64.zip" "Missing Windows 64 Java (https://github.com/ojdkbuild/ojdkbuild)"
check_requirement "ls jdk-*-linux-x64.zip" "Missing Linux 64 Java (https://github.com/ojdkbuild/contrib_jdk10u-ci/releases)"
check_requirement "ls jdk-*-linux-armhf.zip" "Missing Linux ARM Java (https://github.com/ojdkbuild/contrib_jdk10u-aarch32-ci/releases)"
check_requirement "which sencha" "Missing sencha cmd package (https://www.sencha.com/products/extjs/cmd-download/)"
check_requirement "which unzip" "Missing unzip"
check_requirement "which wine" "Missing wine"
check_requirement "which innoextract" "Missing innoextract"
check_requirement "which makeself" "Missing makeself"
check_requirement "which jlink" "Missing jlink"

prepare () {
  ../traccar-web/tools/minify.sh

  mkdir -p out/{conf,data,lib,logs,web,schema,templates}

  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp ../schema/* out/schema
  cp -r ../templates/* out/templates
  cp -r ../traccar-web/web/* out/web
  cp default.xml out/conf
  cp traccar.xml out/conf

  innoextract innosetup-*.exe
  echo "If you got any errors here try isetup version 5.5.5 (or check supported versions using 'innoextract -v')"
}

cleanup () {
  rm ../traccar-web/web/app.min.js

  rm -r out
  rm -r tmp
  rm -r app
}

package_other () {
  cp README.txt out
  cd out
  zip -r ../traccar-other-$VERSION.zip *
  cd ..
  rm out/README.txt
}

package_windows () {
  unzip -o java-*.windows.x86_64.zip
  jlink --module-path java-*.windows.x86_64/jmods --add-modules java.se.ee --output out/jre
  rm -rf java-*.windows.x86_64
  wine app/ISCC.exe traccar.iss
  rm -rf out/jre
  zip -j traccar-windows-64-$VERSION.zip Output/traccar-setup.exe README.txt
  rm -r Output
}

package_unix () {
  cp setup.sh out
  cp traccar.service out

  unzip -o jdk-*-linux-x64.zip
  jlink --module-path jdk-*-linux-x64/jmods --add-modules java.se.ee --output out/jre
  rm -rf jdk-*-linux-x64
  makeself --notemp out traccar.run "traccar" ./setup.sh
  rm -rf out/jre
  zip -j traccar-linux-64-$VERSION.zip traccar.run README.txt
  rm traccar.run

  unzip -o jdk-*-linux-armhf.zip
  jlink --module-path jdk-*-linux-armhf/jmods --add-modules java.se.ee --output out/jre
  rm -rf jdk-*-linux-armhf
  makeself --notemp out traccar.run "traccar" ./setup.sh
  rm -rf out/jre
  zip -j traccar-linux-arm-$VERSION.zip traccar.run README.txt
  rm traccar.run

  rm out/setup.sh
  rm out/traccar.service
}

prepare

package_other
package_windows
package_unix

cleanup
