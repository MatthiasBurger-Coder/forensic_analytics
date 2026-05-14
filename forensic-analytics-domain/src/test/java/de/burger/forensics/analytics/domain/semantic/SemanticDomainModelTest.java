package de.burger.forensics.analytics.domain.semantic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticDomainModelTest {
    @Test
    void storesSemanticGraphAsImmutableEvidenceLists() {
        var nodes = new ArrayList<>(List.of(node()));

        var graph = new SemanticGraph(
            nodes,
            List.of(edge()),
            List.of(method()),
            List.of(callRelation()),
            List.of(controlFlowRelation()),
            List.of(dataFlowPath()),
            List.of(anchor())
        );
        nodes.clear();

        assertEquals(List.of(node()), graph.nodes());
        assertEquals(List.of(edge()), graph.edges());
        assertEquals(List.of(method()), graph.methods());
        assertEquals(List.of(callRelation()), graph.callRelations());
        assertEquals(List.of(controlFlowRelation()), graph.controlFlowRelations());
        assertEquals(List.of(dataFlowPath()), graph.dataFlowPaths());
        assertEquals(List.of(anchor()), graph.anchors());
    }

    @Test
    void createsEmptySemanticGraph() {
        assertEquals(
            new SemanticGraph(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
            SemanticGraph.empty()
        );
    }

    @Test
    void normalizesOptionalCodeText() {
        var node = new SemanticNode("node-1", "CALL", "App.java", null, "main", null, 1, null);
        var anchor = new SemanticAnchor("scan-1", "node-1", "App.java", null, "main", null, 1, null, 1.0d, "line-match");

        assertEquals("", node.normalizedCode());
        assertEquals("", anchor.normalizedCode());
    }

    @Test
    void rejectsInvalidSemanticGraphLists() {
        assertThrows(NullPointerException.class, () -> new SemanticGraph(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), null, List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), List.of(), List.of(), List.of(), List.of(), null, List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticGraph(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null));
    }

    @Test
    void rejectsInvalidSemanticFacts() {
        assertThrows(IllegalArgumentException.class, () -> new SemanticNode(null, "CALL", "App.java", null, "main", null, 1, "call"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticNode("node-1", "", "App.java", null, "main", null, 1, "call"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticNode("node-1", "CALL", "", null, "main", null, 1, "call"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticNode("node-1", "CALL", "App.java", null, "", null, 1, "call"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticNode("node-1", "CALL", "App.java", null, "main", null, 0, "call"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticEdge("", "node-1", "node-2", "AST"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticEdge("edge-1", "", "node-2", "AST"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticEdge("edge-1", "node-1", "", "AST"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticEdge("edge-1", "node-1", "node-2", ""));
        assertThrows(IllegalArgumentException.class, () -> new SemanticMethod("", "App.java", "com.example.App", "main", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticMethod("method-1", "", "com.example.App", "main", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticMethod("method-1", "App.java", "", "main", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticMethod("method-1", "App.java", "com.example.App", "", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticMethod("method-1", "App.java", "com.example.App", "main", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CallRelation("", "method-2", "node-1"));
        assertThrows(IllegalArgumentException.class, () -> new CallRelation("method-1", "", "node-1"));
        assertThrows(IllegalArgumentException.class, () -> new CallRelation("method-1", "method-2", ""));
        assertThrows(IllegalArgumentException.class, () -> new ControlFlowRelation("", "node-2", "FALLTHROUGH"));
        assertThrows(IllegalArgumentException.class, () -> new ControlFlowRelation("node-1", "", "FALLTHROUGH"));
        assertThrows(IllegalArgumentException.class, () -> new ControlFlowRelation("node-1", "node-2", ""));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowStep("", 0, "argument"));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowStep("node-1", -1, "argument"));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowStep("node-1", 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowPath("", "node-1", "node-2", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowPath("path-1", "", "node-2", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DataFlowPath("path-1", "node-1", "", List.of()));
        assertThrows(NullPointerException.class, () -> new DataFlowPath("path-1", "node-1", "node-2", null));
    }

    @Test
    void rejectsInvalidSemanticAnchors() {
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("", "node-1", "App.java", null, "main", null, 1, "call", 0.9d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "", "App.java", null, "main", null, 1, "call", 0.9d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "", null, "main", null, 1, "call", 0.9d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "App.java", null, "", null, 1, "call", 0.9d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "App.java", null, "main", null, 0, "call", 0.9d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "App.java", null, "main", null, 1, "call", -0.1d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "App.java", null, "main", null, 1, "call", 1.1d, "line-match"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticAnchor("scan-1", "node-1", "App.java", null, "main", null, 1, "call", 0.9d, ""));
    }

    private static SemanticNode node() {
        return new SemanticNode("node-1", "CALL", "App.java", "com.example.App", "main", "main()", 12, "helper()");
    }

    private static SemanticEdge edge() {
        return new SemanticEdge("edge-1", "node-1", "node-2", "AST");
    }

    private static SemanticMethod method() {
        return new SemanticMethod("method-1", "App.java", "com.example.App", "main", "main()", 10);
    }

    private static CallRelation callRelation() {
        return new CallRelation("method-1", "method-2", "node-1");
    }

    private static ControlFlowRelation controlFlowRelation() {
        return new ControlFlowRelation("node-1", "node-2", "FALLTHROUGH");
    }

    private static DataFlowPath dataFlowPath() {
        return new DataFlowPath("path-1", "node-1", "node-2", List.of(new DataFlowStep("node-1", 0, "argument")));
    }

    private static SemanticAnchor anchor() {
        return new SemanticAnchor("scan-1", "node-1", "App.java", "com.example.App", "main", "main()", 12, "helper()", 0.9d, "line-match");
    }
}
