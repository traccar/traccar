#!/usr/bin/env bash

#
# Script to create installers for various platforms.
#

cd $(dirname $0)

usage () {
  echo "Usage: $0 VERSION [PLATFORM]"
  echo "Build Traccar installers."
  echo
  echo "Without PLATFORM provided, builds installers for all platforms."
  echo
  echo "Available platforms:"
  echo " * linux-64"
  echo " * linux-arm"
  echo " * windows-64"
  echo " * other"
  exit 1
}

if [[ $# -lt 1 ]]
then
  usage
fi

info () {
  echo -e "[\033[1;34mINFO\033[0m] "$1
}

ok () {
  echo -e "[\033[1;32m OK \033[0m] "$1
}

warn () {
  echo -e "[\033[1;31mWARN\033[0m] "$1
}

VERSION=$1
PLATFORM=${2:-all}
export EXTJS_PATH=$(cd ../..; pwd)/ext-6.2.0
PREREQ=true

check_requirement () {
  if ! eval $2 &>/dev/null
  then
	warn "$3"
	PREREQ=false
  else
	ok "$@"
  fi
}

info "Checking build requirements for platform: "$PLATFORM
check_requirement "Traccar server archive" "ls ../target/tracker-server.jar" "Missing traccar archive"
check_requirement "Traccar web interface" "ls ../traccar-web/tools/minify.sh" "Missing traccar-web sources"
check_requirement "Zip" "which zip" "Missing zip binary"
check_requirement "Unzip" "which unzip" "Missing unzip binary"
check_requirement "Ext JS" "ls $EXTJS_PATH" "ExtJS not found in $EXTJS_PATH (https://www.sencha.com/legal/GPL/)"
check_requirement "Sencha Cmd" "which sencha" "Missing Sencha Cmd package (https://www.sencha.com/products/extjs/cmd-download/)"
if [ $PLATFORM != "other" ]; then
  check_requirement "Jlink" "which jlink" "Missing jlink binary (openjdk-10-jdk-headless)"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "windows-64" ]; then
  check_requirement "Inno Extractor" "which innoextract" "Missing innoextract binary"
  check_requirement "Inno Setup" "ls innosetup-*.exe" "Missing Inno Setup (http://www.jrsoftware.org/isdl.php)"
  check_requirement "Windows 64 Java" "ls java-*.windows.x86_64.zip" "Missing Windows 64 Java (https://github.com/ojdkbuild/ojdkbuild)"
  check_requirement "Wine" "which wine" "Missing wine binary"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-64" -o $PLATFORM = "linux-arm" ]; then
  check_requirement "Makeself" "which makeself" "Missing makeself binary"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-64" ]; then
  check_requirement "Linux 64 Java" "ls jdk-*-linux-x64.zip" "Missing Linux 64 Java (https://github.com/ojdkbuild/contrib_jdk10u-ci/releases)"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-arm" ]; then
  check_requirement "Linux ARM Java" "ls jdk-*-linux-armhf.zip" "Missing Linux ARM Java (https://github.com/ojdkbuild/contrib_jdk10u-aarch32-ci/releases)"
fi
if [ $PREREQ = false ]; then
  info "Missing build requirements, aborting..."
  exit 1
else
  info "Building..."
fi

prepare () {
  info "Generating app.min.js"
  ../traccar-web/tools/minify.sh >/dev/null
  ok "Created app.min.js"

  mkdir -p out/{conf,data,lib,logs,web,schema,templates}

  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp ../schema/* out/schema
  cp -r ../templates/* out/templates
  cp -r ../traccar-web/web/* out/web
  cp default.xml out/conf
  cp traccar.xml out/conf

  if [ $PLATFORM = "all" -o $PLATFORM = "windows-64" ]; then
	innoextract innosetup-*.exe >/dev/null
	info "If you got any errors here try Inno Setup version 5.5.5 (or check supported versions using 'innoextract -v')"
  fi
}

cleanup () {
  info "Cleanup"
  rm ../traccar-web/web/app.min.js

  rm -r out
  if [ $PLATFORM = "all" -o $PLATFORM = "windows-64" ]; then
	rm -r tmp
	rm -r app
  fi
}

package_other () {
  info "Building Zip archive"
  cp README.txt out
  cd out
  zip -q -r ../traccar-other-$VERSION.zip *
  cd ..
  rm out/README.txt
  ok "Created Zip archive"
}

package_windows () {
  info "Building Windows 64 installer"
  unzip -q -o java-*.windows.x86_64.zip
  jlink --module-path java-*.windows.x86_64/jmods --add-modules java.se.ee --output out/jre
  rm -rf java-*.windows.x86_64
  wine app/ISCC.exe traccar.iss >/dev/null
  rm -rf out/jre
  zip -q -j traccar-windows-64-$VERSION.zip Output/traccar-setup.exe README.txt
  rm -r Output
  ok "Created Windows 64 installer"
}

package_linux () {
  cp setup.sh out
  cp traccar.service out

  unzip -q -o jdk-*-linux-$1.zip
  jlink --module-path jdk-*-linux-$1/jmods --add-modules java.se.ee --output out/jre
  rm -rf jdk-*-linux-$1
  makeself --quiet --notemp out traccar.run "traccar" ./setup.sh
  rm -rf out/jre

  zip -q -j traccar-linux-$2-$VERSION.zip traccar.run README.txt

  rm traccar.run
  rm out/setup.sh
  rm out/traccar.service
}

package_linux_64 () {
  info "Building Linux 64 installer"
  package_linux x64 64
  ok "Created Linux 64 installer"
}

package_linux_arm () {
  info "Building Linux ARM installer"
  package_linux armhf arm
  ok "Created Linux ARM installer"
}

prepare

case $PLATFORM in
  all)
	package_linux_64
	package_linux_arm
	package_windows
	package_other
	;;

  linux-64)
	package_linux_64
	;;

  linux-arm)
	package_linux_arm
	;;

  windows-64)
	package_windows
	;;

  other)
	package_other
	;;
esac

cleanup

ok "Done"
