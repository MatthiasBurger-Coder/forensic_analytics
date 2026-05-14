package de.burger.forensics.analytics.application.workspace;

public final class WorkspaceAccessDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WorkspaceAccessDeniedException(String message) {
        super(message);
    }
}
