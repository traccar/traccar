FROM openjdk:8-jre-slim
LABEL maintainer="Godwin peter .O <godwin@peter.com.ng>"
ENV TRACCAR_VERSION 4.11
WORKDIR /opt/traccar
COPY setup/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY setup/traccar-other--o.zip /tmp/traccar.zip
RUN set -ex \
    && apt-get update \
    && TERM=xterm DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends unzip \
    && unzip -qo /tmp/traccar.zip -d /opt/traccar \
    && rm /tmp/traccar.zip \
    && apt-get autoremove --yes unzip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/*
ENTRYPOINT ["/entrypoint.sh"]
##docker build -t gpproton/traccar .
##docker login
##docker push gpproton/traccar