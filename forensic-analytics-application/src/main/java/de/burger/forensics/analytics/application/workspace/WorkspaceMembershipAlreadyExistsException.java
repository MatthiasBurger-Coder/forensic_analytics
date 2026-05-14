package de.burger.forensics.analytics.application.workspace;

public final class WorkspaceMembershipAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WorkspaceMembershipAlreadyExistsException(String message) {
        super(message);
    }
}
