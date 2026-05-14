package de.burger.forensics.analytics.domain.semantic;

import java.util.List;
import java.util.Objects;

public record SemanticGraph(
    List<SemanticNode> nodes,
    List<SemanticEdge> edges,
    List<SemanticMethod> methods,
    List<CallRelation> callRelations,
    List<ControlFlowRelation> controlFlowRelations,
    List<DataFlowPath> dataFlowPaths,
    List<SemanticAnchor> anchors
) {
    public SemanticGraph {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods must not be null"));
        callRelations = List.copyOf(Objects.requireNonNull(callRelations, "callRelations must not be null"));
        controlFlowRelations = List.copyOf(Objects.requireNonNull(
            controlFlowRelations,
            "controlFlowRelations must not be null"
        ));
        dataFlowPaths = List.copyOf(Objects.requireNonNull(dataFlowPaths, "dataFlowPaths must not be null"));
        anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors must not be null"));
    }

    public static SemanticGraph empty() {
        return new SemanticGraph(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
