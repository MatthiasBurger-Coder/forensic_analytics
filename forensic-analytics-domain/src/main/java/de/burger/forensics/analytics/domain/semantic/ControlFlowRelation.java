package de.burger.forensics.analytics.domain.semantic;

public record ControlFlowRelation(String sourceNodeId, String targetNodeId, String relationType) {
    public ControlFlowRelation {
        RequiredSemanticText.requireText(sourceNodeId, "source node id");
        RequiredSemanticText.requireText(targetNodeId, "target node id");
        RequiredSemanticText.requireText(relationType, "relation type");
    }
}
