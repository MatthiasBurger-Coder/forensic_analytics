package de.burger.forensics.analytics.domain.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record BuildContext(
    String buildTool,
    String buildId,
    Optional<String> rootProjectName,
    List<String> declaredModules,
    Map<String, String> attributes
) {
    public BuildContext {
        RequiredAnalysisText.requireText(buildTool, "build tool");
        RequiredAnalysisText.requireText(buildId, "build id");
        rootProjectName = copyOptionalText(rootProjectName, "root project name");
        declaredModules = copyTextList(declaredModules, "declared module");
        attributes = copyAttributes(attributes);
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredAnalysisText.requireText(text, fieldName));
        return copied;
    }

    private static List<String> copyTextList(List<String> values, String fieldName) {
        return List.copyOf(Objects.requireNonNull(values, fieldName + "s must not be null")).stream()
            .peek(value -> RequiredAnalysisText.requireText(value, fieldName))
            .toList();
    }

    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        var sorted = new TreeMap<String, String>();
        attributes.forEach((key, value) -> {
            RequiredAnalysisText.requireText(key, "attribute key");
            RequiredAnalysisText.requireText(value, "attribute value");
            sorted.put(key, value);
        });
        return Collections.unmodifiableMap(sorted);
    }
}
