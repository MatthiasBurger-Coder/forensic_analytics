package de.burger.forensics.analytics.adapter.joern.docker;

public interface JoernDockerCommandRunner {
    JoernDockerCommandResult run(JoernDockerCommand command);
}
