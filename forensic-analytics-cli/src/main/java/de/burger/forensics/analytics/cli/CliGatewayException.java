package de.burger.forensics.analytics.cli;

final class CliGatewayException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    CliGatewayException(String message) {
        super(message);
    }
}
