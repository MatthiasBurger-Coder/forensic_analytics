package de.burger.forensics.analytics.services.cliclient.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliClientApplicationTest {
    @Test
    void runDelegatesToDefaultRunnerWithoutExitingJvm() {
        var standardOutput = new ByteArrayOutputStream();
        var errorOutput = new ByteArrayOutputStream();

        var exitCode = CliClientApplication.run(
            new String[] {"help"},
            stream(standardOutput),
            stream(errorOutput)
        );

        assertEquals(0, exitCode);
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("cli-client gateway-submit"));
        assertEquals("", errorOutput.toString(StandardCharsets.UTF_8));
    }

    @Test
    void defaultRunnerCanBeCreatedWithExplicitStreams() {
        var runner = CliClientApplication.defaultRunner(
            stream(new ByteArrayOutputStream()),
            stream(new ByteArrayOutputStream())
        );

        assertEquals(0, runner.run(new String[] {"--help"}));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
