ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
ARG CLI_DISTRIBUTION=cli-client/build/install/cli-client
FROM ${JAVA_RUNTIME_IMAGE}

ARG CLI_DISTRIBUTION

WORKDIR /opt/cli-client

RUN groupadd --system cli-client \
    && useradd --system --gid cli-client --home-dir /opt/cli-client --no-create-home cli-client

COPY ${CLI_DISTRIBUTION}/ /opt/cli-client/

RUN chmod +x /opt/cli-client/bin/cli-client \
    && chown -R cli-client:cli-client /opt/cli-client

USER cli-client

ENTRYPOINT ["/opt/cli-client/bin/cli-client"]
CMD ["--help"]
