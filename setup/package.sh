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
check_requirement "Zip" "which zip" "Missing zip binary"
check_requirement "Unzip" "which unzip" "Missing unzip binary"
if [ $PLATFORM != "other" ]; then
  check_requirement "Jlink" "which jlink" "Missing jlink binary"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "windows-64" ]; then
  check_requirement "Inno Extractor" "which innoextract" "Missing innoextract binary"
  check_requirement "Inno Setup" "ls i*setup-*.exe" "Missing Inno Setup (http://www.jrsoftware.org/isdl.php)"
  check_requirement "Windows 64 Java" "ls OpenJDK*64_windows*.zip" "Missing Windows 64 JDK (https://adoptium.net/)"
  check_requirement "Wine" "which wine" "Missing wine binary"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-64" -o $PLATFORM = "linux-arm" ]; then
  check_requirement "Makeself" "which makeself" "Missing makeself binary"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-64" ]; then
  check_requirement "Linux 64 Java" "ls OpenJDK*x64_linux*.tar.gz" "Missing Linux 64 JDK (https://adoptium.net/)"
fi
if [ $PLATFORM = "all" -o $PLATFORM = "linux-arm" ]; then
  check_requirement "Linux ARM Java" "ls OpenJDK*aarch64_linux*.tar.gz" "Missing Linux ARM JDK (https://adoptium.net/)"
fi
if [ $PREREQ = false ]; then
  info "Missing build requirements, aborting..."
  exit 1
else
  info "Building..."
fi

prepare () {
  mkdir -p out/{conf,data,lib,logs,web,schema,templates}

  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp ../schema/* out/schema
  cp -r ../templates/* out/templates
  cp -r ../traccar-web/build/* out/web
  cp default.xml out/conf
  cp traccar.xml out/conf

  if [ $PLATFORM = "all" -o $PLATFORM = "windows-64" ]; then
	innoextract i*setup-*.exe >/dev/null
	info "If you got any errors here try Inno Setup version 5.5.5 (or check supported versions using 'innoextract -v')"
  fi
}

cleanup () {
  info "Cleanup"
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
  unzip -q OpenJDK*64_windows*.zip
  jlink --module-path jdk-*/jmods --add-modules java.se,jdk.charsets,jdk.crypto.ec,jdk.unsupported --output out/jre
  rm -rf jdk-*
  wine app/ISCC.exe traccar.iss >/dev/null
  rm -rf out/jre
  zip -q -j traccar-windows-64-$VERSION.zip Output/traccar-setup.exe README.txt
  rm -r Output
  ok "Created Windows 64 installer"
}

package_linux () {
  cp setup.sh out
  cp traccar.service out

  tar -xf OpenJDK*$2_linux*.tar.gz
  jlink --module-path jdk-*/jmods --add-modules java.se,jdk.charsets,jdk.crypto.ec,jdk.unsupported --output out/jre
  rm -rf jdk-*
  makeself --needroot --quiet --notemp out traccar.run "traccar" ./setup.sh
  rm -rf out/jre

  zip -q -j traccar-linux-$1-$VERSION.zip traccar.run README.txt

  rm traccar.run
  rm out/setup.sh
  rm out/traccar.service
}

package_linux_64 () {
  info "Building Linux 64 installer"
  package_linux 64 x64
  ok "Created Linux 64 installer"
}

package_linux_arm () {
  info "Building Linux ARM installer"
  package_linux arm aarch64
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
