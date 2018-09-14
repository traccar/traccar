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

check_requirement "ls ../../ext-6.2.0" "Missing ../../ext-6.2.0 (https://www.sencha.com/legal/GPL/)"
check_requirement "ls innosetup-*.exe" "Missing isetup-*.exe (http://www.jrsoftware.org/isdl.php)"
check_requirement "which sencha" "Missing sencha cmd package (https://www.sencha.com/products/extjs/cmd-download/)"
check_requirement "which wine" "Missing wine package"
check_requirement "which innoextract" "Missing innoextract package"
check_requirement "which makeself" "Missing makeself package"

prepare () {
  ../traccar-web/tools/minify.sh

  innoextract innosetup-*.exe
  echo "If you got any errors here try isetup version 5.5.5 (or check supported versions using 'innoextract -v')"
}

cleanup () {
  rm ../traccar-web/web/app.min.js

  rm -r app/
}

copy_files () {
  cp ../target/tracker-server.jar out
  cp ../target/lib/* out/lib
  cp ../schema/* out/schema
  cp -r ../templates/* out/templates
  cp -r ../traccar-web/web/* out/web
  cp default.xml out/conf
  cp traccar.xml out/conf
}

package_windows () {
  mkdir -p out/{conf,data,lib,logs,web,schema,templates}

  copy_files

  wine app/ISCC.exe traccar.iss

  zip -j traccar-windows-$VERSION.zip Output/traccar-setup.exe README.txt

  rm -r Output
  rm -r tmp
  rm -r out
}

package_unix () {
  mkdir -p out/{conf,data,lib,logs,web,schema,templates}
  copy_files

  cp java-test/test.jar out
  cp setup.sh out
  cp other/traccar.service out
  makeself --notemp out traccar.run "traccar" ./setup.sh

  zip -j traccar-linux-$VERSION.zip traccar.run README.txt

  rm traccar.run
  rm -r out
}

package_universal () {
  mkdir -p out/{conf,data,lib,logs,web,schema,templates}

  copy_files

  cp README.txt out

  cd out
  zip -r ../traccar-other-$VERSION.zip *
  cd ..

  rm -rf out/
}

prepare

package_windows
package_unix
package_universal

cleanup
