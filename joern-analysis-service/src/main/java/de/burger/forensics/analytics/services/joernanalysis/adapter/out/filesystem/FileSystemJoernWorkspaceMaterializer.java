package de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspaceMaterializerPort;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializedPackageDescriptor;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireRelativePath;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireSha256;

public final class FileSystemJoernWorkspaceMaterializer implements JoernWorkspaceMaterializerPort {
    private final Path workspaceRoot;
    private final Path packageCacheRoot;

    public FileSystemJoernWorkspaceMaterializer(Path workspaceRoot, Path packageCacheRoot) {
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspace root must not be null")
            .toAbsolutePath()
            .normalize();
        this.packageCacheRoot = Objects.requireNonNull(packageCacheRoot, "package cache root must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public SourceWorkspace materialize(MaterializeJoernWorkspaceCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "materialization command must not be null");
        var workspaceId = "joern-workspace-" + verifiedCommand.metadata().sourceSnapshotId().value();
        var workspacePath = workspaceRoot.resolve(workspaceId).normalize();
        if (!workspacePath.startsWith(workspaceRoot)) {
            throw new JoernCpgArtifactException("Joern workspace resolves outside service workspace root");
        }

        var inputArtifacts = new ArrayList<AnalysisArtifactReference>();
        var materializedPaths = new HashSet<String>();
        var workspaceBytes = new long[] {0L};
        try {
            recreateDirectory(workspacePath);
            inputArtifacts.add(materializePackage(verifiedCommand, verifiedCommand.sourcePackage(), workspacePath, materializedPaths, workspaceBytes));
            inputArtifacts.add(materializePackage(verifiedCommand, verifiedCommand.buildOutputPackage(), workspacePath, materializedPaths, workspaceBytes));
            return new SourceWorkspace(workspaceId, verifiedCommand.sourceRoots(), inputArtifacts);
        } catch (RuntimeException error) {
            deleteDirectory(workspacePath);
            throw error;
        }
    }

    private AnalysisArtifactReference materializePackage(
        MaterializeJoernWorkspaceCommand command,
        MaterializedPackageDescriptor descriptor,
        Path workspacePath,
        Set<String> materializedPaths,
        long[] workspaceBytes
    ) {
        var manifestFile = resolveCachedArtifact(descriptor.manifestArtifact().path());
        var packageFile = resolveCachedArtifact(descriptor.byteAccess().retrievalReference());
        verifyArtifact(manifestFile, descriptor.manifestArtifact());
        verifyArtifact(packageFile, descriptor.packageArtifact());

        var entries = manifestEntries(manifestFile, command.policy().maxArchiveDepth());
        validateManifestEntries(entries, materializedPaths);
        var expectedBytes = entries.values().stream()
            .filter(entry -> entry.kind() == EntryKind.FILE)
            .mapToLong(ManifestEntry::sizeBytes)
            .sum();
        if (workspaceBytes[0] + expectedBytes > command.policy().maxWorkspaceBytes()) {
            throw new JoernCpgArtifactException("Joern materialized workspace exceeds byte quota");
        }

        extractPackage(packageFile, workspacePath, entries, workspaceBytes, command.policy().maxWorkspaceBytes());
        return new AnalysisArtifactReference(
            descriptor.packageArtifact(),
            AnalysisArtifactCategory.STATIC,
            descriptor.producerService(),
            descriptor.schemaVersion(),
            descriptor.completeness(),
            descriptor.byteAccess()
        );
    }

    private Path resolveCachedArtifact(String reference) {
        var relative = requireRelativePath(reference, "package cache reference");
        var path = packageCacheRoot.resolve(relative).normalize();
        if (!path.startsWith(packageCacheRoot)) {
            throw new JoernCpgArtifactException("package cache reference resolves outside service cache");
        }
        requireOwnedParentChain(packageCacheRoot, path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new JoernCpgArtifactException("required package bytes are not available");
        }
        return path;
    }

    private static void verifyArtifact(Path file, ArtifactReference artifact) {
        try {
            var size = regularFileAttributes(file).size();
            if (size != artifact.sizeBytes()) {
                throw new JoernCpgArtifactException("artifact byte size mismatch");
            }
            var checksum = sha256(file);
            if (!checksum.equals(artifact.sha256())) {
                throw new JoernCpgArtifactException("artifact checksum mismatch");
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to verify package artifact.", error);
        }
    }

    private static Map<String, ManifestEntry> manifestEntries(Path manifestFile, long maxDepth) {
        try {
            var root = JsonParser.parseString(Files.readString(manifestFile)).getAsJsonObject();
            var entries = root.getAsJsonArray("entries");
            if (entries == null || entries.isEmpty()) {
                throw new JoernCpgArtifactException("package manifest must contain entries");
            }
            var result = new HashMap<String, ManifestEntry>();
            for (var element : entries) {
                var entry = manifestEntry(element.getAsJsonObject(), maxDepth);
                if (result.put(entry.path(), entry) != null) {
                    throw new JoernCpgArtifactException("package manifest contains duplicate paths");
                }
            }
            return Map.copyOf(result);
        } catch (JoernCpgArtifactException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new JoernCpgArtifactException("package manifest is not valid JSON", error);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to read package manifest.", error);
        }
    }

    private static ManifestEntry manifestEntry(JsonObject object, long maxDepth) {
        var path = archivePath(text(object, "path"));
        if (path.split("/").length > maxDepth) {
            throw new JoernCpgArtifactException("package manifest path exceeds archive depth quota");
        }
        var kind = entryKind(text(object, "kind"));
        if (kind != EntryKind.FILE && kind != EntryKind.DIRECTORY) {
            throw new JoernCpgArtifactException("package manifest contains unsafe archive entry kind");
        }
        var sizeBytes = number(object, "sizeBytes");
        if (sizeBytes < 0) {
            throw new JoernCpgArtifactException("package manifest size must not be negative");
        }
        if (kind == EntryKind.DIRECTORY && sizeBytes != 0) {
            throw new JoernCpgArtifactException("package manifest directory size must be zero");
        }
        var sha256 = kind == EntryKind.FILE ? requireSha256(text(object, "sha256"), "manifest entry checksum") : "";
        return new ManifestEntry(path, kind, sha256, sizeBytes);
    }

    private static void validateManifestEntries(Map<String, ManifestEntry> entries, Set<String> materializedPaths) {
        for (var entry : entries.values()) {
            if (!materializedPaths.add(entry.path())) {
                throw new JoernCpgArtifactException("package manifest collides with an already materialized path");
            }
        }
    }

    private static void extractPackage(
        Path packageFile,
        Path workspacePath,
        Map<String, ManifestEntry> entries,
        long[] workspaceBytes,
        long maxWorkspaceBytes
    ) {
        var seen = new HashSet<String>();
        try (var stream = new ZipInputStream(Files.newInputStream(packageFile, LinkOption.NOFOLLOW_LINKS))) {
            ZipEntry zipEntry;
            while ((zipEntry = stream.getNextEntry()) != null) {
                var normalized = archivePath(zipEntry.getName());
                if (!seen.add(normalized)) {
                    throw new JoernCpgArtifactException("package archive contains duplicate paths");
                }
                var manifestEntry = entries.get(normalized);
                if (manifestEntry == null) {
                    throw new JoernCpgArtifactException("package archive contains an entry not declared by the manifest");
                }
                var target = workspacePath.resolve(normalized).normalize();
                if (!target.startsWith(workspacePath)) {
                    throw new JoernCpgArtifactException("package archive entry resolves outside Joern workspace");
                }
                if (zipEntry.isDirectory()) {
                    if (manifestEntry.kind() != EntryKind.DIRECTORY) {
                        throw new JoernCpgArtifactException("package archive entry kind does not match manifest");
                    }
                    createOwnedDirectories(workspacePath, target);
                } else {
                    if (manifestEntry.kind() != EntryKind.FILE) {
                        throw new JoernCpgArtifactException("package archive entry kind does not match manifest");
                    }
                    createOwnedDirectories(workspacePath, target.getParent());
                    var extracted = writeFile(stream, target, manifestEntry.sizeBytes(), maxWorkspaceBytes - workspaceBytes[0]);
                    workspaceBytes[0] += extracted.sizeBytes();
                    if (extracted.sizeBytes() != manifestEntry.sizeBytes() || !extracted.sha256().equals(manifestEntry.sha256())) {
                        throw new JoernCpgArtifactException("package archive entry checksum mismatch");
                    }
                }
            }
            if (!seen.equals(entries.keySet())) {
                throw new JoernCpgArtifactException("package archive is missing manifest entries");
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to extract Joern package archive.", error);
        }
    }

    private static ExtractedFile writeFile(InputStream input, Path target, long expectedBytes, long remainingWorkspaceBytes) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                var buffer = new byte[8192];
                long size = 0L;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (read == 0) {
                        continue;
                    }
                    size += read;
                    if (size > expectedBytes || size > remainingWorkspaceBytes) {
                        throw new JoernCpgArtifactException("package archive entry exceeds declared byte limits");
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
                return new ExtractedFile(size, HexFormat.of().formatHex(digest.digest()));
            }
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static String archivePath(String value) {
        var normalizedValue = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        if (normalizedValue.contains("\\") || normalizedValue.toLowerCase(Locale.ROOT).contains("://")) {
            throw new JoernCpgArtifactException("package archive path is not service-owned");
        }
        try {
            return requireRelativePath(normalizedValue, "package archive path");
        } catch (IllegalArgumentException error) {
            throw new JoernCpgArtifactException("package archive path is not service-owned", error);
        }
    }

    private static EntryKind entryKind(String value) {
        try {
            return EntryKind.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException error) {
            throw new JoernCpgArtifactException("package manifest contains unsafe archive entry kind", error);
        }
    }

    private static String text(JsonObject object, String field) {
        var value = object.get(field);
        if (value == null || value.getAsString().isBlank()) {
            throw new JoernCpgArtifactException("package manifest entry is missing required text");
        }
        return value.getAsString().strip();
    }

    private static long number(JsonObject object, String field) {
        var value = object.get(field);
        if (value == null) {
            throw new JoernCpgArtifactException("package manifest entry is missing required number");
        }
        return value.getAsLong();
    }

    private static String sha256(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var before = regularFileAttributes(file);
            try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                var buffer = new byte[8192];
                long size = 0L;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (read > 0) {
                        size += read;
                        digest.update(buffer, 0, read);
                    }
                }
                var after = regularFileAttributes(file);
                if (!stableFile(before, after) || size != after.size()) {
                    throw new JoernCpgArtifactException("package artifact changed while being verified");
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static void requireOwnedParentChain(Path root, Path path) {
        try {
            var normalizedRoot = root.toAbsolutePath().normalize();
            var normalizedPath = path.toAbsolutePath().normalize();
            requireDirectoryWithoutLinks(normalizedRoot);
            var current = normalizedRoot;
            var relativeParent = normalizedRoot.relativize(normalizedPath.getParent());
            for (var part : relativeParent) {
                current = current.resolve(part);
                requireDirectoryWithoutLinks(current);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect Joern service-owned path.", error);
        }
    }

    private static void createOwnedDirectories(Path root, Path directory) throws IOException {
        var normalizedRoot = root.toAbsolutePath().normalize();
        var normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(normalizedRoot)) {
            throw new JoernCpgArtifactException("package archive entry resolves outside Joern workspace");
        }
        requireDirectoryWithoutLinks(normalizedRoot);
        var current = normalizedRoot;
        for (var part : normalizedRoot.relativize(normalizedDirectory)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                requireDirectoryWithoutLinks(current);
            } else {
                Files.createDirectory(current);
            }
        }
    }

    private static BasicFileAttributes regularFileAttributes(Path file) throws IOException {
        var attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) {
            throw new JoernCpgArtifactException("required package bytes are not a service-owned regular file");
        }
        return attributes;
    }

    private static void requireDirectoryWithoutLinks(Path directory) throws IOException {
        var attributes = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory()) {
            throw new JoernCpgArtifactException("Joern service-owned directory is not available");
        }
    }

    private static boolean stableFile(BasicFileAttributes before, BasicFileAttributes after) {
        return before.size() == after.size()
            && Objects.equals(before.fileKey(), after.fileKey())
            && Objects.equals(before.lastModifiedTime(), after.lastModifiedTime());
    }

    private static void recreateDirectory(Path directory) {
        deleteDirectory(directory);
        try {
            Files.createDirectories(directory);
            requireDirectoryWithoutLinks(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to create Joern workspace.", error);
        }
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            var paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (var path : paths) {
                Files.delete(path);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to reset Joern workspace.", error);
        }
    }

    private record ManifestEntry(String path, EntryKind kind, String sha256, long sizeBytes) {
    }

    private record ExtractedFile(long sizeBytes, String sha256) {
    }

    private enum EntryKind {
        FILE,
        DIRECTORY,
        SYMLINK,
        HARDLINK,
        DEVICE,
        SOCKET,
        FIFO;

    }
}
