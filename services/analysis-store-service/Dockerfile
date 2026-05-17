ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/analysis-store-service/build/libs/analysis-store-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/analysis-store-service

RUN groupadd --system analysis-store \
    && useradd --system --gid analysis-store --home-dir /opt/analysis-store-service --no-create-home analysis-store \
    && chown -R analysis-store:analysis-store /opt/analysis-store-service

COPY ${SERVICE_JAR} /opt/analysis-store-service/app.jar

USER analysis-store

EXPOSE 9091 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/analysis-store-service/app.jar", "--healthcheck", "--forensics.analysis-store.service.health.host=127.0.0.1", "--forensics.analysis-store.service.health.port=8082"]

ENTRYPOINT ["java", "-jar", "/opt/analysis-store-service/app.jar", "--spring.profiles.active=docker"]
