ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/ingestion-service/build/libs/ingestion-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/ingestion-service

RUN groupadd --system ingestion-service \
    && useradd --system --gid ingestion-service --home-dir /opt/ingestion-service --no-create-home ingestion-service \
    && chown -R ingestion-service:ingestion-service /opt/ingestion-service

COPY ${SERVICE_JAR} /opt/ingestion-service/app.jar

USER ingestion-service

EXPOSE 9090 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/ingestion-service/app.jar", "--healthcheck", "--forensics.ingestion.service.health.host=127.0.0.1", "--forensics.ingestion.service.health.port=8081"]

ENTRYPOINT ["java", "-jar", "/opt/ingestion-service/app.jar", "--spring.profiles.active=docker"]
