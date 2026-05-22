package de.burger.forensics.analytics.services.cliclient.bootstrap;

import de.burger.forensics.analytics.services.cliclient.adapter.in.cli.CliClientRunner;

import java.io.PrintStream;
import java.util.Objects;

public final class CliClientApplication {
    private CliClientApplication() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream standardOutput, PrintStream errorOutput) {
        var checkedArgs = Objects.requireNonNullElse(args, new String[0]);
        var checkedStandardOutput = Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        var checkedErrorOutput = Objects.requireNonNull(errorOutput, "errorOutput must not be null");
        var runner = defaultRunner(checkedStandardOutput, checkedErrorOutput);
        return runner.run(checkedArgs);
    }

    static CliClientRunner defaultRunner(PrintStream standardOutput, PrintStream errorOutput) {
        var checkedStandardOutput = Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        var checkedErrorOutput = Objects.requireNonNull(errorOutput, "errorOutput must not be null");
        return CliClientRunner.defaultRunner(checkedStandardOutput, checkedErrorOutput);
    }
}
