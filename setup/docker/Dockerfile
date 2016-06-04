FROM java:8-alpine

MAINTAINER Danilo Recchia <danilo.recchia@vortus.solutions>

RUN apk upgrade --update && \
    apk add --update curl bash && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /opt/traccar/logs

ENV JAVA_OPTS -Xms256m -Xmx1024m

COPY ./tmp/traccar.xml /opt/traccar/traccar.xml
COPY ./tmp/data /opt/traccar/data
COPY ./tmp/web /opt/traccar/web
COPY ./tmp/traccar-server.jar /opt/traccar/traccar-server.jar
COPY ./files/start.sh /opt/traccar/start.sh

RUN chmod +x /opt/traccar/start.sh

EXPOSE 8082
EXPOSE 5000-5150

ENTRYPOINT /opt/traccar/start.sh
