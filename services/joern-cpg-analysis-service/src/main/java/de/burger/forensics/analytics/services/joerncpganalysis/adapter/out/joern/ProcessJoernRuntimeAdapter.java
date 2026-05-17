package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.joern;

import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireSha256ImageReference;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.sha256;

public final class ProcessJoernRuntimeAdapter implements JoernRuntimePort {
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
        try {
            Files.createDirectories(artifactRoot.resolve(artifactDirectory));
        } catch (IOException error) {
            throw new JoernRuntimeUnavailableException("Failed to create Joern artifact directory.", error);
        }

        var timeout = Duration.ofSeconds(command.policy().timeoutSeconds());
        var diagnostics = new ArrayList<JoernCpgDiagnostic>();
        var version = joernVersion(command, timeout);
        diagnostics.addAll(version.diagnostics());
        createCpg(command, workspace, artifactDirectory, timeout);
        runOptionalQuery(command, artifactDirectory, "callgraph.sc", CALLGRAPH, command.policy().requireCallgraph(), diagnostics, timeout);
        runOptionalQuery(command, artifactDirectory, "controlflow.sc", CONTROLFLOW, command.policy().requireControlflow(), diagnostics, timeout);
        runOptionalQuery(command, artifactDirectory, "dataflow.sc", DATAFLOW, command.policy().requireDataflow(), diagnostics, timeout);

        return new JoernRuntimeResult(version.value(), runtimeImageReference, artifactDirectory, diagnostics);
    }

    private VersionProbe joernVersion(AnalyzeJoernCpgCommand command, Duration timeout) {
        var result = run(List.of(joernExecutable, "--version"), artifactRoot, timeout);
        if (!result.successful()) {
            return unknownVersion(command);
        }
        var output = result.stdout().isBlank() ? result.stderr() : result.stdout();
        return output.isBlank() ? unknownVersion(command) : new VersionProbe(output.strip(), List.of());
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
            var process = new ProcessBuilder(arguments)
                .directory(workingDirectory.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start();
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    private static String sanitize(String output) {
        var text = output == null ? "" : output.replace('\r', ' ').replace('\n', ' ').replace('\\', '/').strip();
        return text.length() > 240 ? text.substring(0, 240) : text;
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
