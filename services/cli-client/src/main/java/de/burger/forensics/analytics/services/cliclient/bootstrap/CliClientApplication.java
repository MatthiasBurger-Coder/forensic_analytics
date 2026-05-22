package de.burger.forensics.analytics.services.cliclient.bootstrap;

import de.burger.forensics.analytics.services.cliclient.adapter.in.cli.CliClientRunner;

public final class CliClientApplication {
    private CliClientApplication() {
    }

    public static void main(String[] args) {
        System.exit(CliClientRunner.defaultRunner(System.out, System.err).run(args));
    }
}
