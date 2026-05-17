ARG JOERN_BASE_IMAGE=ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295
ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG SERVICE_JAR=services/joern-cpg-analysis-service/build/libs/joern-cpg-analysis-service-0.1.0-SNAPSHOT.jar

FROM ${JAVA_RUNTIME_IMAGE} AS java-runtime
FROM ${JOERN_BASE_IMAGE}

ARG JOERN_BASE_IMAGE
ARG SERVICE_JAR

USER root

COPY --from=java-runtime /opt/java/openjdk /opt/java25

ENV JAVA_HOME=/opt/java25
ENV PATH=/opt/java25/bin:${PATH}
ENV FORENSICS_JOERN_CPG_ANALYSIS_SERVICE_JOERN_RUNTIME_IMAGE_REFERENCE=${JOERN_BASE_IMAGE}

WORKDIR /opt/joern-cpg-analysis-service

RUN mkdir -p /var/lib/forensic-analytics/joern-cpg-workspaces \
    /var/lib/forensic-analytics/joern-cpg-artifacts \
    /opt/forensic-analytics/joern-cpg-analysis/queries \
    && chown -R joern:joern /opt/joern-cpg-analysis-service /var/lib/forensic-analytics /opt/forensic-analytics

COPY ${SERVICE_JAR} /opt/joern-cpg-analysis-service/app.jar

RUN chown joern:joern /opt/joern-cpg-analysis-service/app.jar

USER joern

EXPOSE 9094 8085

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["java", "-jar", "/opt/joern-cpg-analysis-service/app.jar", "--healthcheck", "--forensics.joern-cpg-analysis.service.health.host=127.0.0.1", "--forensics.joern-cpg-analysis.service.health.port=8085"]

ENTRYPOINT ["java", "-jar", "/opt/joern-cpg-analysis-service/app.jar", "--spring.profiles.active=docker"]
