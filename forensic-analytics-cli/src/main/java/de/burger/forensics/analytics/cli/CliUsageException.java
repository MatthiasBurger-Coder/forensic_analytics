package de.burger.forensics.analytics.cli;

final class CliUsageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    CliUsageException(String message) {
        super(message);
    }
}
