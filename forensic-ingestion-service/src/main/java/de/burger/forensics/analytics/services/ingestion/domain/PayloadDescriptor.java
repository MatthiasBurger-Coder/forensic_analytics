package de.burger.forensics.analytics.services.ingestion.domain;

import java.util.Map;

public record PayloadDescriptor(
    String payloadId,
    AnalysisPayloadKind kind,
    String contentType,
    Map<String, String> attributes
) {
    public PayloadDescriptor {
        payloadId = RequiredText.require(payloadId, "payloadId");
        kind = java.util.Objects.requireNonNull(kind, "kind must not be null");
        contentType = RequiredText.require(contentType, "contentType");
        attributes = copyAttributes(attributes);
    }

    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        var values = java.util.Objects.requireNonNull(attributes, "attributes must not be null");
        values.forEach((key, value) -> {
            RequiredText.require(key, "attributes.key");
            RequiredText.require(value, "attributes[" + key + "]");
        });
        return Map.copyOf(values);
    }
}
