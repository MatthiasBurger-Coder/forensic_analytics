ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/forensic-gateway-service/build/libs/forensic-gateway-service-0.1.0-SNAPSHOT.jar
FROM ${JAVA_RUNTIME_IMAGE}

ARG SERVICE_JAR

WORKDIR /opt/forensic-gateway-service

RUN groupadd --system forensic-gateway \
    && useradd --system --gid forensic-gateway --home-dir /opt/forensic-gateway-service --no-create-home forensic-gateway \
    && chown -R forensic-gateway:forensic-gateway /opt/forensic-gateway-service

COPY ${SERVICE_JAR} /opt/forensic-gateway-service/app.jar

USER forensic-gateway

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/forensic-gateway-service/app.jar", "--healthcheck", "--forensics.gateway.service.http.host=127.0.0.1", "--forensics.gateway.service.http.port=8080"]

ENTRYPOINT ["java", "-jar", "/opt/forensic-gateway-service/app.jar", "--spring.profiles.active=docker"]
