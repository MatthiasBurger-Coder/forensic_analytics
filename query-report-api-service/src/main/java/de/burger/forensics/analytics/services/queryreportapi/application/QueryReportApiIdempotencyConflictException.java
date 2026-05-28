package de.burger.forensics.analytics.services.queryreportapi.application;

public final class QueryReportApiIdempotencyConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public QueryReportApiIdempotencyConflictException(String message) {
        super(message);
    }
}
