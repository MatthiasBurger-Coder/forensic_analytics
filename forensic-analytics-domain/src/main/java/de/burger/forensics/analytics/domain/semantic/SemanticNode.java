package de.burger.forensics.analytics.domain.semantic;

public record SemanticNode(
    String nodeId,
    String nodeType,
    String relativePath,
    String fullyQualifiedClassName,
    String methodName,
    String signature,
    int lineNumber,
    String normalizedCode
) {
    public SemanticNode {
        RequiredSemanticText.requireText(nodeId, "node id");
        RequiredSemanticText.requireText(nodeType, "node type");
        RequiredSemanticText.requireText(relativePath, "relative path");
        RequiredSemanticText.requireText(methodName, "method name");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("line number must be positive");
        }
        normalizedCode = normalizedCode == null ? "" : normalizedCode;
    }
}
