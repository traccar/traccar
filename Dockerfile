FROM debian:buster as builder

WORKDIR /build/

RUN apt update && \
    apt install -y sudo curl git \
    dirmngr apt-transport-https zip wget unzip && \
    curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash - && \
    dpkg --add-architecture i386

RUN apt update &&  \
    apt install -y default-jdk innoextract wine wine32 makeself nodejs && \
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 && \
    export PATH=$PATH:$JAVA_HOME/bin && \
    echo $PATH

RUN git clone --recurse-submodules https://github.com/elevenClouds/traccar.git && \
    (cd traccar/traccar-web && git checkout build) && \
    (cd traccar && git checkout build && ./gradlew build && sleep 10) && \
    wget http://cdn.sencha.com/ext/gpl/ext-6.2.0-gpl.zip && \
    unzip ext-*-gpl.zip ; rm ext-*-gpl.zip && \
    wget http://cdn.sencha.com/cmd/7.1.0.15/no-jre/SenchaCmd-7.1.0.15-linux-i386.sh.zip && \
    unzip SenchaCmd-*.zip ; rm SenchaCmd-*.zip && \
    ./SenchaCmd-*.sh -q ; rm SenchaCmd-* && \
    export PATH=$PATH:~/bin/Sencha/Cmd/

RUN (cd traccar/traccar-web && ./tools/package.sh) && \
    cd traccar/setup && \
    wget https://github.com/ojdkbuild/contrib_jdk11u-ci/releases/download/jdk-11.0.8%2B10/jdk-11.0.8-ojdkbuild-linux-x64.zip && \
    ./package.sh -o other

FROM openjdk:11-jre-slim as runtime
LABEL maintainer="Godwin peter .O <godwin@peter.com.ng>"
ENV TRACCAR_VERSION 4.13

WORKDIR /opt/traccar
COPY --from=builder /build/traccar/setup/traccar-other--o.zip /tmp/traccar.zip
COPY setup/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN set -ex \
    && apt-get update && apt-get upgrade -y \
    && TERM=xterm DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends unzip \
    && unzip -qo /tmp/traccar.zip -d /opt/traccar \
    && rm /tmp/traccar.zip \
    && apt-get autoremove --yes unzip curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/*

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-Xms512m", "-Xmx512m", "-Djava.net.preferIPv4Stack=true", "-jar", "tracker-server.jar", "conf/traccar.xml"]
