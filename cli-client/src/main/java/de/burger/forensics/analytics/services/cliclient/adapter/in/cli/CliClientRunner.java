package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

import com.google.gson.Gson;
import de.burger.forensics.analytics.services.cliclient.adapter.out.http.HttpRepositoryAnalysisSubmissionClient;
import de.burger.forensics.analytics.services.cliclient.application.CliClientSubmissionService;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientValidationException;

import java.io.PrintStream;
import java.net.http.HttpClient;
import java.util.Objects;

public final class CliClientRunner {
    private final CliArgumentParser parser;
    private final CliClientSubmissionService submissionService;
    private final CliClientOutput output;
    private final PrintStream standardOutput;
    private final PrintStream errorOutput;

    public CliClientRunner(
        CliClientSubmissionService submissionService,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        this(new CliArgumentParser(), submissionService, new CliClientOutput(), standardOutput, errorOutput);
    }

    private CliClientRunner(
        CliArgumentParser parser,
        CliClientSubmissionService submissionService,
        CliClientOutput output,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.submissionService = Objects.requireNonNull(submissionService, "submissionService must not be null");
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.standardOutput = Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput must not be null");
    }

    public static CliClientRunner defaultRunner(PrintStream standardOutput, PrintStream errorOutput) {
        return new CliClientRunner(
            new CliClientSubmissionService(new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson())),
            standardOutput,
            errorOutput
        );
    }

    public int run(String[] args) {
        try {
            return runParsed(parser.parse(args));
        } catch (CliClientValidationException e) {
            errorOutput.println(e.getMessage());
            errorOutput.print(CliArgumentParser.USAGE);
            return 2;
        } catch (RuntimeException e) {
            errorOutput.println("Command failed: " + e.getMessage());
            return 1;
        }
    }

    private int runParsed(CliClientCommand command) {
        if (command instanceof HelpCommand) {
            standardOutput.print(CliArgumentParser.USAGE);
            return 0;
        }
        if (command instanceof GatewaySubmitCliCommand gatewaySubmit) {
            var result = submissionService.submit(gatewaySubmit.command());
            standardOutput.print(output.format(result));
            return 0;
        }
        throw new IllegalStateException("Unsupported CLI command.");
    }
}
