ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/analysis-orchestrator-service/build/libs/analysis-orchestrator-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/analysis-orchestrator-service

RUN groupadd --system analysis-orchestrator \
    && useradd --system --gid analysis-orchestrator --home-dir /opt/analysis-orchestrator-service --no-create-home analysis-orchestrator \
    && chown -R analysis-orchestrator:analysis-orchestrator /opt/analysis-orchestrator-service

COPY ${SERVICE_JAR} /opt/analysis-orchestrator-service/app.jar

USER analysis-orchestrator

EXPOSE 9098 8089

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/analysis-orchestrator-service/app.jar", "--healthcheck", "--forensics.analysis-orchestrator.service.health.host=127.0.0.1", "--forensics.analysis-orchestrator.service.health.port=8089"]

ENTRYPOINT ["java", "-jar", "/opt/analysis-orchestrator-service/app.jar", "--spring.profiles.active=docker"]
