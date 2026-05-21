package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(result.output().contains("http.followRedirects=false"));
        assertTrue(result.output().contains("GIT_TERMINAL_PROMPT=0"));
        assertTrue(result.output().contains("GIT_CONFIG_NOSYSTEM=1"));
        assertTrue(result.output().contains("GIT_CONFIG_GLOBAL="));
        assertTrue(result.output().contains("GIT_CONFIG_SYSTEM="));
        assertFalse(result.output().contains("HOME=" + workspace.resolve(".git-home")));
    }

    @Test
    void ignoresRepositoryOwnedGlobalGitConfig() throws Exception {
        var repository = workspace.resolve("repository");
        Files.createDirectories(repository.resolve(".git-home"));
        var marker = workspace.resolve("malicious-filter-marker");
        Files.writeString(repository.resolve(".git-home").resolve(".gitconfig"), """
            [filter "malicious"]
                clean = touch %s
                required = true
            """.formatted(marker.toAbsolutePath()));
        Files.writeString(repository.resolve(".gitattributes"), "payload.txt filter=malicious\n");
        Files.writeString(repository.resolve("payload.txt"), "payload");
        var runner = new SafeGitCommandRunner();

        var init = runner.run(new GitCommand(repository, Duration.ofSeconds(5), List.of("init", "--quiet")));
        var add = runner.run(new GitCommand(repository, Duration.ofSeconds(5), List.of("add", "payload.txt")));

        assertEquals(0, init.exitCode(), init.output());
        assertEquals(0, add.exitCode(), add.output());
        assertFalse(Files.exists(marker), "repository-owned git config must not execute filters");
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
            echo "HOME=$HOME"
            echo "XDG_CONFIG_HOME=$XDG_CONFIG_HOME"
            echo "GIT_TERMINAL_PROMPT=$GIT_TERMINAL_PROMPT"
            echo "GIT_CONFIG_NOSYSTEM=$GIT_CONFIG_NOSYSTEM"
            echo "GIT_CONFIG_GLOBAL=$GIT_CONFIG_GLOBAL"
            echo "GIT_CONFIG_SYSTEM=$GIT_CONFIG_SYSTEM"
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
