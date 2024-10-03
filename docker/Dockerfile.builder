FROM docker.io/library/node:20-bookworm-slim AS node

FROM docker.io/library/debian:bookworm-slim as builder
LABEL maintainer="Godwin peter .O <peter@drolx.com>"
# Import Node.js binaries
COPY --from=node /usr/lib /usr/lib
COPY --from=node /usr/local/share /usr/local/share
COPY --from=node /usr/local/lib /usr/local/lib
COPY --from=node /usr/local/include /usr/local/include
COPY --from=node /usr/local/bin /usr/local/bin

RUN apt update && \
    apt upgrade -y && \
    apt install -y wget unzip zip openjdk-17-jdk

WORKDIR /tmp
# Install sencha tool
RUN wget "https://trials.sencha.com/cmd/7.6.0/SenchaCmd-7.6.0.87-linux-amd64.sh.zip" && \
    unzip SenchaCmd-*.zip && \
    ./SenchaCmd-*.sh -q

## Set environment
ENV HOME=/root
RUN echo '\n' >> $HOME/.bashrc
RUN echo 'export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"' >> $HOME/.bashrc
RUN echo 'export PATH="${PATH}:$JAVA_HOME/bin/"' >> $HOME/.bashrc
RUN echo 'export PATH="${PATH}:$HOME/bin/Sencha/Cmd/"' >> $HOME/.bashrc
ENV PATH="${PATH}:$HOME/bin/Sencha/Cmd/"

RUN rm -rf /tmp/* && apt clean
