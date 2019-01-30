FROM openjdk:8-jre

LABEL maintainer="Godwin peter .O <godwin@peter.com.ng>"

ENV TRACCAR_VERSION 4.2

WORKDIR /opt/traccar

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

COPY ./traccar-other--o.zip /tmp/traccar.zip

RUN set -ex \
    && unzip -qo /tmp/traccar.zip -d /opt/traccar && \
    rm /tmp/traccar.zip

ENTRYPOINT ["/entrypoint.sh"]
