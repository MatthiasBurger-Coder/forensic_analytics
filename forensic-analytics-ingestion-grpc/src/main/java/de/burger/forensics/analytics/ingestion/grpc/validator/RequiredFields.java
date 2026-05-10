package de.burger.forensics.analytics.ingestion.grpc.validator;

import com.google.protobuf.ByteString;

final class RequiredFields {
    private RequiredFields() {
    }

    static void nonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
    }

    static void nonEmpty(ByteString value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new ValidationException(fieldName + " must not be empty");
        }
    }

    static void present(boolean value, String fieldName) {
        if (!value) {
            throw new ValidationException(fieldName + " must be present");
        }
    }
}
