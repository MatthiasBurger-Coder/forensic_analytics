package de.burger.forensics.analytics.cli;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class CliArgumentParser {
    static final String USAGE = """
        Usage:
          forensic-analytics analyze --repo <path-or-file-uri> --profile <profile> --output <directory> --joern-mode <off|docker>
        """;

    private static final String ANALYZE = "analyze";
    private static final String REPO = "--repo";
    private static final String PROFILE = "--profile";
    private static final String OUTPUT = "--output";
    private static final String JOERN_MODE = "--joern-mode";
    private static final Set<String> ANALYZE_OPTIONS = Set.of(REPO, PROFILE, OUTPUT, JOERN_MODE);

    CliCommand parse(String[] args) {
        if (args == null || args.length == 0) {
            throw new CliUsageException("Missing command.");
        }
        if ("--help".equals(args[0]) || "help".equals(args[0])) {
            return new HelpCommand();
        }
        if (!ANALYZE.equals(args[0])) {
            throw new CliUsageException("Unknown command: " + args[0]);
        }
        return parseAnalyze(args);
    }

    private static AnalyzeCommand parseAnalyze(String[] args) {
        var options = new LinkedHashMap<String, String>();
        for (var index = 1; index < args.length; index++) {
            var option = args[index];
            if (!ANALYZE_OPTIONS.contains(option)) {
                throw new CliUsageException("Unknown analyze option: " + option);
            }
            if (options.containsKey(option)) {
                throw new CliUsageException("Duplicate analyze option: " + option);
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new CliUsageException("Missing value for analyze option: " + option);
            }
            options.put(option, args[++index]);
        }
        return new AnalyzeCommand(
            required(options, REPO),
            required(options, PROFILE),
            Path.of(required(options, OUTPUT)),
            JoernMode.parse(required(options, JOERN_MODE))
        );
    }

    private static String required(Map<String, String> options, String option) {
        var value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new CliUsageException("Missing required analyze option: " + option);
        }
        return value;
    }
}
