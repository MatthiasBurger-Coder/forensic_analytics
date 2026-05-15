package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.ingestion.request.EngineIngestionRequestException;
import de.burger.forensics.analytics.ingestion.request.EngineIngestionRequestImporter;
import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.OperationLogger;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ForensicAnalyticsCli {
    private final CliArgumentParser parser;
    private final Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory;
    private final AnalysisResultOutput resultOutput;
    private final Function<EngineRequestImportCommand, EngineIngestionRequestImporter> requestImporterFactory;
    private final EngineRequestImportOutput requestImportOutput;
    private final PrintStream standardOutput;
    private final PrintStream errorOutput;
    private final OperationLogger operationLogger;

    public ForensicAnalyticsCli(
        RunRepositoryAnalysisUseCase useCase,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        this(
            ignored -> Objects.requireNonNull(useCase, "useCase must not be null"),
            defaultRequestImporterFactory(),
            new CliArgumentParser(),
            new AnalysisResultOutput(),
            new EngineRequestImportOutput(),
            standardOutput,
            errorOutput,
            OperationLogger.system(ForensicAnalyticsCli.class)
        );
    }

    private ForensicAnalyticsCli(
        Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory,
        Function<EngineRequestImportCommand, EngineIngestionRequestImporter> requestImporterFactory,
        CliArgumentParser parser,
        AnalysisResultOutput resultOutput,
        EngineRequestImportOutput requestImportOutput,
        PrintStream standardOutput,
        PrintStream errorOutput,
        OperationLogger operationLogger
    ) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.useCaseFactory = Objects.requireNonNull(useCaseFactory, "useCaseFactory must not be null");
        this.resultOutput = Objects.requireNonNull(resultOutput, "resultOutput must not be null");
        this.requestImporterFactory = Objects.requireNonNull(requestImporterFactory, "requestImporterFactory must not be null");
        this.requestImportOutput = Objects.requireNonNull(requestImportOutput, "requestImportOutput must not be null");
        this.standardOutput = Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput must not be null");
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
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
        return withFactories(useCaseFactory, defaultRequestImporterFactory(), standardOutput, errorOutput);
    }

    static ForensicAnalyticsCli withFactories(
        Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory,
        Function<EngineRequestImportCommand, EngineIngestionRequestImporter> requestImporterFactory,
        PrintStream standardOutput,
        PrintStream errorOutput
    ) {
        return withFactories(
            useCaseFactory,
            requestImporterFactory,
            standardOutput,
            errorOutput,
            OperationLogger.system(ForensicAnalyticsCli.class)
        );
    }

    static ForensicAnalyticsCli withFactories(
        Function<AnalyzeCommand, RunRepositoryAnalysisUseCase> useCaseFactory,
        Function<EngineRequestImportCommand, EngineIngestionRequestImporter> requestImporterFactory,
        PrintStream standardOutput,
        PrintStream errorOutput,
        OperationLogger operationLogger
    ) {
        return new ForensicAnalyticsCli(
            useCaseFactory,
            requestImporterFactory,
            new CliArgumentParser(),
            new AnalysisResultOutput(),
            new EngineRequestImportOutput(),
            standardOutput,
            errorOutput,
            operationLogger
        );
    }

    private static Function<EngineRequestImportCommand, EngineIngestionRequestImporter> defaultRequestImporterFactory() {
        return ignored -> new EngineIngestionRequestImporter(
            new DefaultForensicIngestionUseCase(new InMemoryIngestionSessionRepository())
        );
    }

    public int run(String[] args) {
        try {
            return runParsedWithLogging(parser.parse(args));
        } catch (CliUsageException e) {
            errorOutput.println(e.getMessage());
            errorOutput.print(CliArgumentParser.USAGE);
            return 2;
        } catch (CliConfigurationException | EngineIngestionRequestException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
            errorOutput.println("Command failed: " + e.getMessage());
            return 1;
        }
    }

    private int runParsedWithLogging(CliCommand command) {
        try (var correlationScope = CorrelationContext.openGenerated()) {
            correlationScope.correlationId();
            var operation = operationName(command);
            var startedAt = System.nanoTime();
            operationLogger.started(operation);
            try {
                var exitCode = runParsed(command);
                operationLogger.succeeded(operation, elapsedMillis(startedAt));
                return exitCode;
            } catch (RuntimeException error) {
                operationLogger.failed(operation, elapsedMillis(startedAt), error);
                throw error;
            }
        }
    }

    private int runParsed(CliCommand command) {
        return switch (command) {
            case HelpCommand ignored -> {
                standardOutput.print(CliArgumentParser.USAGE);
                yield 0;
            }
            case AnalyzeCommand analyze -> runAnalyze(analyze);
            case EngineRequestImportCommand importRequest -> runImportRequest(importRequest);
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

    private int runImportRequest(EngineRequestImportCommand command) {
        var importer = Objects.requireNonNull(
            requestImporterFactory.apply(command),
            "request importer must not be null"
        );
        var result = Objects.requireNonNull(
            importer.importRequest(command.requestFile()),
            "engine request import result must not be null"
        );
        var summaryPath = requestImportOutput.write(command, result);
        standardOutput.print(requestImportOutput.format(command, result));
        standardOutput.println("summaryPath=" + summaryPath);
        return 0;
    }

    private static String operationName(CliCommand command) {
        return switch (command) {
            case AnalyzeCommand ignored -> "cli.analyze";
            case EngineRequestImportCommand ignored -> "cli.ingest-request";
            case HelpCommand ignored -> "cli.help";
        };
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
