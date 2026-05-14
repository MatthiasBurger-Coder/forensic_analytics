ARG JOERN_BASE_IMAGE=ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295
FROM ${JOERN_BASE_IMAGE}

USER root

RUN mkdir -p /analysis/input \
    /analysis/workspace \
    /analysis/output \
    /analysis/logs \
    /opt/forensic-analytics/joern/scripts

COPY scripts/*.sh /opt/forensic-analytics/joern/scripts/

RUN chmod +x /opt/forensic-analytics/joern/scripts/*.sh \
    && chown -R joern:joern /analysis /opt/forensic-analytics/joern

ENV ANALYSIS_INPUT_DIR=/analysis/input
ENV JOERN_WORKSPACE_DIR=/analysis/workspace
ENV ANALYSIS_OUTPUT_DIR=/analysis/output
ENV ANALYSIS_LOG_DIR=/analysis/logs
ENV JOERN_HEAP=8G

USER joern
WORKDIR /analysis

ENTRYPOINT ["/opt/forensic-analytics/joern/scripts/joern-entrypoint.sh"]
CMD ["joern", "--help"]
