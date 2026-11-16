# Image built from the staged package in deploy/out (see build.sh).
# Built on the target host, so it is native to its architecture (arm64 on the Pi).

FROM eclipse-temurin:25 AS jdk
RUN jlink --add-modules java.se,jdk.charsets,jdk.crypto.ec,jdk.net,jdk.unsupported \
    --strip-debug --no-header-files --no-man-pages --compress=2 --output /jre

FROM ubuntu:24.04
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
COPY out /opt/traccar
COPY --from=jdk /jre /opt/traccar/jre
RUN mkdir -p /opt/traccar/data /opt/traccar/logs
WORKDIR /opt/traccar
ENTRYPOINT ["/opt/traccar/jre/bin/java", "-XX:+ExitOnOutOfMemoryError"]
CMD ["-jar", "tracker-server.jar", "conf/traccar.xml"]
