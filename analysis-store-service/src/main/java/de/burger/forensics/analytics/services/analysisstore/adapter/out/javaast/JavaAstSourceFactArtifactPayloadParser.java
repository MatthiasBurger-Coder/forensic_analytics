package de.burger.forensics.analytics.services.analysisstore.adapter.out.javaast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.StaticSourceLocation;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class JavaAstSourceFactArtifactPayloadParser {
    public static final String MEDIA_TYPE = "application/vnd.forensic-analytics.java-ast-source-facts.v1+json";
    private static final Set<String> TOP_LEVEL_FIELDS = Set.of(
        "schemaVersion",
        "analysisRunId",
        "analysisJobId",
        "sourceSnapshotId",
        "summary",
        "sourceFacts",
        "diagnostics"
    );
    private static final Set<String> SUMMARY_FIELDS = Set.of(
        "receivedFileCount",
        "parsedFileCount",
        "skippedFileCount",
        "parseErrorCount",
        "sourceFactCount",
        "parser",
        "parserVersion"
    );
    private static final Set<String> FACT_FIELDS = Set.of(
        "factId",
        "factType",
        "location",
        "signature",
        "summary",
        "evidenceKind"
    );
    private static final Set<String> LOCATION_FIELDS = Set.of(
        "sourcePath",
        "fullyQualifiedClassName",
        "methodName",
        "lineNumber",
        "columnNumber"
    );
    private static final Set<String> DIAGNOSTIC_FIELDS = Set.of(
        "code",
        "message",
        "severity",
        "sourceSnapshotId",
        "sourcePath",
        "lineNumber",
        "columnNumber",
        "retryable",
        "affectsCompleteness"
    );

    public ParsedSourceFacts parse(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisArtifactReference artifact,
        byte[] content
    ) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        Objects.requireNonNull(artifact, "artifact must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (!MEDIA_TYPE.equals(artifact.artifact().type())) {
            return failure("SOURCE_FACT_ARTIFACT_MEDIA_TYPE_UNSUPPORTED", "Java AST source fact artifact media type is unsupported.");
        }
        try {
            var document = JsonParser.parseString(new String(content, StandardCharsets.UTF_8)).getAsJsonObject();
            requireOnlyFields(document, TOP_LEVEL_FIELDS, "source fact artifact");
            requireIdentity(analysisRunId, analysisJobId, sourceSnapshotId, artifact, document);
            var sourceFactElements = document.getAsJsonArray("sourceFacts");
            var diagnostics = diagnostics(document.getAsJsonArray("diagnostics"), sourceSnapshotId);
            var payloadCompleteness = diagnostics.stream().anyMatch(SourceFactPayloadDiagnostic::affectsCompleteness)
                ? AnalysisCompleteness.INCOMPLETE
                : AnalysisCompleteness.COMPLETE;
            var sourceFacts = sourceFacts(sourceFactElements, artifact.path(), payloadCompleteness);
            validateSummary(document.getAsJsonObject("summary"), sourceFactElements.size());
            if (sourceFacts.hasBlockingDiagnostics()) {
                return new ParsedSourceFacts(
                    List.of(),
                    AnalysisCompleteness.INCOMPLETE,
                    sourceFacts.diagnostics()
                );
            }
            var allDiagnostics = new java.util.ArrayList<SourceFactPayloadDiagnostic>();
            allDiagnostics.addAll(sourceFacts.diagnostics());
            allDiagnostics.addAll(diagnostics);
            var completeness = allDiagnostics.stream().anyMatch(SourceFactPayloadDiagnostic::affectsCompleteness)
                    ? AnalysisCompleteness.INCOMPLETE
                    : AnalysisCompleteness.COMPLETE;
            return new ParsedSourceFacts(sourceFacts.facts(), completeness, allDiagnostics);
        } catch (IllegalArgumentException | IllegalStateException | JsonSyntaxException error) {
            return failure("SOURCE_FACT_ARTIFACT_SCHEMA_INVALID", "Java AST source fact artifact payload is invalid.");
        }
    }

    private static void requireIdentity(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisArtifactReference artifact,
        JsonObject document
    ) {
        if (!artifact.schemaVersion().equals(requiredString(document, "schemaVersion"))
            || !analysisRunId.value().equals(requiredString(document, "analysisRunId"))
            || !analysisJobId.value().equals(requiredString(document, "analysisJobId"))
            || !sourceSnapshotId.value().equals(requiredString(document, "sourceSnapshotId"))) {
            throw new IllegalArgumentException("source fact artifact identity mismatch");
        }
    }

    private static void validateSummary(JsonObject summary, int acceptedSourceFactCount) {
        requireOnlyFields(summary, SUMMARY_FIELDS, "source fact summary");
        nonNegative(summary, "receivedFileCount");
        nonNegative(summary, "parsedFileCount");
        nonNegative(summary, "skippedFileCount");
        nonNegative(summary, "parseErrorCount");
        if (nonNegative(summary, "sourceFactCount") != acceptedSourceFactCount) {
            throw new IllegalArgumentException("source fact count mismatch");
        }
        requiredString(summary, "parser");
        requiredString(summary, "parserVersion");
    }

    private static SourceFacts sourceFacts(JsonArray facts, String artifactPath, AnalysisCompleteness payloadCompleteness) {
        var acceptedFacts = new java.util.ArrayList<AcceptedStaticSourceFact>();
        var diagnostics = new java.util.ArrayList<SourceFactPayloadDiagnostic>();
        for (var element : facts) {
            try {
                var fact = element.getAsJsonObject();
                requireOnlyFields(fact, FACT_FIELDS, "source fact");
                var factType = requiredString(fact, "factType");
                if (!"java-method".equals(factType)) {
                    diagnostics.add(new SourceFactPayloadDiagnostic(
                        "UNSUPPORTED_STATIC_FACT_TYPE",
                        "Java AST source fact type is unsupported.",
                        true
                    ));
                    continue;
                }
                var evidenceKind = requiredString(fact, "evidenceKind");
                if (!"STATIC_SOURCE_FACT".equals(evidenceKind)) {
                    diagnostics.add(new SourceFactPayloadDiagnostic(
                        "UNSUPPORTED_STATIC_EVIDENCE_KIND",
                        "Java AST source fact evidence kind is unsupported.",
                        true
                    ));
                    continue;
                }
                acceptedFacts.add(new AcceptedStaticSourceFact(
                    requiredString(fact, "factId"),
                    factType,
                    location(fact.getAsJsonObject("location")),
                    requiredString(fact, "signature"),
                    artifactPath,
                    payloadCompleteness
                ));
            } catch (IllegalArgumentException | IllegalStateException error) {
                diagnostics.add(new SourceFactPayloadDiagnostic(
                    "SOURCE_FACT_ARTIFACT_FACT_INVALID",
                    "Java AST source fact entry is invalid.",
                    true
                ));
            }
        }
        return new SourceFacts(List.copyOf(acceptedFacts), List.copyOf(diagnostics));
    }

    private static StaticSourceLocation location(JsonObject location) {
        requireOnlyFields(location, LOCATION_FIELDS, "source fact location");
        return new StaticSourceLocation(
            safePath(requiredString(location, "sourcePath"), "sourcePath"),
            requiredString(location, "fullyQualifiedClassName"),
            requiredString(location, "methodName"),
            positive(location, "lineNumber"),
            positive(location, "columnNumber")
        );
    }

    private static List<SourceFactPayloadDiagnostic> diagnostics(JsonArray diagnostics, SourceSnapshotId sourceSnapshotId) {
        var parsed = new java.util.ArrayList<SourceFactPayloadDiagnostic>();
        for (var element : diagnostics) {
            var diagnostic = element.getAsJsonObject();
            requireOnlyFields(diagnostic, DIAGNOSTIC_FIELDS, "source fact diagnostic");
            if (!sourceSnapshotId.value().equals(requiredString(diagnostic, "sourceSnapshotId"))) {
                throw new IllegalArgumentException("diagnostic source snapshot id mismatch");
            }
            var sourcePath = requiredStringOrEmpty(diagnostic, "sourcePath");
            if (!sourcePath.isBlank()) {
                safePath(sourcePath, "diagnostic sourcePath");
            }
            nonNegative(diagnostic, "lineNumber");
            nonNegative(diagnostic, "columnNumber");
            requiredBoolean(diagnostic, "retryable");
            var severity = requiredString(diagnostic, "severity");
            if (!Set.of("INFO", "WARNING", "ERROR").contains(severity)) {
                throw new IllegalArgumentException("unsupported diagnostic severity");
            }
            parsed.add(new SourceFactPayloadDiagnostic(
                requiredString(diagnostic, "code"),
                diagnosticMessage(diagnostic),
                requiredBoolean(diagnostic, "affectsCompleteness")
            ));
        }
        return List.copyOf(parsed);
    }

    private static ParsedSourceFacts failure(String code, String message) {
        return new ParsedSourceFacts(
            List.of(),
            AnalysisCompleteness.INCOMPLETE,
            List.of(new SourceFactPayloadDiagnostic(code, message, true))
        );
    }

    private static void requireOnlyFields(JsonObject object, Set<String> allowedFields, String objectName) {
        if (object == null) {
            throw new IllegalArgumentException(objectName + " must be an object");
        }
        if (!object.keySet().equals(allowedFields)) {
            throw new IllegalArgumentException(objectName + " fields do not match the v1 contract");
        }
    }

    private static String requiredString(JsonObject object, String fieldName) {
        var value = requiredElement(object, fieldName);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return value.getAsString();
    }

    private static String safePath(String value, String fieldName) {
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must not contain control characters");
        }
        return ArtifactByteAccess.requirePublicReference(value, fieldName);
    }

    private static String diagnosticMessage(JsonObject object) {
        var message = requiredString(object, "message");
        var lower = message.toLowerCase(Locale.ROOT);
        if (message.chars().anyMatch(Character::isISOControl)
            || lower.contains("file:")
            || lower.contains("://")
            || lower.contains("token")
            || lower.contains("password")
            || lower.contains("secret")
            || lower.contains("credential")
            || lower.contains("authorization")
            || message.matches(".*(^|\\s)/[^\\s]+.*")
            || message.matches(".*(^|\\s)[A-Za-z]:[\\\\/][^\\s]+.*")) {
            throw new IllegalArgumentException("message must not contain private paths, URIs, secrets or control characters");
        }
        return message;
    }

    private static String requiredStringOrEmpty(JsonObject object, String fieldName) {
        var value = requiredElement(object, fieldName);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return value.getAsString();
    }

    private static int nonNegative(JsonObject object, String fieldName) {
        var value = requiredElement(object, fieldName);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
        int number;
        try {
            number = new java.math.BigDecimal(value.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException(fieldName + " must be an integer", error);
        }
        if (number < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return number;
    }

    private static int positive(JsonObject object, String fieldName) {
        var number = nonNegative(object, fieldName);
        if (number < 1) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return number;
    }

    private static boolean requiredBoolean(JsonObject object, String fieldName) {
        var value = requiredElement(object, fieldName);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(fieldName + " must be boolean");
        }
        return value.getAsBoolean();
    }

    private static JsonElement requiredElement(JsonObject object, String fieldName) {
        if (!object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return object.get(fieldName);
    }

    private record SourceFacts(
        List<AcceptedStaticSourceFact> facts,
        List<SourceFactPayloadDiagnostic> diagnostics
    ) {
        boolean hasBlockingDiagnostics() {
            return diagnostics.stream().anyMatch(SourceFactPayloadDiagnostic::affectsCompleteness);
        }
    }

    public record ParsedSourceFacts(
        List<AcceptedStaticSourceFact> facts,
        AnalysisCompleteness completeness,
        List<SourceFactPayloadDiagnostic> diagnostics
    ) {
        public ParsedSourceFacts {
            facts = List.copyOf(Objects.requireNonNull(facts, "facts must not be null"));
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        }
    }

    public record SourceFactPayloadDiagnostic(
        String code,
        String message,
        boolean affectsCompleteness
    ) {
        public SourceFactPayloadDiagnostic {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code must not be blank");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }
}
