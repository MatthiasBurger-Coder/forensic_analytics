package de.burger.forensics.analytics.services.joernanalysis.adapter.out.joern;

import de.burger.forensics.analytics.services.joernanalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
import static de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.SLICES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessJoernRuntimeAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void createsCpgAndRunsAvailableQueryScripts() throws Exception {
        var artifactRoot = tempDir.resolve("artifacts");
        var queryRoot = tempDir.resolve("queries");
        Files.createDirectories(queryRoot);
        Files.writeString(queryRoot.resolve("callgraph.sc"), "// synthetic fixture");
        Files.writeString(queryRoot.resolve("controlflow.sc"), "// synthetic fixture");
        Files.writeString(queryRoot.resolve("dataflow.sc"), "// synthetic fixture");
        Files.writeString(queryRoot.resolve("slices.sc"), "// synthetic fixture");

        var adapter = new ProcessJoernRuntimeAdapter(
            artifactRoot,
            queryRoot,
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(true, true, true), workspace());
        var output = artifactRoot.resolve(result.artifactDirectory());

        assertEquals("joern-test-1", result.joernVersion());
        assertEquals(List.of(), result.diagnostics());
        assertTrue(Files.isRegularFile(output.resolve(CPG)));
        assertTrue(Files.isRegularFile(output.resolve(CALLGRAPH)));
        assertTrue(Files.isRegularFile(output.resolve(CONTROLFLOW)));
        assertTrue(Files.isRegularFile(output.resolve(DATAFLOW)));
        assertTrue(Files.isRegularFile(output.resolve(SLICES)));
    }

    @Test
    void reportsMissingRequiredQueryScriptsAsIncompleteDiagnostics() throws Exception {
        var artifactRoot = tempDir.resolve("artifacts");
        var queryRoot = tempDir.resolve("queries");
        Files.createDirectories(queryRoot);

        var adapter = new ProcessJoernRuntimeAdapter(
            artifactRoot,
            queryRoot,
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(true, true, true), workspace());

        assertEquals(List.of(CALLGRAPH, CONTROLFLOW, DATAFLOW, SLICES), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.artifactPath())
            .toList());
        assertTrue(Files.isRegularFile(artifactRoot.resolve(result.artifactDirectory()).resolve(CPG)));
    }

    @Test
    void clearsStaleArtifactsBeforeCurrentJoernRun() throws Exception {
        var artifactRoot = tempDir.resolve("artifacts");
        var queryRoot = tempDir.resolve("queries");
        Files.createDirectories(queryRoot);
        var staleDirectory = artifactRoot.resolve(artifactDirectory());
        Files.createDirectories(staleDirectory);
        Files.writeString(staleDirectory.resolve(DATAFLOW), "{\"stale\":true}");
        Files.writeString(staleDirectory.resolve(SLICES), "{\"stale\":true}");

        var adapter = new ProcessJoernRuntimeAdapter(
            artifactRoot,
            queryRoot,
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(false, false, true), workspace());

        assertEquals(List.of(DATAFLOW, SLICES), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.artifactPath())
            .toList());
        assertTrue(Files.isRegularFile(artifactRoot.resolve(result.artifactDirectory()).resolve(CPG)));
        assertTrue(Files.notExists(artifactRoot.resolve(result.artifactDirectory()).resolve(DATAFLOW)));
        assertTrue(Files.notExists(artifactRoot.resolve(result.artifactDirectory()).resolve(SLICES)));
    }

    @Test
    void mapsJoernParseFailureToUnavailableRuntime() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(7, "/mnt/private/App.java token=secret")),
            "64m",
            image()
        );

        var error = assertThrows(JoernRuntimeUnavailableException.class, () -> adapter.analyze(command(false, false, false), workspace()));

        assertTrue(error.getMessage().contains("diagnostic details redacted"));
    }

    @Test
    void sanitizesUrisAndSourceSnippetsFromRuntimeFailures() throws Exception {
        var uriFailure = parseFailureAdapter("uri", "see https://example.test/private/source");
        var windowsFailure = parseFailureAdapter("windows", "C:\\private\\trace.log");
        var sourceSnippetFailure = parseFailureAdapter("source", "public class App {}");
        var packageSnippetFailure = parseFailureAdapter("package", "package demo;");
        var importSnippetFailure = parseFailureAdapter("import", "import java.util.List;");
        var longFailure = parseFailureAdapter("long", "a".repeat(320));

        var uri = assertThrows(JoernRuntimeUnavailableException.class, () -> uriFailure.analyze(command(false, false, false), workspace()));
        var windows = assertThrows(JoernRuntimeUnavailableException.class, () -> windowsFailure.analyze(command(false, false, false), workspace()));
        var source = assertThrows(JoernRuntimeUnavailableException.class, () -> sourceSnippetFailure.analyze(command(false, false, false), workspace()));
        var packageSnippet = assertThrows(JoernRuntimeUnavailableException.class, () -> packageSnippetFailure.analyze(command(false, false, false), workspace()));
        var importSnippet = assertThrows(JoernRuntimeUnavailableException.class, () -> importSnippetFailure.analyze(command(false, false, false), workspace()));
        var truncated = assertThrows(JoernRuntimeUnavailableException.class, () -> longFailure.analyze(command(false, false, false), workspace()));

        assertTrue(uri.getMessage().contains("[redacted-uri]"));
        assertTrue(windows.getMessage().contains("diagnostic details redacted"));
        assertTrue(source.getMessage().contains("diagnostic details redacted"));
        assertTrue(packageSnippet.getMessage().contains("diagnostic details redacted"));
        assertTrue(importSnippet.getMessage().contains("diagnostic details redacted"));
        assertTrue(truncated.getMessage().length() < 300);
    }

    @Test
    void ignoresFailuresForOptionalAvailableScripts() throws Exception {
        var queryRoot = tempDir.resolve("queries");
        Files.createDirectories(queryRoot);
        Files.writeString(queryRoot.resolve("callgraph.sc"), "// synthetic fixture");

        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            queryRoot,
            executable("joern", fakeFailingQueryJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(false, false, false), workspace());

        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void mapsMissingJoernExecutableToUnavailableRuntime() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            "missing-joern-executable",
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        assertThrows(JoernRuntimeUnavailableException.class, () -> adapter.analyze(command(false, false, false), workspace()));
    }

    @Test
    void reportsQueryFailureForRequiredAvailableScript() throws Exception {
        var queryRoot = tempDir.resolve("queries");
        Files.createDirectories(queryRoot);
        Files.writeString(queryRoot.resolve("callgraph.sc"), "// synthetic fixture");

        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            queryRoot,
            executable("joern", fakeFailingQueryJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(true, false, false), workspace());

        assertEquals(List.of("JOERN_QUERY_FAILED"), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.code())
            .toList());
    }

    @Test
    void usesUnknownVersionWhenVersionCommandProducesNoOutput() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeSilentVersionJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(false, false, false), workspace());

        assertEquals("UNKNOWN", result.joernVersion());
        assertEquals(List.of("JOERN_VERSION_UNAVAILABLE"), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.code())
            .toList());
    }

    @Test
    void usesUnknownVersionWhenVersionCommandFails() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeFailingVersionJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            image()
        );

        var result = adapter.analyze(command(false, false, false), workspace());

        assertEquals("UNKNOWN", result.joernVersion());
        assertEquals(List.of("JOERN_VERSION_UNAVAILABLE"), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.code())
            .toList());
    }

    @Test
    void redactsUnsafeJoernVersionOutput() throws Exception {
        var privatePath = versionOutputAdapter("version-path", "/mnt/private/joern");
        var windowsPath = versionOutputAdapter("version-windows", "C:\\private\\joern.exe");
        var uri = versionOutputAdapter("version-uri", "https://example.test/joern");
        var sourceSnippet = versionOutputAdapter("version-source", "public class App {}");
        var packagePrivateSnippet = versionOutputAdapter("version-package-private-source", "class App {}");
        var statementSnippet = versionOutputAdapter("version-statement-source", "return 1");

        assertEquals("UNKNOWN", privatePath.analyze(command(false, false, false), workspace()).joernVersion());
        assertEquals("UNKNOWN", windowsPath.analyze(command(false, false, false), workspace()).joernVersion());
        assertEquals("UNKNOWN", uri.analyze(command(false, false, false), workspace()).joernVersion());
        assertEquals("UNKNOWN", sourceSnippet.analyze(command(false, false, false), workspace()).joernVersion());
        assertEquals("UNKNOWN", packagePrivateSnippet.analyze(command(false, false, false), workspace()).joernVersion());
        assertEquals("UNKNOWN", statementSnippet.analyze(command(false, false, false), workspace()).joernVersion());
    }

    @Test
    void timesOutLongRunningJoernProcess() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeJoernScript()),
            executable("joern-parse", """
                #!/bin/sh
                sleep 2
                exit 0
                """),
            "64m",
            image()
        );

        assertThrows(JoernCpgAnalysisTimeoutException.class, () -> adapter.analyze(commandWithTimeout(false, false, false, 1), workspace()));
    }

    @Test
    void rejectsRuntimeImageMismatchBeforeJoernInvocation() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(0)),
            "64m",
            "ghcr.io/joernio/joern@sha256:" + "b".repeat(64)
        );

        assertThrows(JoernRuntimeUnavailableException.class, () -> adapter.analyze(command(false, false, false), workspace()));
    }

    @Test
    void rejectsInvalidRuntimeConfiguration() {
        assertThrows(NullPointerException.class, () -> new ProcessJoernRuntimeAdapter(null, tempDir, "joern", "joern-parse", "64m", image()));
        assertThrows(NullPointerException.class, () -> new ProcessJoernRuntimeAdapter(tempDir, null, "joern", "joern-parse", "64m", image()));
        assertThrows(IllegalArgumentException.class, () -> new ProcessJoernRuntimeAdapter(tempDir, tempDir, " ", "joern-parse", "64m", image()));
        assertThrows(IllegalArgumentException.class, () -> new ProcessJoernRuntimeAdapter(tempDir, tempDir, "joern", "", "64m", image()));
        assertThrows(IllegalArgumentException.class, () -> new ProcessJoernRuntimeAdapter(tempDir, tempDir, "joern", "joern-parse", null, image()));
        assertThrows(IllegalArgumentException.class, () -> new ProcessJoernRuntimeAdapter(tempDir, tempDir, "joern", "joern-parse", "64m", "ghcr.io/joernio/joern:latest"));
    }

    private ResolvedJoernWorkspace workspace() throws Exception {
        var sourceRoot = tempDir.resolve("workspace/src/main/java");
        Files.createDirectories(sourceRoot);
        Files.writeString(sourceRoot.resolve("App.java"), "class App {}\n");
        return new ResolvedJoernWorkspace(
            new SourceSnapshotId("snapshot-1"),
            "workspace-1",
            tempDir.resolve("workspace"),
            List.of(sourceRoot),
            20
        );
    }

    private String executable(String fileName, String content) throws Exception {
        var executable = tempDir.resolve(fileName);
        Files.writeString(executable, content);
        executable.toFile().setExecutable(true);
        return executable.toString();
    }

    private ProcessJoernRuntimeAdapter parseFailureAdapter(String name, String stderr) throws Exception {
        return new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts-" + name),
            tempDir.resolve("queries-" + name),
            executable("joern-" + name, fakeJoernScript()),
            executable("joern-parse-" + name, fakeParseScript(7, stderr)),
            "64m",
            image()
        );
    }

    private ProcessJoernRuntimeAdapter versionOutputAdapter(String name, String output) throws Exception {
        return new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts-" + name),
            tempDir.resolve("queries-" + name),
            executable("joern-" + name, fakeVersionOutputJoernScript(output)),
            executable("joern-parse-" + name, fakeParseScript(0)),
            "64m",
            image()
        );
    }

    private static String fakeJoernScript() {
        return """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
              echo "joern-test-1"
              exit 0
            fi
            out=""
            while [ "$#" -gt 0 ]; do
              if [ "$1" = "--params" ]; then
                params="$2"
                for pair in $(echo "$params" | tr ',' ' '); do
                  case "$pair" in
                    out=*) out="${pair#out=}" ;;
                  esac
                done
              fi
              shift
            done
            mkdir -p "$(dirname "$out")"
            printf '{}' > "$out"
            exit 0
            """;
    }

    private static String fakeFailingQueryJoernScript() {
        return """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
              echo "joern-test-1"
              exit 0
            fi
            exit 5
            """;
    }

    private static String fakeSilentVersionJoernScript() {
        return """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
              exit 0
            fi
            out=""
            while [ "$#" -gt 0 ]; do
              if [ "$1" = "--params" ]; then
                params="$2"
                for pair in $(echo "$params" | tr ',' ' '); do
                  case "$pair" in
                    out=*) out="${pair#out=}" ;;
                  esac
                done
              fi
              shift
            done
            if [ -n "$out" ]; then
              mkdir -p "$(dirname "$out")"
              printf '{}' > "$out"
            fi
            exit 0
            """;
    }

    private static String fakeFailingVersionJoernScript() {
        return """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
              echo "version unavailable" >&2
              exit 9
            fi
            exit 0
            """;
    }

    private static String fakeVersionOutputJoernScript(String output) {
        return """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
              printf '%%s' '%s'
              exit 0
            fi
            exit 0
            """.formatted(output);
    }

    private static String fakeParseScript(int exitCode) {
        return fakeParseScript(exitCode, "");
    }

    private static String fakeParseScript(int exitCode, String stderr) {
        return """
            #!/bin/sh
            out=""
            while [ "$#" -gt 0 ]; do
              if [ "$1" = "--output" ]; then
                shift
                out="$1"
              fi
              shift
            done
            mkdir -p "$(dirname "$out")"
            printf cpg > "$out"
            printf '%%s' '%s' >&2
            exit %d
            """.formatted(stderr, exitCode);
    }

    private static AnalyzeJoernCpgCommand command(boolean callgraph, boolean controlflow, boolean dataflow) {
        return commandWithTimeout(callgraph, controlflow, dataflow, 60);
    }

    private static AnalyzeJoernCpgCommand commandWithTimeout(
        boolean callgraph,
        boolean controlflow,
        boolean dataflow,
        long timeoutSeconds
    ) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new JoernCpgPolicy(
                2,
                1_000_000,
                1_000_000,
                timeoutSeconds,
                image(),
                "queries-v1",
                callgraph,
                controlflow,
                dataflow
            ),
            new SourceWorkspace("joern-workspace-snapshot-1", List.of(new SourceRoot("src/main/java", "java")), List.of(inputArtifact()))
        );
    }

    private static AnalysisArtifactReference inputArtifact() {
        return new AnalysisArtifactReference(
            new ArtifactReference("source-package.zip", "application/zip", "a".repeat(64), 1),
            AnalysisArtifactCategory.STATIC,
            "repository-analysis-service",
            "source-package-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "repository-analysis-service",
                "repository-analysis.v1.GetRepositoryPreparation",
                "source-snapshot/snapshot-1",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }

    private static String artifactDirectory() {
        return "joern-cpg/" + de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.sha256(
            "run-1|job-1|snapshot-1"
        ).substring(0, 24);
    }
}
