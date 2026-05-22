ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/query-report-api-service/build/libs/query-report-api-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/query-report-api-service

RUN groupadd --system query-report-api \
    && useradd --system --gid query-report-api --home-dir /opt/query-report-api-service --no-create-home query-report-api \
    && chown -R query-report-api:query-report-api /opt/query-report-api-service

COPY ${SERVICE_JAR} /opt/query-report-api-service/app.jar

USER query-report-api

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/query-report-api-service/app.jar", "--healthcheck", "--forensics.query-report-api.service.http.host=127.0.0.1", "--forensics.query-report-api.service.http.port=8080"]

ENTRYPOINT ["java", "-jar", "/opt/query-report-api-service/app.jar", "--spring.profiles.active=docker"]
