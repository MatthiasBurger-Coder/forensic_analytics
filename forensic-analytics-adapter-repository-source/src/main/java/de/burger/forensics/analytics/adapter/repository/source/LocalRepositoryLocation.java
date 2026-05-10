package de.burger.forensics.analytics.adapter.repository.source;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

final class LocalRepositoryLocation {
    private static final Pattern URI_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[a-zA-Z]:[\\\\/].*");
    private static final Pattern SCP_STYLE_REMOTE = Pattern.compile("^[^/\\\\]+@[^:]+:.+");

    private LocalRepositoryLocation() {
    }

    static Path resolve(String repositoryLocation, Path baseDirectory) {
        Objects.requireNonNull(baseDirectory, "baseDirectory must not be null");
        if (repositoryLocation == null || repositoryLocation.isBlank()) {
            throw new IllegalArgumentException("repository location must not be blank");
        }
        if (repositoryLocation.startsWith("file:")) {
            return Path.of(URI.create(repositoryLocation)).toAbsolutePath().normalize();
        }
        if (isUnsupportedUri(repositoryLocation)) {
            throw new IllegalArgumentException("Only local filesystem repository locations are supported");
        }
        var repositoryPath = Path.of(repositoryLocation);
        var resolved = repositoryPath.isAbsolute() ? repositoryPath : baseDirectory.resolve(repositoryPath);
        return resolved.toAbsolutePath().normalize();
    }

    private static boolean isUnsupportedUri(String repositoryLocation) {
        return SCP_STYLE_REMOTE.matcher(repositoryLocation).matches()
            || (URI_SCHEME.matcher(repositoryLocation).matches()
                && !WINDOWS_ABSOLUTE_PATH.matcher(repositoryLocation).matches());
    }
}
