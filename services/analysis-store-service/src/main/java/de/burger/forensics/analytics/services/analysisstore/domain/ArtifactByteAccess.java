package de.burger.forensics.analytics.services.analysisstore.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public record ArtifactByteAccess(
    String ownerService,
    String retrievalContract,
    String retrievalReference,
    ArtifactByteCustody byteCustody
) {
    public ArtifactByteAccess {
        ownerService = RequiredText.require(ownerService, "ownerService");
        retrievalContract = requireSafeToken(retrievalContract, "retrievalContract");
        retrievalReference = requirePublicReference(retrievalReference, "retrievalReference");
        Objects.requireNonNull(byteCustody, "byteCustody must not be null");
    }

    public static String requirePublicReference(String value, String fieldName) {
        var reference = requireSafeToken(value, fieldName).replace('\\', '/');
        var lower = reference.toLowerCase(Locale.ROOT);
        if (reference.startsWith("/")
            || lower.startsWith("file:")
            || reference.matches("^[A-Za-z]:.*")
            || reference.contains("://")
            || reference.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must not be a private path or URI");
        }
        if (Arrays.asList(reference.split("/")).stream().anyMatch(part -> part.isBlank() || part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException(fieldName + " must not contain traversal, current-directory or blank path segments");
        }
        return reference;
    }

    private static String requireSafeToken(String value, String fieldName) {
        if (value != null && value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must not contain control characters");
        }
        var token = RequiredText.require(value, fieldName);
        var lower = token.toLowerCase(Locale.ROOT);
        if (lower.contains("secret")
            || lower.contains("token")
            || lower.contains("password")
            || lower.contains("credential")) {
            throw new IllegalArgumentException(fieldName + " must not contain sensitive coordinates");
        }
        return token;
    }
}
