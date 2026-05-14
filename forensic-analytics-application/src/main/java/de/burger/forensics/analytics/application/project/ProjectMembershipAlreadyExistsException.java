package de.burger.forensics.analytics.application.project;

public final class ProjectMembershipAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProjectMembershipAlreadyExistsException(String message) {
        super(message);
    }
}
