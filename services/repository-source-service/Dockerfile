ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/repository-source-service/build/libs/repository-source-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/repository-source-service

RUN groupadd --system repository-source \
    && useradd --system --gid repository-source --home-dir /opt/repository-source-service --no-create-home repository-source \
    && mkdir -p /var/lib/forensic-analytics/repository-workspaces /var/lib/forensic-analytics/repository-source-data \
    && chown -R repository-source:repository-source /opt/repository-source-service /var/lib/forensic-analytics

COPY ${SERVICE_JAR} /opt/repository-source-service/app.jar

USER repository-source

EXPOSE 9092 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/repository-source-service/app.jar", "--healthcheck", "--forensics.repository-source.service.health.host=127.0.0.1", "--forensics.repository-source.service.health.port=8083"]

ENTRYPOINT ["java", "-jar", "/opt/repository-source-service/app.jar", "--spring.profiles.active=docker"]
