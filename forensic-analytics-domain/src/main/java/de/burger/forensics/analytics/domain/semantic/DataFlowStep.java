package de.burger.forensics.analytics.domain.semantic;

public record DataFlowStep(String nodeId, int orderIndex, String kind) {
    public DataFlowStep {
        RequiredSemanticText.requireText(nodeId, "node id");
        RequiredSemanticText.requireText(kind, "step kind");
        if (orderIndex < 0) {
            throw new IllegalArgumentException("order index must not be negative");
        }
    }
}
