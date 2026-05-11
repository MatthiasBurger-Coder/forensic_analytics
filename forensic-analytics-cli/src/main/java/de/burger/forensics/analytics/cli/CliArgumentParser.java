package de.burger.forensics.analytics.cli;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class CliArgumentParser {
    static final String USAGE = """
        Usage:
          forensic-analytics analyze --repo <path-or-file-uri> --profile <profile> --output <directory> --joern-mode <off|docker>
          forensic-analytics ingest-request --request <engine-request.json> --output <directory>
        """;

    private static final String ANALYZE = "analyze";
    private static final String INGEST_REQUEST = "ingest-request";
    private static final String REPO = "--repo";
    private static final String PROFILE = "--profile";
    private static final String OUTPUT = "--output";
    private static final String JOERN_MODE = "--joern-mode";
    private static final String REQUEST = "--request";
    private static final Set<String> ANALYZE_OPTIONS = Set.of(REPO, PROFILE, OUTPUT, JOERN_MODE);
    private static final Set<String> INGEST_REQUEST_OPTIONS = Set.of(REQUEST, OUTPUT);

    CliCommand parse(String[] args) {
        if (args == null || args.length == 0) {
            throw new CliUsageException("Missing command.");
        }
        if ("--help".equals(args[0]) || "help".equals(args[0])) {
            return new HelpCommand();
        }
        return switch (args[0]) {
            case ANALYZE -> parseAnalyze(args);
            case INGEST_REQUEST -> parseIngestRequest(args);
            default -> throw new CliUsageException("Unknown command: " + args[0]);
        };
    }

    private static AnalyzeCommand parseAnalyze(String[] args) {
        var options = parseOptions(ANALYZE, args, ANALYZE_OPTIONS);
        return new AnalyzeCommand(
            required(ANALYZE, options, REPO),
            required(ANALYZE, options, PROFILE),
            Path.of(required(ANALYZE, options, OUTPUT)),
            JoernMode.parse(required(ANALYZE, options, JOERN_MODE))
        );
    }

    private static EngineRequestImportCommand parseIngestRequest(String[] args) {
        var options = parseOptions(INGEST_REQUEST, args, INGEST_REQUEST_OPTIONS);
        return new EngineRequestImportCommand(
            Path.of(required(INGEST_REQUEST, options, REQUEST)),
            Path.of(required(INGEST_REQUEST, options, OUTPUT))
        );
    }

    private static Map<String, String> parseOptions(String commandName, String[] args, Set<String> allowedOptions) {
        var options = new LinkedHashMap<String, String>();
        for (var index = 1; index < args.length; index++) {
            var option = args[index];
            if (!allowedOptions.contains(option)) {
                throw new CliUsageException("Unknown " + commandName + " option: " + option);
            }
            if (options.containsKey(option)) {
                throw new CliUsageException("Duplicate " + commandName + " option: " + option);
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new CliUsageException("Missing value for " + commandName + " option: " + option);
            }
            options.put(option, args[++index]);
        }
        return options;
    }

    private static String required(String commandName, Map<String, String> options, String option) {
        var value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new CliUsageException("Missing required " + commandName + " option: " + option);
        }
        return value;
    }
}
