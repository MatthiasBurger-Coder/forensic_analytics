ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG BOOT_APP_JAR=forensic-analytics-boot-app/build/libs/forensic-analytics-boot-app-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG BOOT_APP_JAR

WORKDIR /opt/forensic-analytics

RUN groupadd --system forensic-analytics \
    && useradd --system --gid forensic-analytics --home-dir /opt/forensic-analytics --no-create-home forensic-analytics \
    && mkdir -p /var/lib/forensic-analytics/workspaces \
    && chown -R forensic-analytics:forensic-analytics /opt/forensic-analytics /var/lib/forensic-analytics

COPY ${BOOT_APP_JAR} /opt/forensic-analytics/app.jar

USER forensic-analytics

VOLUME ["/var/lib/forensic-analytics/workspaces"]
EXPOSE 8080 9090

ENTRYPOINT ["java", "-jar", "/opt/forensic-analytics/app.jar", "--spring.profiles.active=docker"]
