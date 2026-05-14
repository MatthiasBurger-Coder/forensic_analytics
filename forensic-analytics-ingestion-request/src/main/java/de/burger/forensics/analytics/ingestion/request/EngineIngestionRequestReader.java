package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.application.ingestion.command.BuildIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.ModuleIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.PluginIdentityCommand;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EngineIngestionRequestReader {
    public EngineIngestionRequest read(Path requestFile) {
        Objects.requireNonNull(requestFile, "requestFile must not be null");
        var normalizedRequestFile = requestFile.toAbsolutePath().normalize();
        try {
            return fromJson(normalizedRequestFile, Files.readString(normalizedRequestFile, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read engine ingestion request " + normalizedRequestFile + ".", exception);
        }
    }

    EngineIngestionRequest fromJson(Path requestFile, String json) {
        Objects.requireNonNull(requestFile, "requestFile must not be null");
        var root = JsonParser.parseObject(json);
        var schemaVersion = text(root, "schemaVersion");
        return new EngineIngestionRequest(
            schemaVersion,
            buildIdentity(object(root, "buildIdentity")),
            moduleIdentity(object(root, "moduleIdentity")),
            pluginIdentity(object(root, "pluginIdentity")),
            payloads(requestFile.toAbsolutePath().normalize().getParent(), list(root, "payloads"))
        );
    }

    private static BuildIdentityCommand buildIdentity(Map<String, Object> buildIdentity) {
        return new BuildIdentityCommand(
            text(buildIdentity, "projectId"),
            text(buildIdentity, "repositoryUrl"),
            text(buildIdentity, "branchName"),
            text(buildIdentity, "commitHash"),
            text(buildIdentity, "buildId"),
            text(buildIdentity, "scanTimestamp")
        );
    }

    private static ModuleIdentityCommand moduleIdentity(Map<String, Object> moduleIdentity) {
        return new ModuleIdentityCommand(
            text(moduleIdentity, "moduleName"),
            text(moduleIdentity, "modulePath")
        );
    }

    private static PluginIdentityCommand pluginIdentity(Map<String, Object> pluginIdentity) {
        return new PluginIdentityCommand(
            text(pluginIdentity, "pluginName"),
            text(pluginIdentity, "pluginVersion")
        );
    }

    private static List<EngineIngestionPayloadReference> payloads(Path requestDirectory, List<Object> payloads) {
        if (payloads.isEmpty()) {
            throw new EngineIngestionRequestException("payloads must not be empty");
        }
        return payloads.stream()
            .map(payload -> payload(requestDirectory, castObject(payload, "payloads[]")))
            .toList();
    }

    private static EngineIngestionPayloadReference payload(Path requestDirectory, Map<String, Object> payload) {
        var descriptor = new AnalysisPayloadDescriptor(
            text(payload, "payloadId"),
            payloadKind(text(payload, "kind")),
            text(payload, "contentType"),
            attributes(object(payload, "attributes"))
        );
        return new EngineIngestionPayloadReference(descriptor, payloadFile(requestDirectory, text(payload, "file")));
    }

    private static AnalysisPayloadKind payloadKind(String kind) {
        try {
            return AnalysisPayloadKind.valueOf(kind);
        } catch (IllegalArgumentException exception) {
            throw new EngineIngestionRequestException("Unsupported engine payload kind: " + kind, exception);
        }
    }

    private static Path payloadFile(Path requestDirectory, String file) {
        var path = Path.of(file);
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return requestDirectory.resolve(path).toAbsolutePath().normalize();
    }

    private static Map<String, String> attributes(Map<String, Object> attributes) {
        var converted = new LinkedHashMap<String, String>();
        attributes.forEach((key, value) -> converted.put(key, castText(value, "attributes." + key)));
        return converted;
    }

    private static Map<String, Object> object(Map<String, Object> source, String fieldName) {
        return castObject(required(source, fieldName), fieldName);
    }

    private static List<Object> list(Map<String, Object> source, String fieldName) {
        return castList(required(source, fieldName), fieldName);
    }

    private static String text(Map<String, Object> source, String fieldName) {
        return castText(required(source, fieldName), fieldName);
    }

    private static Object required(Map<String, Object> source, String fieldName) {
        var value = source.get(fieldName);
        if (value == null) {
            throw new EngineIngestionRequestException("Missing engine request field: " + fieldName);
        }
        return value;
    }

    private static Map<String, Object> castObject(Object value, String fieldName) {
        if (value instanceof Map<?, ?> map) {
            var converted = new LinkedHashMap<String, Object>();
            map.forEach((key, mapValue) -> converted.put(castText(key, fieldName + ".key"), mapValue));
            return converted;
        }
        throw new EngineIngestionRequestException("Engine request field must be an object: " + fieldName);
    }

    private static List<Object> castList(Object value, String fieldName) {
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new EngineIngestionRequestException("Engine request field must be an array: " + fieldName);
    }

    private static String castText(Object value, String fieldName) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new EngineIngestionRequestException("Engine request field must be non-blank text: " + fieldName);
    }
}
