package de.burger.forensics.analytics.domain.semantic;

public record SemanticEdge(String edgeId, String sourceNodeId, String targetNodeId, String edgeType) {
    public SemanticEdge {
        RequiredSemanticText.requireText(edgeId, "edge id");
        RequiredSemanticText.requireText(sourceNodeId, "source node id");
        RequiredSemanticText.requireText(targetNodeId, "target node id");
        RequiredSemanticText.requireText(edgeType, "edge type");
    }
}
