package de.burger.forensics.analytics.domain.workspace;

final class RequiredWorkspaceText {
    private RequiredWorkspaceText() {
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
