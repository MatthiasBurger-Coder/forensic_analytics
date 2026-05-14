package de.burger.forensics.analytics.domain.ingestion;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record AnalysisPayloadDescriptor(
    String payloadId,
    AnalysisPayloadKind kind,
    String contentType,
    Map<String, String> attributes
) {
    public AnalysisPayloadDescriptor {
        payloadId = requireText(payloadId, "payloadId");
        kind = Objects.requireNonNull(kind, "kind must not be null");
        contentType = requireText(contentType, "contentType");
        attributes = immutableSortedAttributes(attributes);
    }

    private static Map<String, String> immutableSortedAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        var sortedAttributes = new TreeMap<String, String>();
        attributes.forEach((key, value) -> sortedAttributes.put(
            requireText(key, "attribute key"),
            requireText(value, "attribute value")
        ));
        return Collections.unmodifiableMap(sortedAttributes);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
