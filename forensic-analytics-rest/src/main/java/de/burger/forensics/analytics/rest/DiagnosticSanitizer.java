package de.burger.forensics.analytics.rest;

import java.util.regex.Pattern;

final class DiagnosticSanitizer {
    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:[\\\\/][^\\s\"'<>]+");
    private static final Pattern LOCAL_UNIX_PATH = Pattern.compile(
        "(?<!:)\\/(?:mnt|home|tmp|var|etc|Users|workspace|workspaces)[^\\s\"'<>]*"
    );
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
        "(?i)\\b(token|password|secret|credential|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+"
    );
    private static final Pattern STACK_FRAME = Pattern.compile("\\bat\\s+[\\w.$]+\\([^)]*\\)");
    private static final int MAX_DIAGNOSTIC_LENGTH = 240;

    String sanitize(String diagnostic) {
        if (diagnostic == null || diagnostic.isBlank()) {
            return "No diagnostic available";
        }
        var sanitized = diagnostic.replace('\n', ' ').replace('\r', ' ').strip();
        sanitized = SECRET_ASSIGNMENT.matcher(sanitized).replaceAll("$1=<redacted>");
        sanitized = WINDOWS_PATH.matcher(sanitized).replaceAll("<local-path>");
        sanitized = LOCAL_UNIX_PATH.matcher(sanitized).replaceAll("<local-path>");
        sanitized = STACK_FRAME.matcher(sanitized).replaceAll("<stack-frame>");
        if (sanitized.length() > MAX_DIAGNOSTIC_LENGTH) {
            return sanitized.substring(0, MAX_DIAGNOSTIC_LENGTH) + "...";
        }
        return sanitized;
    }
}
