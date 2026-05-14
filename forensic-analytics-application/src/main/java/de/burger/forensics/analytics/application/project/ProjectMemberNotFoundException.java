package de.burger.forensics.analytics.application.project;

public final class ProjectMemberNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProjectMemberNotFoundException(String message) {
        super(message);
    }
}
