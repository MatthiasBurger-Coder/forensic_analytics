package de.burger.forensics.analytics.domain.analysis;

final class RequiredAnalysisText {
    private RequiredAnalysisText() {
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
