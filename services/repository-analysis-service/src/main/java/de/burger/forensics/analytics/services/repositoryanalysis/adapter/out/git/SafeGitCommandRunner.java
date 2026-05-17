package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class SafeGitCommandRunner implements GitCommandRunner {
    private static final int OUTPUT_LIMIT = 16 * 1024;
    private final String executable;

    public SafeGitCommandRunner() {
        this("git");
    }

    SafeGitCommandRunner(String executable) {
        this.executable = executable;
    }

    @Override
    public GitCommandResult run(GitCommand command) {
        try {
            Files.createDirectories(command.workingDirectory());
            var arguments = new ArrayList<String>();
            arguments.add(executable);
            arguments.add("-c");
            arguments.add("core.hooksPath=/dev/null");
            arguments.add("-c");
            arguments.add("credential.helper=");
            arguments.add("-c");
            arguments.add("protocol.file.allow=never");
            arguments.add("-c");
            arguments.add("protocol.ext.allow=never");
            arguments.add("-c");
            arguments.add("filter.lfs.required=false");
            arguments.add("-c");
            arguments.add("filter.lfs.smudge=");
            arguments.add("-c");
            arguments.add("filter.lfs.clean=");
            arguments.addAll(command.arguments());
            var builder = new ProcessBuilder(arguments)
                .directory(command.workingDirectory().toFile())
                .redirectErrorStream(true);
            configureEnvironment(builder.environment(), command);
            var process = builder.start();
            var output = CompletableFuture.supplyAsync(() -> boundedOutput(process));
            var completed = process.waitFor(command.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new GitCommandResult(124, output.join());
            }
            return new GitCommandResult(process.exitValue(), output.join());
        } catch (IOException error) {
            throw new IllegalStateException("Failed to run git command", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running git command", error);
        }
    }

    private static void configureEnvironment(Map<String, String> environment, GitCommand command) throws IOException {
        var path = environment.get("PATH");
        environment.clear();
        if (path != null && !path.isBlank()) {
            environment.put("PATH", path);
        }
        var gitHome = command.workingDirectory().resolve(".git-home");
        Files.createDirectories(gitHome);
        environment.put("HOME", gitHome.toString());
        environment.put("XDG_CONFIG_HOME", gitHome.resolve(".config").toString());
        environment.put("GIT_TERMINAL_PROMPT", "0");
        environment.put("GIT_ASKPASS", "");
        environment.put("SSH_ASKPASS", "");
        environment.put("GIT_CONFIG_NOSYSTEM", "1");
    }

    private static String boundedOutput(Process process) {
        try (var input = process.getInputStream(); var output = new ByteArrayOutputStream()) {
            var buffer = new byte[512];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() < OUTPUT_LIMIT) {
                    output.write(buffer, 0, Math.min(read, OUTPUT_LIMIT - output.size()));
                }
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            return "";
        }
    }

    static GitCommand command(java.nio.file.Path workingDirectory, Duration timeout, String... arguments) {
        return new GitCommand(workingDirectory, timeout, java.util.List.of(arguments));
    }
}
