package de.burger.forensics.analytics.cli;

import java.util.Arrays;

enum JoernMode {
    OFF("off"),
    DOCKER("docker");

    private final String cliValue;

    JoernMode(String cliValue) {
        this.cliValue = cliValue;
    }

    String cliValue() {
        return cliValue;
    }

    static JoernMode parse(String value) {
        return Arrays.stream(values())
            .filter(mode -> mode.cliValue.equals(value))
            .findFirst()
            .orElseThrow(() -> new CliUsageException("Unsupported Joern mode: " + value));
    }
}
