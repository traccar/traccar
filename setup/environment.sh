#!/usr/bin/env bash

add-apt-repository ppa:openjdk-r/ppa
apt update
apt install openjdk-11-jdk zip unzip innoextract wine makeself

# /usr/bin/printf '\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x00\xe2\x68\x6e\x45\xfb\x43\xdf\xa4\xd9\x92\xdd\x41\xce\xb6\xb2\x1c\x63\x30\xd7\x92' > /etc/ssl/certs/java/cacerts
# /var/lib/dpkg/info/ca-certificates-java.postinst configure

git clone --recurse-submodules https://github.com/traccar/traccar.git
(cd traccar/traccar-web && git checkout master)
(cd traccar && ./gradlew assemble)

wget http://cdn.sencha.com/ext/gpl/ext-6.2.0-gpl.zip
unzip ext-*-gpl.zip ; rm ext-*-gpl.zip

wget http://cdn.sencha.com/cmd/7.1.0.15/no-jre/SenchaCmd-7.1.0.15-linux-i386.sh.zip
unzip SenchaCmd-*.zip ; rm SenchaCmd-*.zip
./SenchaCmd-*.sh -q ; rm SenchaCmd-*
export PATH=$PATH:~/bin/Sencha/Cmd/

cd traccar/setup
wget http://files.jrsoftware.org/is/5/isetup-5.5.6.exe
wget https://github.com/ojdkbuild/ojdkbuild/releases/download/java-11-openjdk-debug-11.0.6.10-1/java-11-openjdk-debug-11.0.6.10-1.windows.ojdkbuild.x86_64.zip
wget https://github.com/ojdkbuild/contrib_jdk11u-ci/releases/download/jdk-11.0.5%2B10/jdk-11.0.5-ojdkbuild-linux-x64.zip
wget https://github.com/ojdkbuild/contrib_jdk11u-arm32-ci/releases/download/jdk-11.0.5%2B10/jdk-11.0.5-ojdkbuild-linux-armhf.zip
