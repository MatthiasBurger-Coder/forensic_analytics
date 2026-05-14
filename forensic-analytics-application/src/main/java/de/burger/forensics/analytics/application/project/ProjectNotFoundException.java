package de.burger.forensics.analytics.application.project;

public final class ProjectNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException(String message) {
        super(message);
    }
}
