package de.burger.forensics.analytics.ingestion.request;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class JsonParser {
    private final String input;
    private int index;

    private JsonParser(String input) {
        this.input = input;
    }

    static Map<String, Object> parseObject(String input) {
        var parser = new JsonParser(input);
        var value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.end()) {
            throw parser.error("Unexpected trailing JSON content");
        }
        if (value instanceof Map<?, ?> object) {
            var converted = new LinkedHashMap<String, Object>();
            object.forEach((key, objectValue) -> converted.put(String.valueOf(key), objectValue));
            return converted;
        }
        throw parser.error("Engine request JSON root must be an object");
    }

    private Object parseValue() {
        skipWhitespace();
        if (end()) {
            throw error("Unexpected end of JSON");
        }
        return switch (peek()) {
            case '{' -> parseObjectValue();
            case '[' -> parseArrayValue();
            case '"' -> parseString();
            default -> throw error("Unsupported JSON value");
        };
    }

    private Map<String, Object> parseObjectValue() {
        expect('{');
        var values = new LinkedHashMap<String, Object>();
        skipWhitespace();
        if (consume('}')) {
            return values;
        }
        do {
            skipWhitespace();
            var key = parseString();
            skipWhitespace();
            expect(':');
            values.put(key, parseValue());
            skipWhitespace();
        } while (consume(','));
        expect('}');
        return values;
    }

    private Object parseArrayValue() {
        expect('[');
        var values = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return values;
        }
        do {
            values.add(parseValue());
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return values;
    }

    private String parseString() {
        expect('"');
        var value = new StringBuilder();
        while (!end()) {
            var current = input.charAt(index++);
            if (current == '"') {
                return value.toString();
            }
            if (current == '\\') {
                value.append(parseEscapedCharacter());
                continue;
            }
            value.append(current);
        }
        throw error("Unterminated JSON string");
    }

    private char parseEscapedCharacter() {
        if (end()) {
            throw error("Unterminated JSON escape");
        }
        return switch (input.charAt(index++)) {
            case '\\' -> '\\';
            case '"' -> '"';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> throw error("Unsupported JSON escape");
        };
    }

    private void skipWhitespace() {
        while (!end() && Character.isWhitespace(peek())) {
            index++;
        }
    }

    private boolean consume(char expected) {
        if (!end() && peek() == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("Expected '" + expected + "'");
        }
    }

    private char peek() {
        return input.charAt(index);
    }

    private boolean end() {
        return index >= input.length();
    }

    private EngineIngestionRequestException error(String message) {
        return new EngineIngestionRequestException(message + " at character " + index);
    }
}
