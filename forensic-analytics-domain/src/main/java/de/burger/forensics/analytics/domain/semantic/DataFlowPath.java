package de.burger.forensics.analytics.domain.semantic;

import java.util.List;
import java.util.Objects;

public record DataFlowPath(String pathId, String sourceNodeId, String targetNodeId, List<DataFlowStep> steps) {
    public DataFlowPath {
        RequiredSemanticText.requireText(pathId, "path id");
        RequiredSemanticText.requireText(sourceNodeId, "source node id");
        RequiredSemanticText.requireText(targetNodeId, "target node id");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps must not be null"));
    }
}
