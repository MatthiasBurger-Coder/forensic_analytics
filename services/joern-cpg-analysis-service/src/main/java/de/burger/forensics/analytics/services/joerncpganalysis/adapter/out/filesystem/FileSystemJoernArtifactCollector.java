package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.completeness;

public final class FileSystemJoernArtifactCollector implements JoernArtifactCollectorPort {
    public static final String CPG = "cpg.bin.zip";
    public static final String CALLGRAPH = "callgraph.json";
    public static final String CONTROLFLOW = "controlflow.json";
    public static final String DATAFLOW = "dataflow.json";
    public static final String SLICES = "slices.json";
    public static final String PROVENANCE = "joern-provenance.json";
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
            if (Files.isRegularFile(file)) {
                artifacts.add(reference(command, file));
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

    private Path artifactDirectory(JoernRuntimeResult runtimeResult) {
        var directory = artifactRoot.resolve(runtimeResult.artifactDirectory()).normalize();
        if (!directory.startsWith(artifactRoot)) {
            throw new JoernCpgArtifactException("Joern artifact directory resolves outside service artifact root");
        }
        try {
            Files.createDirectories(directory);
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

    private AnalysisArtifactReference reference(AnalyzeJoernCpgCommand command, Path file) {
        return reference(command, file, AnalysisCompleteness.COMPLETE);
    }

    private AnalysisArtifactReference reference(
        AnalyzeJoernCpgCommand command,
        Path file,
        AnalysisCompleteness artifactCompleteness
    ) {
        try {
            var artifact = new ArtifactReference(relativePath(file), artifactType(file), sha256(file), Files.size(file));
            return new AnalysisArtifactReference(
                artifact,
                AnalysisArtifactCategory.STATIC,
                PRODUCER_SERVICE,
                SEMANTIC_ARTIFACT_SCHEMA_VERSION,
                artifactCompleteness
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
            Files.writeString(provenance, GSON.toJson(metadata), StandardCharsets.UTF_8);
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

    private static String sha256(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available.", error);
        }
    }
}
