package de.burger.forensics.analytics.domain.semantic;

public record SemanticMethod(
    String methodId,
    String relativePath,
    String fullyQualifiedClassName,
    String methodName,
    String signature,
    int lineNumber
) {
    public SemanticMethod {
        RequiredSemanticText.requireText(methodId, "method id");
        RequiredSemanticText.requireText(relativePath, "relative path");
        RequiredSemanticText.requireText(fullyQualifiedClassName, "fully qualified class name");
        RequiredSemanticText.requireText(methodName, "method name");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("line number must be positive");
        }
    }
}
