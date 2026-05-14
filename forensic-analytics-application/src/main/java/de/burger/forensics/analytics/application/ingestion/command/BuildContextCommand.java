package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.analysis.BuildContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record BuildContextCommand(
    String buildTool,
    String buildId,
    Optional<String> rootProjectName,
    List<String> declaredModules,
    Map<String, String> attributes
) {
    public BuildContextCommand(
        String buildTool,
        String buildId,
        String rootProjectName,
        List<String> declaredModules,
        Map<String, String> attributes
    ) {
        this(buildTool, buildId, optionalText(rootProjectName), declaredModules, attributes);
    }

    public BuildContextCommand {
        requireText(buildTool, "build tool");
        requireText(buildId, "build id");
        rootProjectName = Objects.requireNonNull(rootProjectName, "rootProjectName must not be null");
        rootProjectName.ifPresent(rootProject -> requireText(rootProject, "root project name"));
        declaredModules = copyTextList(declaredModules, "declared module");
        attributes = copyAttributes(attributes);
    }

    public BuildContext toDomain() {
        return new BuildContext(buildTool, buildId, rootProjectName, declaredModules, attributes);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static List<String> copyTextList(List<String> values, String fieldName) {
        return List.copyOf(Objects.requireNonNull(values, fieldName + "s must not be null")).stream()
            .peek(value -> requireText(value, fieldName))
            .toList();
    }

    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        var sorted = new TreeMap<String, String>();
        attributes.forEach((key, value) -> {
            requireText(key, "attribute key");
            requireText(value, "attribute value");
            sorted.put(key, value);
        });
        return Collections.unmodifiableMap(sorted);
    }
}
