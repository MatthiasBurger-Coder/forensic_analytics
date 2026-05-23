package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class SafeMetadata {
    private static final Set<String> SENSITIVE_TOKENS = Set.of(
        "authorization",
        "credential",
        "password",
        "secret",
        "token"
    );

    private SafeMetadata() {
    }

    public static String requireOpaqueId(String value, String fieldName) {
        var id = requireSafeValue(value, fieldName);
        if (!id.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(fieldName + " must be an opaque public id");
        }
        return id;
    }

    public static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var sorted = new TreeMap<String, String>();
        Objects.requireNonNull(attributes, "attributes must not be null").forEach((key, value) -> sorted.put(
            requireSafeValue(key, "attribute key"),
            requireSafeValue(value, "attribute value")
        ));
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static String requireSafeValue(String value, String fieldName) {
        var token = RequiredText.require(value, fieldName);
        var lower = token.toLowerCase(Locale.ROOT);
        if (SENSITIVE_TOKENS.stream().anyMatch(lower::contains)
            || looksLikePrivatePath(lower)
            || token.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must not contain sensitive or private values");
        }
        return token;
    }

    private static boolean looksLikePrivatePath(String lower) {
        return lower.startsWith("file:")
            || lower.startsWith("/")
            || lower.startsWith("\\")
            || lower.contains("://")
            || lower.matches(".*[a-z]:[\\\\/].*")
            || lower.contains("/home/")
            || lower.contains("/tmp/")
            || lower.contains("/var/")
            || lower.contains("/mnt/")
            || lower.contains("/users/")
            || lower.contains("/workspace/")
            || lower.contains("/workspaces/")
            || lower.contains("\\users\\")
            || lower.contains("\\tmp\\")
            || lower.contains("\\workspace\\");
    }
}
