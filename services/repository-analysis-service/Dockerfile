ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/repository-analysis-service/build/libs/repository-analysis-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/repository-analysis-service

RUN groupadd --system repository-analysis \
    && useradd --system --gid repository-analysis --home-dir /opt/repository-analysis-service --no-create-home repository-analysis \
    && mkdir -p /var/lib/forensic-analytics/repository-workspaces \
    && chown -R repository-analysis:repository-analysis /opt/repository-analysis-service /var/lib/forensic-analytics

COPY ${SERVICE_JAR} /opt/repository-analysis-service/app.jar

USER repository-analysis

EXPOSE 9092 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/repository-analysis-service/app.jar", "--healthcheck", "--forensics.repository-analysis.service.health.host=127.0.0.1", "--forensics.repository-analysis.service.health.port=8083"]

ENTRYPOINT ["java", "-jar", "/opt/repository-analysis-service/app.jar", "--spring.profiles.active=docker"]
