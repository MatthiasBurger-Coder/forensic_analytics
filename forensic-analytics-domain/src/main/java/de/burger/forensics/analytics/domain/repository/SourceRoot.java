package de.burger.forensics.analytics.domain.repository;

public record SourceRoot(String path) {
    public SourceRoot {
        RequiredRepositoryText.requireText(path, "source root");
    }
}
