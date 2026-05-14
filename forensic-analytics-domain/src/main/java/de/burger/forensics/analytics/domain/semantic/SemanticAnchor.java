package de.burger.forensics.analytics.domain.semantic;

public record SemanticAnchor(
    String scanEventKey,
    String semanticNodeId,
    String relativePath,
    String fullyQualifiedClassName,
    String methodName,
    String signature,
    int lineNumber,
    String normalizedCode,
    double confidence,
    String matchStrategy
) {
    public SemanticAnchor {
        RequiredSemanticText.requireText(scanEventKey, "scan event key");
        RequiredSemanticText.requireText(semanticNodeId, "semantic node id");
        RequiredSemanticText.requireText(relativePath, "relative path");
        RequiredSemanticText.requireText(methodName, "method name");
        RequiredSemanticText.requireText(matchStrategy, "match strategy");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("line number must be positive");
        }
        if (confidence < 0.0d || confidence > 1.0d) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        normalizedCode = normalizedCode == null ? "" : normalizedCode;
    }
}
