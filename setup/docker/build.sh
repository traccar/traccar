#!/bin/bash

which mvn &> /dev/null || { echo >&2 "Maven package cant be found on path. Aborting."; exit 1; }
which awk &> /dev/null  || { echo >&2 "Awk package cant be found on path. Aborting."; exit 1; }
which docker &> /dev/null  || { echo >&2 "Docker package cant be found on path. Aborting."; exit 1; }
mvn package || { echo >&2 "Maven package has failed. Aborting."; exit 1; }

export company="tananaev"
export software="traccar"
export version=$(head -n 10 ./pom.xml |grep version|cut -d ">" -f2|cut -d"<" -f1)

tmp="./setup/docker/tmp"

mkdir -p ${tmp}

cat ./setup/traccar.xml | awk '/web.path/ && !modif { printf("    <entry key=\"web.debug\">true</entry>\n"); modif=1 } {print}' > ${tmp}/traccar.xml
cp -rf ./setup/default.xml ${tmp}
cp -rf ./schema ${tmp}/schema
cp -rf ./target/tracker-server.jar ${tmp}/traccar-server.jar
cp -rf ./target/lib ${tmp}/lib
cp -rf ./traccar-web/web ${tmp}/web

docker build -t ${company}/${software}:${version} ./setup/docker/

rm -rf ${tmp}
