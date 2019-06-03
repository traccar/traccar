FROM openjdk:8-jre-slim

LABEL maintainer="Godwin peter .O <godwin@peter.com.ng>"

ENV TRACCAR_VERSION 4.5

WORKDIR /opt/traccar

COPY setup/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

COPY setup/traccar-other--o.zip /tmp/traccar.zip

RUN set -ex && \
    unzip -qo /tmp/traccar.zip -d /opt/traccar && \
    rm /tmp/traccar.zip && \
    rm -rf /var/lib/apt/lists/* /tmp/*

ENTRYPOINT ["/entrypoint.sh"]
