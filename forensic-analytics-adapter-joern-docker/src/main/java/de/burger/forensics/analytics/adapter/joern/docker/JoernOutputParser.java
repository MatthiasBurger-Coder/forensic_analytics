package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.domain.semantic.CallRelation;
import de.burger.forensics.analytics.domain.semantic.ControlFlowRelation;
import de.burger.forensics.analytics.domain.semantic.DataFlowPath;
import de.burger.forensics.analytics.domain.semantic.DataFlowStep;
import de.burger.forensics.analytics.domain.semantic.SemanticAnchor;
import de.burger.forensics.analytics.domain.semantic.SemanticEdge;
import de.burger.forensics.analytics.domain.semantic.SemanticGraph;
import de.burger.forensics.analytics.domain.semantic.SemanticMethod;
import de.burger.forensics.analytics.domain.semantic.SemanticNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public final class JoernOutputParser {
    private static final String FIELD_SIGNATURE = "signature";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_TARGET = "target";

    public SemanticGraph parse(JoernDockerArtifactPaths paths) {
        Objects.requireNonNull(paths, "paths must not be null");
        var callgraph = readIfExists(paths.callgraph());
        var controlflow = readIfExists(paths.controlflow());
        var dataflow = readIfExists(paths.dataflow());
        var slices = readIfExists(paths.slices());
        return new SemanticGraph(
            JsonArray.objects(callgraph, "nodes").stream().map(JoernOutputParser::node).toList(),
            JsonArray.objects(callgraph, "edges").stream().map(JoernOutputParser::edge).toList(),
            JsonArray.objects(callgraph, "methods").stream().map(JoernOutputParser::method).toList(),
            JsonArray.objects(callgraph, "calls").stream().map(JoernOutputParser::call).toList(),
            JsonArray.objects(controlflow, "relations").stream().map(JoernOutputParser::controlFlow).toList(),
            JsonArray.objects(dataflow, "paths").stream().map(JoernOutputParser::dataFlowPath).toList(),
            JsonArray.objects(slices, "anchors").stream().map(JoernOutputParser::anchor).toList()
        );
    }

    private static SemanticNode node(String object) {
        return new SemanticNode(
            JsonField.text(object, "id"),
            JsonField.text(object, "type"),
            JsonField.text(object, "file"),
            JsonField.optionalText(object, "fqcn"),
            JsonField.text(object, "method"),
            JsonField.optionalText(object, FIELD_SIGNATURE),
            JsonField.integer(object, "line"),
            JsonField.optionalText(object, "code")
        );
    }

    private static SemanticEdge edge(String object) {
        return new SemanticEdge(
            JsonField.text(object, "id"),
            JsonField.text(object, FIELD_SOURCE),
            JsonField.text(object, FIELD_TARGET),
            JsonField.text(object, "type")
        );
    }

    private static SemanticMethod method(String object) {
        return new SemanticMethod(
            JsonField.text(object, "id"),
            JsonField.text(object, "file"),
            JsonField.text(object, "fqcn"),
            JsonField.text(object, "name"),
            JsonField.optionalText(object, FIELD_SIGNATURE),
            JsonField.integer(object, "line")
        );
    }

    private static CallRelation call(String object) {
        return new CallRelation(
            JsonField.text(object, "caller"),
            JsonField.text(object, "callee"),
            JsonField.text(object, "node")
        );
    }

    private static ControlFlowRelation controlFlow(String object) {
        return new ControlFlowRelation(
            JsonField.text(object, FIELD_SOURCE),
            JsonField.text(object, FIELD_TARGET),
            JsonField.text(object, "type")
        );
    }

    private static DataFlowPath dataFlowPath(String object) {
        return new DataFlowPath(
            JsonField.text(object, "id"),
            JsonField.text(object, FIELD_SOURCE),
            JsonField.text(object, FIELD_TARGET),
            JsonArray.objects(object, "steps").stream()
                .map(step -> new DataFlowStep(
                    JsonField.text(step, "node"),
                    JsonField.integer(step, "order"),
                    JsonField.text(step, "kind")
                ))
                .toList()
        );
    }

    private static SemanticAnchor anchor(String object) {
        return new SemanticAnchor(
            JsonField.text(object, "scanEventKey"),
            JsonField.text(object, "node"),
            JsonField.text(object, "file"),
            JsonField.optionalText(object, "fqcn"),
            JsonField.text(object, "method"),
            JsonField.optionalText(object, FIELD_SIGNATURE),
            JsonField.integer(object, "line"),
            JsonField.optionalText(object, "code"),
            JsonField.decimal(object, "confidence"),
            JsonField.text(object, "strategy")
        );
    }

    private static String readIfExists(java.nio.file.Path file) {
        if (!Files.exists(file)) {
            return "{}";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Joern artifact " + file + ".", e);
        }
    }

    private static final class JsonArray {
        private JsonArray() {
        }

        static List<String> objects(String json, String arrayName) {
            var arrayStart = json.indexOf("\"" + arrayName + "\"");
            if (arrayStart < 0) {
                return List.of();
            }
            var openBracket = json.indexOf('[', arrayStart);
            var closeBracket = matching(json, openBracket, '[', ']');
            if (openBracket < 0 || closeBracket < 0) {
                return List.of();
            }
            return splitObjects(json.substring(openBracket + 1, closeBracket));
        }

        private static List<String> splitObjects(String arrayBody) {
            var objects = new java.util.ArrayList<String>();
            var cursor = 0;
            var openBrace = arrayBody.indexOf('{', cursor);
            while (openBrace >= 0) {
                var closeBrace = matching(arrayBody, openBrace, '{', '}');
                if (closeBrace < 0) {
                    return List.copyOf(objects);
                }
                objects.add(arrayBody.substring(openBrace, closeBrace + 1));
                cursor = closeBrace + 1;
                openBrace = arrayBody.indexOf('{', cursor);
            }
            return List.copyOf(objects);
        }

        private static int matching(String text, int openIndex, char open, char close) {
            if (openIndex < 0) {
                return -1;
            }
            var depth = 0;
            var inString = false;
            for (var index = openIndex; index < text.length(); index++) {
                var current = text.charAt(index);
                var escaped = index > 0 && text.charAt(index - 1) == '\\';
                if (current == '"' && !escaped) {
                    inString = !inString;
                }
                if (inString) {
                    continue;
                }
                if (current == open) {
                    depth++;
                }
                if (current == close) {
                    depth--;
                    if (depth == 0) {
                        return index;
                    }
                }
            }
            return -1;
        }
    }

    private static final class JsonField {
        private JsonField() {
        }

        static String text(String object, String fieldName) {
            var value = optionalText(object, fieldName);
            if (value == null || value.isBlank()) {
                throw new JoernDockerAnalysisException("Missing required Joern field: " + fieldName);
            }
            return value;
        }

        static String optionalText(String object, String fieldName) {
            var marker = "\"" + fieldName + "\"";
            var fieldStart = object.indexOf(marker);
            if (fieldStart < 0) {
                return null;
            }
            var colon = object.indexOf(':', fieldStart + marker.length());
            if (colon < 0) {
                return null;
            }
            var firstQuote = object.indexOf('"', colon + 1);
            if (firstQuote < 0) {
                return null;
            }
            var builder = new StringBuilder();
            var escaped = false;
            for (var index = firstQuote + 1; index < object.length(); index++) {
                var current = object.charAt(index);
                if (escaped) {
                    builder.append(unescape(current));
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    return builder.toString();
                } else {
                    builder.append(current);
                }
            }
            return null;
        }

        static int integer(String object, String fieldName) {
            try {
                return Integer.parseInt(number(object, fieldName));
            } catch (NumberFormatException e) {
                throw new JoernDockerAnalysisException("Invalid numeric Joern field: " + fieldName, e);
            }
        }

        static double decimal(String object, String fieldName) {
            try {
                return Double.parseDouble(number(object, fieldName));
            } catch (NumberFormatException e) {
                throw new JoernDockerAnalysisException("Invalid numeric Joern field: " + fieldName, e);
            }
        }

        private static String number(String object, String fieldName) {
            var marker = "\"" + fieldName + "\"";
            var fieldStart = object.indexOf(marker);
            if (fieldStart < 0) {
                throw new JoernDockerAnalysisException("Missing required Joern field: " + fieldName);
            }
            var colon = object.indexOf(':', fieldStart + marker.length());
            if (colon < 0) {
                throw new JoernDockerAnalysisException("Invalid Joern field: " + fieldName);
            }
            var cursor = colon + 1;
            while (cursor < object.length() && Character.isWhitespace(object.charAt(cursor))) {
                cursor++;
            }
            var end = cursor;
            while (end < object.length() && "-0123456789.".indexOf(object.charAt(end)) >= 0) {
                end++;
            }
            if (end == cursor) {
                throw new JoernDockerAnalysisException("Invalid numeric Joern field: " + fieldName);
            }
            return object.substring(cursor, end);
        }

        private static char unescape(char escaped) {
            return switch (escaped) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'b' -> '\b';
                case 'f' -> '\f';
                default -> escaped;
            };
        }
    }
}
