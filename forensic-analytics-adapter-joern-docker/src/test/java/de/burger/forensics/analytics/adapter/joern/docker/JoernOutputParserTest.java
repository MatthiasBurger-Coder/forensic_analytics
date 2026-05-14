package de.burger.forensics.analytics.adapter.joern.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JoernOutputParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesJoernArtifactsIntoSemanticGraph() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        writeArtifacts(paths);

        var graph = new JoernOutputParser().parse(paths);

        assertEquals(1, graph.nodes().size());
        assertEquals("call-node-1", graph.nodes().getFirst().nodeId());
        assertEquals("CALL", graph.nodes().getFirst().nodeType());
        assertEquals("src/main/java/com/example/App.java", graph.nodes().getFirst().relativePath());
        assertEquals("com.example.App", graph.nodes().getFirst().fullyQualifiedClassName());
        assertEquals("greet", graph.nodes().getFirst().methodName());
        assertEquals("java.lang.String greet(java.lang.String)", graph.nodes().getFirst().signature());
        assertEquals(6, graph.nodes().getFirst().lineNumber());
        assertEquals("helper(name)", graph.nodes().getFirst().normalizedCode());
        assertEquals("edge-1", graph.edges().getFirst().edgeId());
        assertEquals("method-1", graph.methods().getFirst().methodId());
        assertEquals("method-1", graph.callRelations().getFirst().callerMethodId());
        assertEquals("method-2", graph.callRelations().getFirst().calleeMethodId());
        assertEquals("NEXT", graph.controlFlowRelations().getFirst().relationType());
        assertEquals("path-1", graph.dataFlowPaths().getFirst().pathId());
        assertEquals(2, graph.dataFlowPaths().getFirst().steps().size());
        assertEquals("argument", graph.dataFlowPaths().getFirst().steps().getFirst().kind());
        assertEquals("scan-1", graph.anchors().getFirst().scanEventKey());
        assertEquals(0.95d, graph.anchors().getFirst().confidence());
        assertEquals("signature-and-line", graph.anchors().getFirst().matchStrategy());
    }

    @Test
    void missingArtifactsProduceEmptyGraph() {
        var graph = new JoernOutputParser().parse(JoernDockerArtifactPaths.under(tempDir));

        assertEquals(0, graph.nodes().size());
        assertEquals(0, graph.edges().size());
        assertEquals(0, graph.methods().size());
        assertEquals(0, graph.callRelations().size());
        assertEquals(0, graph.controlFlowRelations().size());
        assertEquals(0, graph.dataFlowPaths().size());
        assertEquals(0, graph.anchors().size());
    }

    @Test
    void malformedArraysProduceEmptyGraphInsteadOfInventedFacts() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(paths.callgraph(), "{\"nodes\": true}", StandardCharsets.UTF_8);

        var graph = new JoernOutputParser().parse(paths);

        assertEquals(0, graph.nodes().size());
    }

    @Test
    void truncatedArrayKeepsOnlyCompletedObjects() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.callgraph(),
            """
            {
              "nodes": [
                {
                  "id": "node-1",
                  "type": "CALL",
                  "file": "App.java",
                  "method": "main",
                  "line": 1
                },
                {
              ]
            }
            """,
            StandardCharsets.UTF_8
        );

        var graph = new JoernOutputParser().parse(paths);

        assertEquals(1, graph.nodes().size());
        assertEquals("node-1", graph.nodes().getFirst().nodeId());
    }

    @Test
    void escapedTextFieldsAreDecoded() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.callgraph(),
            """
            {
              "nodes": [
                {
                  "id": "node-1",
                  "type": "CALL",
                  "file": "App.java",
                  "method": "main",
                  "line": 1,
                  "code": "first\\nsecond\\tthird\\\"done"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );

        var graph = new JoernOutputParser().parse(paths);

        assertEquals("first\nsecond\tthird\"done", graph.nodes().getFirst().normalizedCode());
    }

    @Test
    void missingRequiredFieldsFailTheImport() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.callgraph(),
            "{\"nodes\":[{\"id\":\"node-1\",\"type\":\"CALL\",\"file\":\"App.java\",\"line\":1}]}",
            StandardCharsets.UTF_8
        );

        var failure = assertThrows(JoernDockerAnalysisException.class, () -> new JoernOutputParser().parse(paths));

        assertEquals("Missing required Joern field: method", failure.getMessage());
    }

    @Test
    void invalidTextFieldShapeFailsTheImport() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.callgraph(),
            """
            {
              "edges": [
                {
                  "id" "edge-1"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );

        var failure = assertThrows(JoernDockerAnalysisException.class, () -> new JoernOutputParser().parse(paths));

        assertEquals("Missing required Joern field: id", failure.getMessage());
    }

    @Test
    void invalidNumericFieldsFailTheImport() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.slices(),
            """
            {
              "anchors": [
                {
                  "scanEventKey": "scan-1",
                  "node": "node-1",
                  "file": "App.java",
                  "method": "greet",
                  "line": 1,
                  "confidence": "unknown",
                  "strategy": "signature-and-line"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );

        var failure = assertThrows(JoernDockerAnalysisException.class, () -> new JoernOutputParser().parse(paths));

        assertEquals("Invalid numeric Joern field: confidence", failure.getMessage());
    }

    @Test
    void invalidNumericFormatsKeepTheParserFailureExplicit() throws Exception {
        var paths = JoernDockerArtifactPaths.under(tempDir);
        Files.writeString(
            paths.slices(),
            """
            {
              "anchors": [
                {
                  "scanEventKey": "scan-1",
                  "node": "node-1",
                  "file": "App.java",
                  "method": "greet",
                  "line": 1,
                  "confidence": 1.2.3,
                  "strategy": "signature-and-line"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );

        var failure = assertThrows(JoernDockerAnalysisException.class, () -> new JoernOutputParser().parse(paths));

        assertEquals("Invalid numeric Joern field: confidence", failure.getMessage());
        assertEquals(NumberFormatException.class, failure.getCause().getClass());
    }

    private static void writeArtifacts(JoernDockerArtifactPaths paths) throws Exception {
        Files.writeString(
            paths.callgraph(),
            """
            {
              "nodes": [
                {
                  "id": "call-node-1",
                  "type": "CALL",
                  "file": "src/main/java/com/example/App.java",
                  "fqcn": "com.example.App",
                  "method": "greet",
                  "signature": "java.lang.String greet(java.lang.String)",
                  "line": 6,
                  "code": "helper(name)"
                }
              ],
              "edges": [
                {
                  "id": "edge-1",
                  "source": "call-node-1",
                  "target": "return-node-1",
                  "type": "AST"
                }
              ],
              "methods": [
                {
                  "id": "method-1",
                  "file": "src/main/java/com/example/App.java",
                  "fqcn": "com.example.App",
                  "name": "greet",
                  "signature": "java.lang.String greet(java.lang.String)",
                  "line": 5
                }
              ],
              "calls": [
                {
                  "caller": "method-1",
                  "callee": "method-2",
                  "node": "call-node-1"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            paths.controlflow(),
            """
            {
              "relations": [
                {
                  "source": "call-node-1",
                  "target": "return-node-1",
                  "type": "NEXT"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            paths.dataflow(),
            """
            {
              "paths": [
                {
                  "id": "path-1",
                  "source": "parameter-node-1",
                  "target": "return-node-1",
                  "steps": [
                    {
                      "node": "parameter-node-1",
                      "order": 0,
                      "kind": "argument"
                    },
                    {
                      "node": "return-node-1",
                      "order": 1,
                      "kind": "return"
                    }
                  ]
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            paths.slices(),
            """
            {
              "anchors": [
                {
                  "scanEventKey": "scan-1",
                  "node": "call-node-1",
                  "file": "src/main/java/com/example/App.java",
                  "fqcn": "com.example.App",
                  "method": "greet",
                  "signature": "java.lang.String greet(java.lang.String)",
                  "line": 6,
                  "code": "helper(name)",
                  "confidence": 0.95,
                  "strategy": "signature-and-line"
                }
              ]
            }
            """,
            StandardCharsets.UTF_8
        );
    }
}
