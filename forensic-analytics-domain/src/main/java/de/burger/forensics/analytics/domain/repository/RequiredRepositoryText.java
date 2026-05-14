package de.burger.forensics.analytics.domain.repository;

final class RequiredRepositoryText {
    private RequiredRepositoryText() {
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
