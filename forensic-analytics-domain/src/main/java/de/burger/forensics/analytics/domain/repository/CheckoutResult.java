package de.burger.forensics.analytics.domain.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;

public record CheckoutResult(
    String resolvedRemoteUrl,
    Optional<String> requestedBranch,
    Optional<String> requestedCommit,
    String resolvedCommit,
    List<SourceRoot> detectedSourceRoots,
    String checkoutStatus,
    List<String> diagnostics
) {
    public CheckoutResult {
        RequiredRepositoryText.requireText(resolvedRemoteUrl, "resolved remote url");
        requestedBranch = copyOptionalText(requestedBranch, "requested branch");
        requestedCommit = copyOptionalText(requestedCommit, "requested commit");
        RequiredRepositoryText.requireText(resolvedCommit, "resolved commit");
        detectedSourceRoots = copySourceRoots(detectedSourceRoots);
        RequiredRepositoryText.requireText(checkoutStatus, "checkout status");
        diagnostics = copyDiagnostics(diagnostics);
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredRepositoryText.requireText(text, fieldName));
        return copied;
    }

    private static List<String> copyDiagnostics(List<String> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> RequiredRepositoryText.requireText(diagnostic, "diagnostic"))
            .toList();
    }

    private static List<SourceRoot> copySourceRoots(List<SourceRoot> detectedSourceRoots) {
        return List.copyOf(Objects.requireNonNull(detectedSourceRoots, "detectedSourceRoots must not be null")).stream()
            .sorted(Comparator.comparing(SourceRoot::path))
            .toList();
    }
}
