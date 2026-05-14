package de.burger.forensics.analytics.domain.semantic;

final class RequiredSemanticText {
    private RequiredSemanticText() {
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
