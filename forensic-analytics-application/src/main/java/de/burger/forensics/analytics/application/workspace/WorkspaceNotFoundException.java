package de.burger.forensics.analytics.application.workspace;

public final class WorkspaceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WorkspaceNotFoundException(String message) {
        super(message);
    }
}
