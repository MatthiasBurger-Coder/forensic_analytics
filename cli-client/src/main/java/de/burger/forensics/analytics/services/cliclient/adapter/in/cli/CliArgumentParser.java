package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientValidationException;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CliArgumentParser {
    static final String USAGE = """
        Usage:
          cli-client gateway-submit --gateway <query-report-api-base-url> --repo-url <https-url> --branch <branch> --request-id <id> --schema-version gateway.v1 --requested-outputs BTM_RULES --build-tool <tool> --build-id <id> --root-project <name> --declared-modules <comma-list> --correlation-id <id> --idempotency-key <key> --timeout-seconds <seconds> --max-workspace-bytes <bytes> --allow-shallow-clone <true|false>
        """;

    private static final String GATEWAY_SUBMIT = "gateway-submit";
    private static final String GATEWAY = "--gateway";
    private static final String REPO_URL = "--repo-url";
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

    CliClientCommand parse(String[] args) {
        if (args == null || args.length == 0) {
            throw new CliClientValidationException("Missing command.");
        }
        if ("--help".equals(args[0]) || "help".equals(args[0])) {
            return new HelpCommand();
        }
        if (GATEWAY_SUBMIT.equals(args[0])) {
            return parseGatewaySubmit(args);
        }
        throw new CliClientValidationException("Unknown command: " + args[0]);
    }

    private static GatewaySubmitCliCommand parseGatewaySubmit(String[] args) {
        var options = parseOptions(args);
        return new GatewaySubmitCliCommand(new CliClientSubmissionCommand(
            parseUri(required(options, GATEWAY)),
            required(options, REPO_URL),
            optional(options, BRANCH),
            optional(options, COMMIT),
            required(options, REQUEST_ID),
            required(options, SCHEMA_VERSION),
            parseCsv(required(options, REQUESTED_OUTPUTS), REQUESTED_OUTPUTS),
            optional(options, PROVIDER),
            required(options, BUILD_TOOL),
            required(options, BUILD_ID),
            required(options, ROOT_PROJECT),
            parseCsv(required(options, DECLARED_MODULES), DECLARED_MODULES),
            required(options, CORRELATION_ID),
            required(options, IDEMPOTENCY_KEY),
            parseLong(required(options, TIMEOUT_SECONDS), TIMEOUT_SECONDS),
            parseLong(required(options, MAX_WORKSPACE_BYTES), MAX_WORKSPACE_BYTES),
            parseBoolean(required(options, ALLOW_SHALLOW_CLONE), ALLOW_SHALLOW_CLONE)
        ));
    }

    private static Map<String, String> parseOptions(String[] args) {
        var options = new LinkedHashMap<String, String>();
        for (var index = 1; index < args.length; index++) {
            var option = args[index];
            if (!GATEWAY_SUBMIT_OPTIONS.contains(option)) {
                throw new CliClientValidationException("Unknown gateway-submit option: " + option);
            }
            if (options.containsKey(option)) {
                throw new CliClientValidationException("Duplicate gateway-submit option: " + option);
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new CliClientValidationException("Missing value for gateway-submit option: " + option);
            }
            options.put(option, args[++index]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String option) {
        var value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new CliClientValidationException("Missing required gateway-submit option: " + option);
        }
        return value;
    }

    private static String optional(Map<String, String> options, String option) {
        return options.getOrDefault(option, "");
    }

    private static URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new CliClientValidationException("Invalid gateway-submit option --gateway.");
        }
    }

    private static List<String> parseCsv(String value, String option) {
        var values = Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isBlank())
            .toList();
        if (values.isEmpty()) {
            throw new CliClientValidationException("Missing value for gateway-submit option: " + option);
        }
        return values;
    }

    private static long parseLong(String value, String option) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CliClientValidationException("Invalid numeric gateway-submit option " + option + ".");
        }
    }

    private static boolean parseBoolean(String value, String option) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new CliClientValidationException("Invalid boolean gateway-submit option " + option + ".");
    }
}
