package de.burger.forensics.analytics.services.gateway.application;

public final class GatewayRepositoryAnalysisException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String errorCode;
    private final boolean retryable;

    public GatewayRepositoryAnalysisException(int statusCode, String errorCode, boolean retryable, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public int statusCode() {
        return statusCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
