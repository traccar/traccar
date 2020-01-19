#!/usr/bin/env bash

apt update
apt install openjdk-11-jdk-headless zip unzip innoextract wine-stable makeself

git clone --recurse-submodules https://github.com/traccar/traccar.git
(cd traccar/traccar-web && git checkout master)

./gradlew assemble

wget http://cdn.sencha.com/ext/gpl/ext-6.2.0-gpl.zip
unzip ext-*-gpl.zip ; rm ext-*-gpl.zip

wget http://cdn.sencha.com/cmd/7.1.0.15/no-jre/SenchaCmd-7.1.0.15-linux-amd64.sh.zip
unzip SenchaCmd-*.zip ; rm SenchaCmd-*.zip
./SenchaCmd-*.sh -q ; rm SenchaCmd-*
export PATH=$PATH:~/bin/Sencha/Cmd/

cd traccar/setup
wget http://files.jrsoftware.org/is/5/isetup-5.5.8.exe
wget https://github.com/ojdkbuild/ojdkbuild/releases/download/java-11-openjdk-debug-11.0.6.10-1/java-11-openjdk-debug-11.0.6.10-1.windows.ojdkbuild.x86_64.zip
wget https://github.com/ojdkbuild/contrib_jdk11u-ci/releases/download/jdk-11.0.5%2B10/jdk-11.0.5-ojdkbuild-linux-x64.zip
wget https://github.com/ojdkbuild/contrib_jdk11u-arm32-ci/releases/download/jdk-11.0.5%2B10/jdk-11.0.5-ojdkbuild-linux-armhf.zip
