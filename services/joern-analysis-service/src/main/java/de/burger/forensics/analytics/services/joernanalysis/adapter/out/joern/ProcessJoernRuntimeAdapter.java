package de.burger.forensics.analytics.services.joernanalysis.adapter.out.joern;

import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.SLICES;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.requireSha256ImageReference;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256;

public final class ProcessJoernRuntimeAdapter implements JoernRuntimePort {
    private static final Pattern PRIVATE_PATH = Pattern.compile("(^|\\s)(/[A-Za-z0-9._-]+)+");
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?i)(^|\\s)[a-z]:[/\\\\][^\\s]+");
    private static final Pattern URI = Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://\\S+");
    private static final Pattern SOURCE_DECLARATION = Pattern.compile("(?i)(^|\\s)(class|interface|enum|record)\\s+[A-Za-z_$][A-Za-z0-9_$]*\\b");
    private static final Pattern SAFE_VERSION = Pattern.compile("(?i)^(joern[\\w .+\\-]*\\d[\\w .+\\-]*|v?\\d+(\\.\\d+){0,5}([-+][A-Za-z0-9._-]+)?)$");
    private static final List<String> SENSITIVE_TOKENS = List.of(
        "authorization",
        "credential",
        "password",
        "secret",
        "token",
        "apikey",
        "api_key"
    );

    private final Path artifactRoot;
    private final Path queryScriptsRoot;
    private final String joernExecutable;
    private final String joernParseExecutable;
    private final String javaHeap;
    private final String runtimeImageReference;

    public ProcessJoernRuntimeAdapter(
        Path artifactRoot,
        Path queryScriptsRoot,
        String joernExecutable,
        String joernParseExecutable,
        String javaHeap,
        String runtimeImageReference
    ) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifact root must not be null")
            .toAbsolutePath()
            .normalize();
        this.queryScriptsRoot = Objects.requireNonNull(queryScriptsRoot, "query scripts root must not be null")
            .toAbsolutePath()
            .normalize();
        this.joernExecutable = requireText(joernExecutable, "Joern executable");
        this.joernParseExecutable = requireText(joernParseExecutable, "Joern parse executable");
        this.javaHeap = requireText(javaHeap, "Joern heap");
        this.runtimeImageReference = requireSha256ImageReference(runtimeImageReference, "Joern runtime image reference");
    }

    @Override
    public JoernRuntimeResult analyze(AnalyzeJoernCpgCommand command, ResolvedJoernWorkspace workspace) {
        if (!runtimeImageReference.equals(command.policy().joernImageReference())) {
            throw new JoernRuntimeUnavailableException("Configured Joern runtime image does not match request policy.");
        }
        var artifactDirectory = artifactDirectory(command);
        prepareArtifactDirectory(artifactDirectory);

        var timeout = Duration.ofSeconds(command.policy().timeoutSeconds());
        var diagnostics = new ArrayList<JoernCpgDiagnostic>();
        var version = joernVersion(command, timeout);
        diagnostics.addAll(version.diagnostics());
        createCpg(command, workspace, artifactDirectory, timeout);
        runOptionalQuery(command, artifactDirectory, "callgraph.sc", CALLGRAPH, command.policy().requireCallgraph(), diagnostics, timeout);
        runOptionalQuery(command, artifactDirectory, "controlflow.sc", CONTROLFLOW, command.policy().requireControlflow(), diagnostics, timeout);
        runOptionalQuery(command, artifactDirectory, "dataflow.sc", DATAFLOW, command.policy().requireDataflow(), diagnostics, timeout);
        runOptionalQuery(command, artifactDirectory, "slices.sc", SLICES, command.policy().requireDataflow(), diagnostics, timeout);

        return new JoernRuntimeResult(version.value(), runtimeImageReference, artifactDirectory, diagnostics);
    }

    private VersionProbe joernVersion(AnalyzeJoernCpgCommand command, Duration timeout) {
        var result = run(List.of(joernExecutable, "--version"), artifactRoot, timeout);
        if (!result.successful()) {
            return unknownVersion(command);
        }
        var output = result.stdout().isBlank() ? result.stderr() : result.stdout();
        var version = safeVersion(output);
        return version.isBlank() ? unknownVersion(command) : new VersionProbe(version, List.of());
    }

    private VersionProbe unknownVersion(AnalyzeJoernCpgCommand command) {
        return new VersionProbe(
            "UNKNOWN",
            List.of(JoernCpgDiagnostic.warning(
                command.metadata().sourceSnapshotId(),
                "JOERN_VERSION_UNAVAILABLE",
                "Joern runtime version could not be verified.",
                "",
                true
            ))
        );
    }

    private void createCpg(
        AnalyzeJoernCpgCommand command,
        ResolvedJoernWorkspace workspace,
        String artifactDirectory,
        Duration timeout
    ) {
        var arguments = new ArrayList<String>();
        arguments.add(joernParseExecutable);
        arguments.add("-J-Xmx" + javaHeap);
        workspace.sourceRootPaths().stream().map(Path::toString).forEach(arguments::add);
        arguments.add("--language");
        arguments.add("javasrc");
        arguments.add("--output");
        arguments.add(artifactRoot.resolve(artifactDirectory).resolve(CPG).toString());
        var result = run(arguments, workspace.workspacePath(), timeout);
        if (!result.successful()) {
            throw new JoernRuntimeUnavailableException(
                "Joern CPG creation failed with exit code " + result.exitCode() + ": " + sanitize(result.stderr())
            );
        }
    }

    private void runOptionalQuery(
        AnalyzeJoernCpgCommand command,
        String artifactDirectory,
        String scriptName,
        String outputName,
        boolean required,
        List<JoernCpgDiagnostic> diagnostics,
        Duration timeout
    ) {
        var script = queryScriptsRoot.resolve(scriptName).normalize();
        if (!script.startsWith(queryScriptsRoot) || !Files.isRegularFile(script)) {
            if (required) {
                diagnostics.add(JoernCpgDiagnostic.warning(
                    command.metadata().sourceSnapshotId(),
                    "JOERN_QUERY_SCRIPT_MISSING",
                    "Configured Joern query script is not available.",
                    outputName,
                    true
                ));
            }
            return;
        }
        var cpg = artifactRoot.resolve(artifactDirectory).resolve(CPG);
        var output = artifactRoot.resolve(artifactDirectory).resolve(outputName);
        var arguments = List.of(
            joernExecutable,
            "-J-Xmx" + javaHeap,
            "--script",
            script.toString(),
            "--params",
            "cpg=" + cpg + ",out=" + output
        );
        var result = run(arguments, artifactRoot.resolve(artifactDirectory), timeout);
        if (!result.successful() && required) {
            diagnostics.add(JoernCpgDiagnostic.warning(
                command.metadata().sourceSnapshotId(),
                "JOERN_QUERY_FAILED",
                "Configured Joern query did not produce a complete artifact.",
                outputName,
                true
            ));
        }
    }

    private CommandResult run(List<String> arguments, Path workingDirectory, Duration timeout) {
        Path stdout = null;
        Path stderr = null;
        try {
            stdout = Files.createTempFile(artifactRoot, "joern-stdout-", ".log");
            stderr = Files.createTempFile(artifactRoot, "joern-stderr-", ".log");
            var builder = new ProcessBuilder(arguments)
                .directory(workingDirectory.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile());
            configureEnvironment(builder, workingDirectory);
            var process = builder.start();
            var completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor();
                throw new JoernCpgAnalysisTimeoutException("Joern execution exceeded configured timeout");
            }
            return new CommandResult(
                process.exitValue(),
                Files.readString(stdout, StandardCharsets.UTF_8),
                Files.readString(stderr, StandardCharsets.UTF_8)
            );
        } catch (IOException error) {
            throw new JoernRuntimeUnavailableException("Joern runtime is not available.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new JoernCpgAnalysisTimeoutException("Interrupted while waiting for Joern execution");
        } finally {
            deleteIfPresent(stdout);
            deleteIfPresent(stderr);
        }
    }

    private String artifactDirectory(AnalyzeJoernCpgCommand command) {
        var fingerprint = sha256(
            command.metadata().analysisRunId().value()
                + "|"
                + command.metadata().analysisJobId().value()
                + "|"
                + command.metadata().sourceSnapshotId().value()
        ).substring(0, 24);
        return "joern-cpg/" + fingerprint;
    }

    private void prepareArtifactDirectory(String relativeDirectory) {
        var directory = artifactRoot.resolve(relativeDirectory).normalize();
        if (!directory.startsWith(artifactRoot)) {
            throw new JoernRuntimeUnavailableException("Joern artifact directory resolves outside service artifact root");
        }
        try {
            createServiceOwnedDirectory(artifactRoot, directory);
            clearDirectoryContents(directory);
        } catch (IOException error) {
            throw new JoernRuntimeUnavailableException("Failed to prepare Joern artifact directory.", error);
        }
    }

    private static void createServiceOwnedDirectory(Path root, Path directory) throws IOException {
        Files.createDirectories(root);
        var normalizedRoot = root.toAbsolutePath().normalize();
        var normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(normalizedRoot)) {
            throw new JoernRuntimeUnavailableException("Joern artifact directory resolves outside service artifact root");
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

    private static void clearDirectoryContents(Path directory) throws IOException {
        requireDirectoryWithoutLinks(directory);
        try (var stream = Files.walk(directory)) {
            var paths = stream
                .filter(path -> !path.equals(directory))
                .sorted(Comparator.reverseOrder())
                .toList();
            for (var path : paths) {
                Files.delete(path);
            }
        }
    }

    private static void requireDirectoryWithoutLinks(Path directory) throws IOException {
        var attributes = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory()) {
            throw new JoernRuntimeUnavailableException("Joern artifact directory is not service-owned");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    private static String sanitize(String output) {
        var text = output == null ? "" : output.replace('\r', ' ').replace('\n', ' ').replace('\\', '/').strip();
        if (containsSensitiveToken(text) || PRIVATE_PATH.matcher(text).find() || WINDOWS_PATH.matcher(text).find()) {
            return "diagnostic details redacted";
        }
        var sanitized = URI.matcher(text).replaceAll("[redacted-uri]");
        return sanitized.length() > 240 ? sanitized.substring(0, 240) : sanitized;
    }

    private static String safeVersion(String output) {
        var sanitized = sanitize(output);
        if (sanitized.equals("diagnostic details redacted") || sanitized.contains("[redacted-uri]")) {
            return "";
        }
        return SAFE_VERSION.matcher(sanitized).matches() ? sanitized : "";
    }

    private static void configureEnvironment(ProcessBuilder builder, Path workingDirectory) {
        var environment = builder.environment();
        var path = environment.get("PATH");
        environment.clear();
        if (path != null && !path.isBlank()) {
            environment.put("PATH", path);
        }
        environment.put("HOME", workingDirectory.toString());
        environment.put("LANG", "C.UTF-8");
        environment.put("LC_ALL", "C.UTF-8");
    }

    private static boolean containsSensitiveToken(String value) {
        var normalized = value.toLowerCase(java.util.Locale.ROOT).replace("-", "").replace("_", "");
        var compact = normalized.replace(" ", "");
        return SENSITIVE_TOKENS.stream().anyMatch(normalized::contains)
            || compact.contains("publicclass")
            || SOURCE_DECLARATION.matcher(normalized).find()
            || normalized.contains("package ")
            || normalized.contains("import ");
    }

    private static void deleteIfPresent(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best-effort cleanup; diagnostic output must not hide command results.
        }
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
        private CommandResult {
            stdout = stdout == null ? "" : stdout;
            stderr = stderr == null ? "" : stderr;
        }

        private boolean successful() {
            return exitCode == 0;
        }
    }

    private record VersionProbe(String value, List<JoernCpgDiagnostic> diagnostics) {
        private VersionProbe {
            value = requireText(value, "Joern version");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        }
    }
}
