package de.burger.forensics.analytics.services.ingestion.domain;

final class RequiredText {
    private RequiredText() {
    }

    static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
