package de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactReaderPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SemanticArtifactBytes;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SemanticArtifactBytesRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.completeness;

public final class FileSystemJoernArtifactCollector implements JoernArtifactCollectorPort, JoernArtifactReaderPort {
    public static final String CPG = "cpg.bin.zip";
    public static final String CALLGRAPH = "callgraph.json";
    public static final String CONTROLFLOW = "controlflow.json";
    public static final String DATAFLOW = "dataflow.json";
    public static final String SLICES = "slices.json";
    public static final String PROVENANCE = "joern-provenance.json";
    public static final String BYTE_RETRIEVAL_CONTRACT =
        "joern-cpg-analysis.v1.JoernCpgAnalysisService.GetSemanticArtifactBytes";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Path artifactRoot;

    public FileSystemJoernArtifactCollector(Path artifactRoot) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifact root must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public JoernArtifactCollectionResult collect(AnalyzeJoernCpgCommand command, JoernRuntimeResult runtimeResult) {
        var artifactDirectory = artifactDirectory(runtimeResult);
        var diagnostics = new ArrayList<JoernCpgDiagnostic>();
        var artifacts = new ArrayList<AnalysisArtifactReference>();
        var expected = expected(command);
        var missing = 0;

        for (var fileName : expected) {
            var file = artifactDirectory.resolve(fileName);
            if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                artifacts.add(reference(command, file));
            } else if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new JoernCpgArtifactException("Joern artifact output is not a service-owned regular file");
            } else {
                missing++;
                diagnostics.add(JoernCpgDiagnostic.warning(
                    command.metadata().sourceSnapshotId(),
                    "JOERN_ARTIFACT_MISSING",
                    "Expected Joern artifact was not produced.",
                    artifactDirectory.relativize(file).toString().replace('\\', '/'),
                    true
                ));
            }
        }
        var provenanceDiagnostics = new ArrayList<JoernCpgDiagnostic>();
        provenanceDiagnostics.addAll(runtimeResult.diagnostics());
        provenanceDiagnostics.addAll(diagnostics);
        var completeness = completeness(provenanceDiagnostics);
        artifacts.add(provenance(command, runtimeResult, artifactDirectory, artifacts, provenanceDiagnostics, completeness));
        enforceArtifactLimit(command, artifacts);
        return new JoernArtifactCollectionResult(artifacts, missing, diagnostics);
    }

    @Override
    public JoernArtifactCollectionResult collectUnavailable(
        AnalyzeJoernCpgCommand command,
        ResolvedJoernWorkspace workspace,
        JoernCpgDiagnostic diagnostic
    ) {
        var artifactDirectory = artifactDirectory(artifactDirectory(command));
        var diagnostics = List.of(diagnostic);
        var artifact = unavailableProvenance(command, workspace, artifactDirectory, diagnostics);
        return new JoernArtifactCollectionResult(List.of(artifact), expected(command).size(), diagnostics);
    }

    @Override
    public SemanticArtifactBytes read(SemanticArtifactBytesRequest request) {
        if (!SEMANTIC_ARTIFACT_SCHEMA_VERSION.equals(request.schemaVersion())) {
            throw new IllegalArgumentException("semantic artifact schema version mismatch");
        }
        if (!request.retrievalReference().startsWith(artifactDirectory(request) + "/")) {
            throw new IllegalStateException("semantic artifact identity mismatch");
        }
        var relativePath = Path.of(request.retrievalReference());
        var target = artifactRoot.resolve(relativePath).normalize();
        if (!target.startsWith(artifactRoot)) {
            throw new IllegalArgumentException("retrieval reference must stay inside Joern artifact storage");
        }
        try {
            rejectSymlinkSegments(relativePath);
            var attributes = regularFileAttributes(target);
            if (attributes.size() > request.maxBytes()) {
                throw new IllegalStateException("semantic artifact exceeds requested byte limit");
            }
            if (attributes.size() != request.expectedSizeBytes()) {
                throw new IllegalStateException("semantic artifact size mismatch");
            }
            var bytes = readBytes(target, request.maxBytes());
            var checksum = sha256(bytes);
            if (!checksum.equals(request.expectedSha256())) {
                throw new IllegalStateException("semantic artifact checksum mismatch");
            }
            return new SemanticArtifactBytes(
                new AnalysisArtifactReference(
                    new ArtifactReference(request.retrievalReference(), artifactType(target), checksum, bytes.length),
                    AnalysisArtifactCategory.STATIC,
                    PRODUCER_SERVICE,
                    SEMANTIC_ARTIFACT_SCHEMA_VERSION,
                    completenessFor(target),
                    new ArtifactByteAccess(
                        PRODUCER_SERVICE,
                        BYTE_RETRIEVAL_CONTRACT,
                        request.retrievalReference(),
                        ArtifactByteCustody.PRODUCER_RETAINED
                    )
                ),
                bytes,
                request.safeAttributes()
            );
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to read Joern semantic artifact.", error);
        }
    }

    private Path artifactDirectory(JoernRuntimeResult runtimeResult) {
        return artifactDirectory(runtimeResult.artifactDirectory());
    }

    private Path artifactDirectory(String relativeDirectory) {
        var directory = artifactRoot.resolve(relativeDirectory).normalize();
        if (!directory.startsWith(artifactRoot)) {
            throw new JoernCpgArtifactException("Joern artifact directory resolves outside service artifact root");
        }
        try {
            createServiceOwnedDirectory(artifactRoot, directory);
            return directory;
        } catch (IOException error) {
            throw new JoernCpgArtifactException("Failed to create Joern artifact directory.", error);
        }
    }

    private static List<String> expected(AnalyzeJoernCpgCommand command) {
        var artifacts = new ArrayList<String>();
        artifacts.add(CPG);
        if (command.policy().requireCallgraph()) {
            artifacts.add(CALLGRAPH);
        }
        if (command.policy().requireControlflow()) {
            artifacts.add(CONTROLFLOW);
        }
        if (command.policy().requireDataflow()) {
            artifacts.add(DATAFLOW);
            artifacts.add(SLICES);
        }
        return artifacts;
    }

    private AnalysisArtifactReference unavailableProvenance(
        AnalyzeJoernCpgCommand command,
        ResolvedJoernWorkspace workspace,
        Path artifactDirectory,
        List<JoernCpgDiagnostic> diagnostics
    ) {
        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("analysisRunId", command.metadata().analysisRunId().value());
        metadata.put("analysisJobId", command.metadata().analysisJobId().value());
        metadata.put("sourceSnapshotId", command.metadata().sourceSnapshotId().value());
        metadata.put("producerService", PRODUCER_SERVICE);
        metadata.put("schemaVersion", SEMANTIC_ARTIFACT_SCHEMA_VERSION);
        metadata.put("joernVersion", "unavailable");
        metadata.put("joernImageReference", command.policy().joernImageReference());
        metadata.put("queryBundleVersion", command.policy().queryBundleVersion());
        metadata.put("sourceRootCount", workspace.sourceRootPaths().size());
        metadata.put("completeness", AnalysisCompleteness.UNKNOWN.name());
        metadata.put("artifactPaths", List.of());
        metadata.put("diagnostics", diagnostics.stream().map(JoernCpgDiagnostic::code).sorted().toList());
        var provenance = artifactDirectory.resolve(PROVENANCE);
        try {
            writeString(provenance, GSON.toJson(metadata));
            return reference(command, provenance, AnalysisCompleteness.UNKNOWN);
        } catch (IOException error) {
            throw new JoernCpgArtifactException("Failed to write Joern unavailable provenance artifact.", error);
        }
    }

    private AnalysisArtifactReference reference(AnalyzeJoernCpgCommand command, Path file) {
        return reference(command, file, AnalysisCompleteness.COMPLETE);
    }

    private AnalysisArtifactReference reference(
        AnalyzeJoernCpgCommand command,
        Path file,
        AnalysisCompleteness artifactCompleteness
    ) {
        try {
            var hash = sha256(file, command.policy().maxArtifactBytes());
            var artifact = new ArtifactReference(relativePath(file), artifactType(file), hash.sha256(), hash.sizeBytes());
            return new AnalysisArtifactReference(
                artifact,
                AnalysisArtifactCategory.STATIC,
                PRODUCER_SERVICE,
                SEMANTIC_ARTIFACT_SCHEMA_VERSION,
                artifactCompleteness,
                new ArtifactByteAccess(
                    PRODUCER_SERVICE,
                    BYTE_RETRIEVAL_CONTRACT,
                    artifact.path(),
                    ArtifactByteCustody.PRODUCER_RETAINED
                )
            );
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to inspect Joern artifact.", error);
        }
    }

    private AnalysisArtifactReference provenance(
        AnalyzeJoernCpgCommand command,
        JoernRuntimeResult runtimeResult,
        Path artifactDirectory,
        List<AnalysisArtifactReference> artifacts,
        List<JoernCpgDiagnostic> diagnostics,
        AnalysisCompleteness completeness
    ) {
        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("analysisRunId", command.metadata().analysisRunId().value());
        metadata.put("analysisJobId", command.metadata().analysisJobId().value());
        metadata.put("sourceSnapshotId", command.metadata().sourceSnapshotId().value());
        metadata.put("producerService", PRODUCER_SERVICE);
        metadata.put("schemaVersion", SEMANTIC_ARTIFACT_SCHEMA_VERSION);
        metadata.put("joernVersion", runtimeResult.joernVersion());
        metadata.put("joernImageReference", runtimeResult.joernImageReference());
        metadata.put("queryBundleVersion", command.policy().queryBundleVersion());
        metadata.put("completeness", completeness.name());
        metadata.put("artifactPaths", artifacts.stream().map(reference -> reference.artifact().path()).sorted().toList());
        metadata.put("diagnostics", diagnostics.stream().map(JoernCpgDiagnostic::code).sorted().toList());
        var provenance = artifactDirectory.resolve(PROVENANCE);
        try {
            writeString(provenance, GSON.toJson(metadata));
            return reference(command, provenance, completeness);
        } catch (IOException error) {
            throw new JoernCpgArtifactException("Failed to write Joern provenance artifact.", error);
        }
    }

    private void enforceArtifactLimit(AnalyzeJoernCpgCommand command, List<AnalysisArtifactReference> artifacts) {
        var total = artifacts.stream().mapToLong(reference -> reference.artifact().sizeBytes()).sum();
        if (total > command.policy().maxArtifactBytes()) {
            throw new JoernCpgArtifactException("Joern artifact byte size exceeds scan policy");
        }
    }

    private String relativePath(Path file) {
        var normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(artifactRoot)) {
            throw new JoernCpgArtifactException("Joern artifact resolves outside service artifact root");
        }
        return artifactRoot.relativize(normalized).toString().replace('\\', '/');
    }

    private static String artifactType(Path file) {
        return switch (file.getFileName().toString()) {
            case CPG -> "application/vnd.forensic-analytics.joern-cpg.v1+binary";
            case CALLGRAPH -> "application/vnd.forensic-analytics.joern-callgraph.v1+json";
            case CONTROLFLOW -> "application/vnd.forensic-analytics.joern-controlflow.v1+json";
            case DATAFLOW -> "application/vnd.forensic-analytics.joern-dataflow.v1+json";
            case SLICES -> "application/vnd.forensic-analytics.joern-slices.v1+json";
            case PROVENANCE -> "application/vnd.forensic-analytics.joern-provenance.v1+json";
            default -> "application/vnd.forensic-analytics.joern-artifact.v1";
        };
    }

    private static HashResult sha256(Path file, long maxBytes) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var before = regularFileAttributes(file);
            try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                var buffer = new byte[8192];
                long size = 0L;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (read > 0) {
                        size += read;
                        if (size > maxBytes) {
                            throw new JoernCpgArtifactException("Joern artifact byte size exceeds scan policy");
                        }
                        digest.update(buffer, 0, read);
                    }
                }
                var after = regularFileAttributes(file);
                if (!stableFile(before, after) || size != after.size()) {
                    throw new JoernCpgArtifactException("Joern artifact changed while being inspected");
                }
                return new HashResult(size, HexFormat.of().formatHex(digest.digest()));
            }
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available.", error);
        }
    }

    private static byte[] readBytes(Path file, long maxBytes) throws IOException {
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS);
             var output = new ByteArrayOutputStream()) {
            var buffer = new byte[8192];
            long size = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read <= 0) {
                    continue;
                }
                size += read;
                if (size > maxBytes) {
                    throw new JoernCpgArtifactException("semantic artifact exceeds requested byte limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available.", error);
        }
    }

    private static AnalysisCompleteness completenessFor(Path file) {
        if (!PROVENANCE.equals(file.getFileName().toString())) {
            return AnalysisCompleteness.COMPLETE;
        }
        try {
            var json = com.google.gson.JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            var completeness = json.get("completeness");
            if (completeness != null && AnalysisCompleteness.UNKNOWN.name().equals(completeness.getAsString())) {
                return AnalysisCompleteness.UNKNOWN;
            }
            if (completeness != null && AnalysisCompleteness.INCOMPLETE.name().equals(completeness.getAsString())) {
                return AnalysisCompleteness.INCOMPLETE;
            }
            return AnalysisCompleteness.COMPLETE;
        } catch (RuntimeException | IOException error) {
            return AnalysisCompleteness.UNKNOWN;
        }
    }

    private static void writeString(Path file, String content) throws IOException {
        var parent = file.getParent();
        if (parent == null) {
            throw new JoernCpgArtifactException("Joern artifact parent directory is not available");
        }
        requireDirectoryWithoutLinks(parent);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            regularFileAttributes(file);
            Files.delete(file);
        }
        try (var writer = Files.newBufferedWriter(
            file,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )) {
            writer.write(content);
        }
        regularFileAttributes(file);
    }

    private static void createServiceOwnedDirectory(Path root, Path directory) throws IOException {
        Files.createDirectories(root);
        var normalizedRoot = root.toAbsolutePath().normalize();
        var normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(normalizedRoot)) {
            throw new JoernCpgArtifactException("Joern artifact directory resolves outside service artifact root");
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
            throw new JoernCpgArtifactException("Joern artifact output is not a service-owned regular file");
        }
        return attributes;
    }

    private static void requireDirectoryWithoutLinks(Path directory) throws IOException {
        var attributes = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory()) {
            throw new JoernCpgArtifactException("Joern artifact directory is not service-owned");
        }
    }

    private static boolean stableFile(BasicFileAttributes before, BasicFileAttributes after) {
        return before.size() == after.size()
            && Objects.equals(before.fileKey(), after.fileKey())
            && Objects.equals(before.lastModifiedTime(), after.lastModifiedTime());
    }

    private void rejectSymlinkSegments(Path relativePath) throws IOException {
        requireDirectoryWithoutLinks(artifactRoot);
        var probe = artifactRoot;
        for (var segment : relativePath) {
            probe = probe.resolve(segment);
            if (Files.exists(probe, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(probe)) {
                throw new JoernCpgArtifactException("semantic artifact path must not contain symbolic links");
            }
        }
    }

    private static String artifactDirectory(AnalyzeJoernCpgCommand command) {
        var fingerprint = de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256(
            command.metadata().analysisRunId().value()
                + "|"
                + command.metadata().analysisJobId().value()
                + "|"
                + command.metadata().sourceSnapshotId().value()
        ).substring(0, 24);
        return "joern-cpg/" + fingerprint;
    }

    private static String artifactDirectory(SemanticArtifactBytesRequest request) {
        var fingerprint = de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256(
            request.analysisRunId().value()
                + "|"
                + request.analysisJobId().value()
                + "|"
                + request.sourceSnapshotId().value()
        ).substring(0, 24);
        return "joern-cpg/" + fingerprint;
    }

    private record HashResult(long sizeBytes, String sha256) {
    }
}
