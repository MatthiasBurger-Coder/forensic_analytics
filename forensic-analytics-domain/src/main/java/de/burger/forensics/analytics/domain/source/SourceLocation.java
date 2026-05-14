package de.burger.forensics.analytics.domain.source;

public record SourceLocation(
    String sourcePath,
    String fullyQualifiedClassName,
    String methodName,
    int lineNumber
) {
    public SourceLocation {
        requireText(sourcePath, "source path");
        requireText(fullyQualifiedClassName, "fully qualified class name");
        requireText(methodName, "method name");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("line number must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
