package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

final class RequiredFields {
    private RequiredFields() {
    }

    static void nonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
    }

    static void present(boolean present, String fieldName) {
        if (!present) {
            throw new ValidationException(fieldName + " must be present");
        }
    }

    static void positive(int value, String fieldName) {
        if (value < 1) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    static void percent(int value, String fieldName) {
        if (value < 0 || value > 100) {
            throw new ValidationException(fieldName + " must be between 0 and 100");
        }
    }
}
