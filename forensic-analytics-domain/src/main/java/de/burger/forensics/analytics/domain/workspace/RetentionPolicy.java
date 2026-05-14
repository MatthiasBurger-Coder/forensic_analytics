package de.burger.forensics.analytics.domain.workspace;

public record RetentionPolicy(int retentionDays) {
    public RetentionPolicy {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
    }
}
