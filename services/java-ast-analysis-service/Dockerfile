ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/java-ast-analysis-service/build/libs/java-ast-analysis-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/java-ast-analysis-service

RUN groupadd --system java-ast-analysis \
    && useradd --system --gid java-ast-analysis --home-dir /opt/java-ast-analysis-service --no-create-home java-ast-analysis \
    && mkdir -p /var/lib/forensic-analytics/java-ast-artifacts \
    && chown -R java-ast-analysis:java-ast-analysis /opt/java-ast-analysis-service /var/lib/forensic-analytics

COPY ${SERVICE_JAR} /opt/java-ast-analysis-service/app.jar

USER java-ast-analysis

EXPOSE 9093 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/java-ast-analysis-service/app.jar", "--healthcheck", "--forensics.java-ast-analysis.service.health.host=127.0.0.1", "--forensics.java-ast-analysis.service.health.port=8084"]

ENTRYPOINT ["java", "-jar", "/opt/java-ast-analysis-service/app.jar", "--spring.profiles.active=docker"]
