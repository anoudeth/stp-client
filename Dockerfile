FROM amazoncorretto:21.0.2-alpine AS corretto-jdk

#--- required for strip-debug to work
RUN apk add --no-cache binutils

#--- Build minimal JRE with only required modules
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.logging,java.xml,java.xml.crypto,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

#--- main app image
FROM alpine:3.20

#--- Timezone
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Bangkok /etc/localtime && \
    echo "Asia/Bangkok" > /etc/timezone && \
    apk del tzdata

ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

#--- copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME

WORKDIR /usr/apps
COPY target/*.jar ./
RUN mkdir ./config_props
COPY src/main/resources/* ./config_props
COPY key/ ./key/

ENTRYPOINT ["java", "-jar", "stp-client.jar", "--spring.config.location=./config_props/application.yml"]