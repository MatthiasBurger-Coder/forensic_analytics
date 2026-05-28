ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=java-parser-analysis-service/build/libs/java-parser-analysis-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/java-parser-analysis-service

RUN groupadd --system java-parser-analysis \
    && useradd --system --gid java-parser-analysis --home-dir /opt/java-parser-analysis-service --no-create-home java-parser-analysis \
    && mkdir -p /var/lib/forensic-analytics/java-parser-analysis-artifacts \
    && chown -R java-parser-analysis:java-parser-analysis /opt/java-parser-analysis-service /var/lib/forensic-analytics

COPY ${SERVICE_JAR} /opt/java-parser-analysis-service/app.jar

USER java-parser-analysis

EXPOSE 9094 8085

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/java-parser-analysis-service/app.jar", "--healthcheck", "--forensics.java-parser-analysis.service.health.host=127.0.0.1", "--forensics.java-parser-analysis.service.health.port=8085"]

ENTRYPOINT ["java", "-jar", "/opt/java-parser-analysis-service/app.jar", "--spring.profiles.active=docker"]
