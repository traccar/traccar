# Pull node to build client
FROM node:18.12.0-alpine as client
COPY ./traccar-web/modern/ .
RUN npm install 
RUN npm run build

# Pull gradle 7  for build time docker
FROM gradle:7.6.0-jdk17 AS server

WORKDIR /home/atmmotors

COPY . .
RUN mkdir -p tmp
RUN ./gradlew assemble


# Pull java 11 runtime for running atmmotors
# FROM openjdk:11.0.11-slim
FROM openjdk:17-jdk-alpine as production
RUN apk upgrade --update && \
    apk add --update curl bash && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /opt/atmmotors/logs && \
    mkdir -p /opt/atmmotors/data &&  \
    mkdir -p /opt/atmmotors/media && \
    mkdir -p /opt/atmmotors/conf

ENV JAVA_OPTS -Xms256m -Xmx1024m

# only copy needed binaries 
COPY --from=server /home/atmmotors/setup/traccar.xml /opt/atmmotors/conf/traccar.xml
COPY --from=server /home/atmmotors/setup/default.xml /opt/atmmotors/conf/default.xml
COPY --from=server /home/atmmotors/schema /opt/atmmotors/schema
COPY --from=server /home/atmmotors/templates /opt/atmmotors/templates
COPY --from=client ./build /opt/atmmotors/modern
COPY --from=server /home/atmmotors/target/lib /opt/atmmotors/lib
COPY --from=server /home/atmmotors/target/tracker-server.jar /opt/atmmotors/atmmotors-server.jar

EXPOSE 8082
EXPOSE 5000-5150

VOLUME /opt/atmmotors/logs
VOLUME /opt/atmmotors/data
VOLUME /opt/atmmotors/media
VOLUME /opt/atmmotors/conf
VOLUME /opt/atmmotors/web

WORKDIR /opt/atmmotors

ENTRYPOINT ["java", "-Xms1g", "-Xmx1g", "-Djava.net.preferIPv4Stack=true"]

CMD ["-jar", "atmmotors-server.jar", "conf/traccar.xml"]