package de.burger.forensics.analytics.adapter.joern.docker;

public record JoernDockerCommandResult(int exitCode, String stdout, String stderr) {
    public JoernDockerCommandResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }

    public boolean successful() {
        return exitCode == 0;
    }
}
