package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.joern;

import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CALLGRAPH;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CONTROLFLOW;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.CPG;
import static de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector.DATAFLOW;
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

        assertEquals(List.of(CALLGRAPH, CONTROLFLOW, DATAFLOW), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.artifactPath())
            .toList());
        assertTrue(Files.isRegularFile(artifactRoot.resolve(result.artifactDirectory()).resolve(CPG)));
    }

    @Test
    void mapsJoernParseFailureToUnavailableRuntime() throws Exception {
        var adapter = new ProcessJoernRuntimeAdapter(
            tempDir.resolve("artifacts"),
            tempDir.resolve("queries"),
            executable("joern", fakeJoernScript()),
            executable("joern-parse", fakeParseScript(7)),
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

    private static String fakeParseScript(int exitCode) {
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
            exit %d
            """.formatted(exitCode);
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
                "joern-cpg-analysis-service-test",
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
            new SourceWorkspace("workspace-1", List.of(new SourceRoot("src/main/java", "java")), List.of())
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
