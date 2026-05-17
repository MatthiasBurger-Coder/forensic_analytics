package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeGitCommandRunnerTest {
    @TempDir
    private Path workspace;

    @Test
    void sanitizesEnvironmentAndPinsGitSafetyOptions() throws Exception {
        var runner = new SafeGitCommandRunner(fakeGit());

        var result = runner.run(new GitCommand(workspace, Duration.ofSeconds(5), List.of("status")));

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("core.hooksPath=/dev/null"));
        assertTrue(result.output().contains("credential.helper="));
        assertTrue(result.output().contains("protocol.file.allow=never"));
        assertTrue(result.output().contains("protocol.ext.allow=never"));
        assertTrue(result.output().contains("GIT_TERMINAL_PROMPT=0"));
        assertTrue(result.output().contains("GIT_CONFIG_NOSYSTEM=1"));
    }

    @Test
    void capsOutputAndReturnsTimeoutExitCode() throws Exception {
        var runner = new SafeGitCommandRunner(fakeGit());

        var loud = runner.run(new GitCommand(workspace, Duration.ofSeconds(5), List.of("loud")));
        var timedOut = runner.run(new GitCommand(workspace, Duration.ofMillis(100), List.of("sleep")));

        assertTrue(loud.output().length() <= 16 * 1024);
        assertEquals(124, timedOut.exitCode());
    }

    @Test
    void reportsExecutableStartFailuresWithoutRawCommandOutput() {
        var runner = new SafeGitCommandRunner(workspace.resolve("missing-git").toString());

        var failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> runner.run(
            new GitCommand(workspace, Duration.ofSeconds(1), List.of("status"))
        ));

        assertEquals("Failed to run git command", failure.getMessage());
    }

    private String fakeGit() throws Exception {
        var script = workspace.resolve("fake-git.sh");
        Files.writeString(script, """
            #!/bin/sh
            echo "ARGS:$*"
            echo "GIT_TERMINAL_PROMPT=$GIT_TERMINAL_PROMPT"
            echo "GIT_CONFIG_NOSYSTEM=$GIT_CONFIG_NOSYSTEM"
            for arg in "$@"; do
              if [ "$arg" = "loud" ]; then
                /usr/bin/yes x | /usr/bin/head -c 20000
              fi
              if [ "$arg" = "sleep" ]; then
                /bin/sleep 5
              fi
            done
            exit 0
            """);
        assertTrue(script.toFile().setExecutable(true));
        return script.toString();
    }
}
