package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class RepositorySourceIdempotencyPayloads {
    private RepositorySourceIdempotencyPayloads() {
    }

    static String cleanup(CleanupRepositoryWorkspaceResult result) {
        return String.join(
            "\t",
            "cleanup",
            encode(result.workspaceId().value()),
            result.workspaceStatus().name(),
            diagnostics(result.diagnostics())
        );
    }

    static CleanupRepositoryWorkspaceResult cleanup(String payload) {
        var fields = fields(line(payload, 0), 4);
        return new CleanupRepositoryWorkspaceResult(
            new WorkspaceId(decode(fields[1])),
            RepositoryWorkspaceStatus.valueOf(fields[2]),
            diagnosticsFromPayload(fields[3])
        );
    }

    static String workspace(RepositoryWorkspace workspace) {
        var lines = new java.util.ArrayList<String>();
        lines.add(String.join(
            "\t",
            "workspace",
            encode(workspace.workspaceId().value()),
            encode(workspace.workspaceTitle().value()),
            encode(workspace.repository().repositoryKey().value()),
            encode(workspace.repository().repositoryUrl()),
            encode(workspace.repository().repositoryHost()),
            encode(workspace.repository().repositoryOwner()),
            encode(workspace.repository().repositoryName()),
            encode(workspace.repository().defaultBranch()),
            workspace.status().name(),
            workspace.createdAt().toString(),
            workspace.updatedAt().toString(),
            diagnostics(workspace.diagnostics()),
            attributes(workspace.safeAttributes())
        ));
        workspace.branches().stream()
            .map(RepositorySourceIdempotencyPayloads::branchLine)
            .forEach(lines::add);
        return String.join("\n", lines);
    }

    static RepositoryWorkspace workspace(String payload) {
        var lines = lines(payload);
        var fields = fields(lines.getFirst(), 14);
        var workspaceId = new WorkspaceId(decode(fields[1]));
        var branches = lines.stream()
            .skip(1)
            .map(RepositorySourceIdempotencyPayloads::branch)
            .toList();
        return new RepositoryWorkspace(
            workspaceId,
            new WorkspaceTitle(decode(fields[2])),
            new RepositoryIdentity(
                new RepositoryKey(decode(fields[3])),
                decode(fields[4]),
                decode(fields[5]),
                decode(fields[6]),
                decode(fields[7]),
                decode(fields[8])
            ),
            RepositoryWorkspaceStatus.valueOf(fields[9]),
            Instant.parse(fields[10]),
            Instant.parse(fields[11]),
            branches,
            diagnosticsFromPayload(fields[12]),
            attributesFromPayload(fields[13])
        );
    }

    static String branch(RepositoryWorkspaceBranch branch) {
        return branchLine(branch);
    }

    static RepositoryWorkspaceBranch branch(String payload) {
        var fields = fields(payload, 12);
        return new RepositoryWorkspaceBranch(
            new WorkspaceBranchId(decode(fields[1])),
            new WorkspaceId(decode(fields[2])),
            decode(fields[3]),
            decode(fields[4]),
            decode(fields[5]),
            decode(fields[6]).isBlank() ? null : new SourceSnapshotId(decode(fields[6])),
            RepositoryWorkspaceBranchStatus.valueOf(fields[7]),
            sourceRootsFromPayload(fields[8]),
            fields[9].isBlank() ? null : Instant.parse(fields[9]),
            Instant.parse(fields[10]),
            diagnosticsFromPayload(fields[11])
        );
    }

    static String refresh(RefreshRepositoryWorkspaceBranchResult result) {
        return String.join(
            "\n",
            String.join(
                "\t",
                "refresh",
                Boolean.toString(result.changed()),
                encode(result.previousCommit()),
                encode(result.previousSourceSnapshotId() == null ? "" : result.previousSourceSnapshotId().value()),
                diagnostics(result.diagnostics()),
                attributes(result.safeAttributes())
            ),
            branchLine(result.branch())
        );
    }

    static RefreshRepositoryWorkspaceBranchResult refresh(String payload) {
        var lines = lines(payload);
        if (lines.size() != 2) {
            throw new IllegalStateException("Idempotency refresh payload is invalid");
        }
        var refresh = fields(lines.getFirst(), 6);
        var previousSnapshot = decode(refresh[3]);
        return new RefreshRepositoryWorkspaceBranchResult(
            branch(lines.get(1)),
            Boolean.parseBoolean(refresh[1]),
            decode(refresh[2]),
            previousSnapshot.isBlank() ? null : new SourceSnapshotId(previousSnapshot),
            diagnosticsFromPayload(refresh[4]),
            attributesFromPayload(refresh[5])
        );
    }

    private static String branchLine(RepositoryWorkspaceBranch branch) {
        return String.join(
            "\t",
            "branch",
            encode(branch.workspaceBranchId().value()),
            encode(branch.workspaceId().value()),
            encode(branch.repositoryBranch()),
            encode(branch.requestedCommit()),
            encode(branch.resolvedCommit()),
            encode(branch.sourceSnapshotId() == null ? "" : branch.sourceSnapshotId().value()),
            branch.status().name(),
            sourceRoots(branch.sourceRoots()),
            branch.lastCheckedAt() == null ? "" : branch.lastCheckedAt().toString(),
            branch.lastUpdatedAt().toString(),
            diagnostics(branch.diagnostics())
        );
    }

    private static List<String> lines(String payload) {
        var lines = Arrays.stream(payload.split("\\n", -1))
            .filter(line -> !line.isBlank())
            .toList();
        if (lines.isEmpty()) {
            throw new IllegalStateException("Idempotency payload is empty");
        }
        return lines;
    }

    private static String line(String payload, int index) {
        var lines = lines(payload);
        if (index >= lines.size()) {
            throw new IllegalStateException("Idempotency payload is invalid");
        }
        return lines.get(index);
    }

    private static String[] fields(String line, int expectedFields) {
        var fields = line.split("\\t", -1);
        if (fields.length != expectedFields) {
            throw new IllegalStateException("Idempotency payload shape is invalid");
        }
        return fields;
    }

    private static String sourceRoots(List<SourceRoot> sourceRoots) {
        return sourceRoots.stream()
            .map(sourceRoot -> encode(sourceRoot.relativePath()) + ":" + encode(sourceRoot.language()))
            .collect(java.util.stream.Collectors.joining(","));
    }

    private static List<SourceRoot> sourceRootsFromPayload(String payload) {
        if (payload.isBlank()) {
            return List.of();
        }
        return Arrays.stream(payload.split(",", -1))
            .map(item -> item.split(":", -1))
            .map(parts -> new SourceRoot(decode(parts[0]), decode(parts[1])))
            .toList();
    }

    private static String diagnostics(List<Diagnostic> diagnostics) {
        return diagnostics.stream()
            .map(diagnostic -> encode(diagnostic.code()) + ":" + diagnostic.severity().name() + ":" + encode(diagnostic.message()))
            .collect(java.util.stream.Collectors.joining(","));
    }

    private static List<Diagnostic> diagnosticsFromPayload(String payload) {
        if (payload.isBlank()) {
            return List.of();
        }
        return Arrays.stream(payload.split(",", -1))
            .map(item -> item.split(":", -1))
            .map(parts -> new Diagnostic(decode(parts[0]), decode(parts[2]), DiagnosticSeverity.valueOf(parts[1])))
            .toList();
    }

    private static String attributes(Map<String, String> attributes) {
        return new TreeMap<>(attributes).entrySet().stream()
            .map(entry -> encode(entry.getKey()) + ":" + encode(entry.getValue()))
            .collect(java.util.stream.Collectors.joining(","));
    }

    private static Map<String, String> attributesFromPayload(String payload) {
        var attributes = new TreeMap<String, String>();
        if (!payload.isBlank()) {
            Arrays.stream(payload.split(",", -1))
                .map(item -> item.split(":", -1))
                .forEach(parts -> attributes.put(decode(parts[0]), decode(parts[1])));
        }
        return attributes;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
