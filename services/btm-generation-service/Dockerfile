ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/btm-generation-service/build/libs/btm-generation-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/btm-generation-service

RUN groupadd --system btm-generation \
    && useradd --system --gid btm-generation --home-dir /opt/btm-generation-service --no-create-home btm-generation \
    && mkdir -p /var/lib/forensic-analytics/btm-artifacts \
    && chown -R btm-generation:btm-generation /opt/btm-generation-service /var/lib/forensic-analytics

COPY ${SERVICE_JAR} /opt/btm-generation-service/app.jar

USER btm-generation

EXPOSE 9095 8086

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/btm-generation-service/app.jar", "--healthcheck", "--forensics.btm-generation.service.health.host=127.0.0.1", "--forensics.btm-generation.service.health.port=8086"]

ENTRYPOINT ["java", "-jar", "/opt/btm-generation-service/app.jar", "--spring.profiles.active=docker"]
