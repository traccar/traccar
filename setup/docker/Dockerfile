FROM java:8-alpine

MAINTAINER Danilo Recchia <danilo.recchia@vortus.solutions>

USER root

RUN apk upgrade --update && \
    apk add --update curl bash && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /opt/traccar/logs && \
    mkdir -p /opt/traccar/data

ENV JAVA_OPTS -Xms256m -Xmx1024m

COPY ./tmp/traccar.xml /opt/traccar/traccar.xml
COPY ./tmp/default.xml /opt/traccar/conf/default.xml
COPY ./tmp/schema /opt/traccar/schema
COPY ./tmp/templates /opt/traccar/templates
COPY ./tmp/web /opt/traccar/web
COPY ./tmp/lib /opt/traccar/lib
COPY ./tmp/traccar-server.jar /opt/traccar/traccar-server.jar

EXPOSE 8082
EXPOSE 5000-5150

WORKDIR /opt/traccar

ENTRYPOINT ["java","-jar","traccar-server.jar","traccar.xml"]
