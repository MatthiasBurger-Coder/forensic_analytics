package de.burger.forensics.analytics.application.workspace;

public final class WorkspaceMemberNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WorkspaceMemberNotFoundException(String message) {
        super(message);
    }
}
