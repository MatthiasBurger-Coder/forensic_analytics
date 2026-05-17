ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/forensic-ingestion-service/build/libs/forensic-ingestion-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/forensic-ingestion-service

RUN groupadd --system forensic-ingestion \
    && useradd --system --gid forensic-ingestion --home-dir /opt/forensic-ingestion-service --no-create-home forensic-ingestion \
    && chown -R forensic-ingestion:forensic-ingestion /opt/forensic-ingestion-service

COPY ${SERVICE_JAR} /opt/forensic-ingestion-service/app.jar

USER forensic-ingestion

EXPOSE 9090 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/forensic-ingestion-service/app.jar", "--healthcheck", "--forensics.ingestion.service.health.host=127.0.0.1", "--forensics.ingestion.service.health.port=8081"]

ENTRYPOINT ["java", "-jar", "/opt/forensic-ingestion-service/app.jar", "--spring.profiles.active=docker"]
