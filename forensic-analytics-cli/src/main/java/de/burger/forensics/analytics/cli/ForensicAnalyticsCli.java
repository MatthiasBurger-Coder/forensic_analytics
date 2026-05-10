package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public final class ForensicAnalyticsCli {
    private final CliArgumentParser parser;
    private final Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory;
    private final AnalysisResultOutput resultOutput;
    private final PrintStream standardOutput;
    private final PrintStream errorOutput;

    public ForensicAnalyticsCli(
        RunRepositoryAnalysisUseCase useCase,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        this(
            ignored -> Objects.requireNonNull(useCase, "useCase must not be null"),
            new CliArgumentParser(),
            new AnalysisResultOutput(),
            standardOutput,
            errorOutput
        );
    }

    private ForensicAnalyticsCli(
        Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory,
        CliArgumentParser parser,
        AnalysisResultOutput resultOutput,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.useCaseFactory = Objects.requireNonNull(useCaseFactory, "useCaseFactory must not be null");
        this.resultOutput = Objects.requireNonNull(resultOutput, "resultOutput must not be null");
        this.standardOutput = Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput must not be null");
    }

    public static void main(String[] args) {
        System.exit(runWithServiceLoader(args, System.out, System.err));
    }

    static int runWithServiceLoader(String[] args, PrintStream standardOutput, PrintStream errorOutput) {
        return withUseCaseFactory(
            ignored -> ServiceLoader.load(RunRepositoryAnalysisUseCase.class)
                .findFirst()
                .orElseThrow(() -> new CliConfigurationException("No RunRepositoryAnalysisUseCase service provider is configured.")),
            standardOutput,
            errorOutput
        ).run(args);
    }

    static ForensicAnalyticsCli withUseCaseFactory(
        Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        return new ForensicAnalyticsCli(
            useCaseFactory,
            new CliArgumentParser(),
            new AnalysisResultOutput(),
            standardOutput,
            errorOutput
        );
    }

    public int run(String[] args) {
        try {
            return runParsed(parser.parse(args));
        } catch (CliUsageException e) {
            errorOutput.println(e.getMessage());
            errorOutput.print(CliArgumentParser.USAGE);
            return 2;
        } catch (CliConfigurationException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
            errorOutput.println("Analysis failed: " + e.getMessage());
            return 1;
        }
    }

    private int runParsed(CliCommand command) {
        return switch (command) {
            case HelpCommand ignored -> {
                standardOutput.print(CliArgumentParser.USAGE);
                yield 0;
            }
            case AnalyzeCommand analyze -> runAnalyze(analyze);
        };
    }

    private int runAnalyze(AnalyzeCommand command) {
        var useCase = Objects.requireNonNull(useCaseFactory.apply(command), "useCase must not be null");
        var result = Objects.requireNonNull(
            useCase.run(command.toRunRepositoryAnalysisCommand()),
            "analysis result must not be null"
        );
        var summaryPath = resultOutput.write(command, result);
        standardOutput.print(resultOutput.format(command, result));
        standardOutput.println("summaryPath=" + summaryPath);
        return 0;
    }
}
