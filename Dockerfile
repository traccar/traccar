FROM debian:buster as builder

WORKDIR /build/
COPY setup/environment.sh .
RUN chmod +x environment.sh && ./environment.sh

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

HEALTHCHECK --interval=35s --timeout=5s CMD curl -f http://localhost:8082/ || exit 1
EXPOSE 8082
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-Xms512m", "-Xmx512m", "-Djava.net.preferIPv4Stack=true", "-jar", "tracker-server.jar", "conf/traccar.xml"]
