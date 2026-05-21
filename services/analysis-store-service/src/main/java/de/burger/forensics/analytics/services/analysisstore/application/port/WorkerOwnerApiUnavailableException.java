package de.burger.forensics.analytics.services.analysisstore.application.port;

public final class WorkerOwnerApiUnavailableException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public WorkerOwnerApiUnavailableException(String ownerService, String statusCode) {
        super(ownerService + " owner API is unavailable with status " + statusCode);
    }

    public WorkerOwnerApiUnavailableException(String ownerService) {
        super(ownerService + " owner API is not available");
    }
}
