package de.burger.forensics.analytics.cli;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CliArgumentParser {
    static final String USAGE = """
        Usage:
          forensic-analytics analyze --repo <path-or-file-uri> --profile <profile> --output <directory> --joern-mode <off|docker>
          forensic-analytics ingest-request --request <engine-request.json> --output <directory>
          forensic-analytics gateway-submit --gateway <gateway-api-base-url> --repo-url <https-url> --branch <branch> --request-id <id> --schema-version <version> --requested-outputs BTM_RULES --build-tool <tool> --build-id <id> --root-project <name> --declared-modules <comma-list> --correlation-id <id> --idempotency-key <key> --timeout-seconds <seconds> --max-workspace-bytes <bytes> --allow-shallow-clone <true|false>
        """;

    private static final String ANALYZE = "analyze";
    private static final String INGEST_REQUEST = "ingest-request";
    private static final String GATEWAY_SUBMIT = "gateway-submit";
    private static final String REPO = "--repo";
    private static final String REPO_URL = "--repo-url";
    private static final String PROFILE = "--profile";
    private static final String OUTPUT = "--output";
    private static final String JOERN_MODE = "--joern-mode";
    private static final String REQUEST = "--request";
    private static final String GATEWAY = "--gateway";
    private static final String BRANCH = "--branch";
    private static final String COMMIT = "--commit";
    private static final String REQUEST_ID = "--request-id";
    private static final String SCHEMA_VERSION = "--schema-version";
    private static final String REQUESTED_OUTPUTS = "--requested-outputs";
    private static final String PROVIDER = "--provider";
    private static final String BUILD_TOOL = "--build-tool";
    private static final String BUILD_ID = "--build-id";
    private static final String ROOT_PROJECT = "--root-project";
    private static final String DECLARED_MODULES = "--declared-modules";
    private static final String CORRELATION_ID = "--correlation-id";
    private static final String IDEMPOTENCY_KEY = "--idempotency-key";
    private static final String TIMEOUT_SECONDS = "--timeout-seconds";
    private static final String MAX_WORKSPACE_BYTES = "--max-workspace-bytes";
    private static final String ALLOW_SHALLOW_CLONE = "--allow-shallow-clone";
    private static final Set<String> ANALYZE_OPTIONS = Set.of(REPO, PROFILE, OUTPUT, JOERN_MODE);
    private static final Set<String> INGEST_REQUEST_OPTIONS = Set.of(REQUEST, OUTPUT);
    private static final Set<String> GATEWAY_SUBMIT_OPTIONS = Set.of(
        GATEWAY,
        REPO_URL,
        BRANCH,
        COMMIT,
        REQUEST_ID,
        SCHEMA_VERSION,
        REQUESTED_OUTPUTS,
        PROVIDER,
        BUILD_TOOL,
        BUILD_ID,
        ROOT_PROJECT,
        DECLARED_MODULES,
        CORRELATION_ID,
        IDEMPOTENCY_KEY,
        TIMEOUT_SECONDS,
        MAX_WORKSPACE_BYTES,
        ALLOW_SHALLOW_CLONE
    );

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
            case GATEWAY_SUBMIT -> parseGatewaySubmit(args);
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

    private static GatewaySubmitCommand parseGatewaySubmit(String[] args) {
        var options = parseOptions(GATEWAY_SUBMIT, args, GATEWAY_SUBMIT_OPTIONS);
        return new GatewaySubmitCommand(
            parseUri(required(GATEWAY_SUBMIT, options, GATEWAY), GATEWAY),
            required(GATEWAY_SUBMIT, options, REPO_URL),
            optional(options, BRANCH),
            optional(options, COMMIT),
            required(GATEWAY_SUBMIT, options, REQUEST_ID),
            required(GATEWAY_SUBMIT, options, SCHEMA_VERSION),
            parseCsv(required(GATEWAY_SUBMIT, options, REQUESTED_OUTPUTS), REQUESTED_OUTPUTS),
            optional(options, PROVIDER),
            required(GATEWAY_SUBMIT, options, BUILD_TOOL),
            required(GATEWAY_SUBMIT, options, BUILD_ID),
            required(GATEWAY_SUBMIT, options, ROOT_PROJECT),
            parseCsv(required(GATEWAY_SUBMIT, options, DECLARED_MODULES), DECLARED_MODULES),
            required(GATEWAY_SUBMIT, options, CORRELATION_ID),
            required(GATEWAY_SUBMIT, options, IDEMPOTENCY_KEY),
            parseLong(required(GATEWAY_SUBMIT, options, TIMEOUT_SECONDS), TIMEOUT_SECONDS),
            parseLong(required(GATEWAY_SUBMIT, options, MAX_WORKSPACE_BYTES), MAX_WORKSPACE_BYTES),
            parseBoolean(required(GATEWAY_SUBMIT, options, ALLOW_SHALLOW_CLONE), ALLOW_SHALLOW_CLONE)
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

    private static String optional(Map<String, String> options, String option) {
        return options.getOrDefault(option, "");
    }

    private static URI parseUri(String value, String option) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new CliUsageException("Invalid gateway-submit option " + option + ": " + value);
        }
    }

    private static List<String> parseCsv(String value, String option) {
        var values = Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isBlank())
            .toList();
        if (values.isEmpty()) {
            throw new CliUsageException("Missing value for gateway-submit option: " + option);
        }
        return values;
    }

    private static long parseLong(String value, String option) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CliUsageException("Invalid numeric gateway-submit option " + option + ": " + value);
        }
    }

    private static boolean parseBoolean(String value, String option) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new CliUsageException("Invalid boolean gateway-submit option " + option + ": " + value);
    }
}
