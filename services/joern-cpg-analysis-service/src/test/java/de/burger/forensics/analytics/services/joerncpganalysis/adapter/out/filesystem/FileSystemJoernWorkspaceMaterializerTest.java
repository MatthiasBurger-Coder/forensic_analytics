package de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernMaterializationPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializationMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializedPackageDescriptor;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemJoernWorkspaceMaterializerTest {
    @TempDir
    Path tempDir;

    @Test
    void materializesValidatedPackagesAndPreservesArtifactByteAccess() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var sourcePackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/source.zip",
            "source-snapshot/snapshot-1/source-manifest.json",
            "repository-analysis-service",
            "repository-analysis.v1.SourcePackage",
            List.of(file("src/main/java/App.java", "class App {}\n"))
        );
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output.zip",
            "source-snapshot/snapshot-1/build-output-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );

        var workspace = new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
            .materialize(command(sourcePackage, buildPackage, 10_000));

        assertEquals("joern-workspace-snapshot-1", workspace.workspaceId());
        assertTrue(Files.isRegularFile(workspaceRoot.resolve("joern-workspace-snapshot-1/src/main/java/App.java")));
        assertTrue(Files.isRegularFile(workspaceRoot.resolve("joern-workspace-snapshot-1/build/classes/App.class")));
        assertEquals(
            List.of("build-artifact-worker-service", "repository-analysis-service"),
            workspace.inputArtifacts().stream().map(artifact -> artifact.byteAccess().ownerService()).toList()
        );
        assertEquals(
            List.of("build-artifact-worker.v1.BuildOutputPackage", "repository-analysis.v1.SourcePackage"),
            workspace.inputArtifacts().stream().map(artifact -> artifact.byteAccess().retrievalContract()).toList()
        );
        assertEquals(
            List.of("source-snapshot/snapshot-1/build-output.zip", "source-snapshot/snapshot-1/source.zip"),
            workspace.inputArtifacts().stream().map(artifact -> artifact.byteAccess().retrievalReference()).toList()
        );
        assertEquals(
            List.of(ArtifactByteCustody.PRODUCER_RETAINED, ArtifactByteCustody.PRODUCER_RETAINED),
            workspace.inputArtifacts().stream().map(artifact -> artifact.byteAccess().byteCustody()).toList()
        );
    }

    @Test
    void materializesDeclaredDirectoryEntries() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var sourcePackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/source-directories.zip",
            "source-snapshot/snapshot-1/source-directories-manifest.json",
            "repository-analysis-service",
            "repository-analysis.v1.SourcePackage",
            List.of(directory("src/main/java"), file("src/main/java/App.java", "class App {}\n"))
        );
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output-directories.zip",
            "source-snapshot/snapshot-1/build-output-directories-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(directory("build/classes"), file("build/classes/App.class", "compiled bytes"))
        );

        new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
            .materialize(command(sourcePackage, buildPackage, 10_000));

        assertTrue(Files.isDirectory(workspaceRoot.resolve("joern-workspace-snapshot-1/src/main/java")));
        assertTrue(Files.isDirectory(workspaceRoot.resolve("joern-workspace-snapshot-1/build/classes")));
    }

    @Test
    void rejectsManifestChecksumMismatchAsArtifactPreconditionFailure() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var sourcePackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/source.zip",
            "source-snapshot/snapshot-1/source-manifest.json",
            "repository-analysis-service",
            "repository-analysis.v1.SourcePackage",
            List.of(file("src/main/java/App.java", "class App {}\n"))
        );
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output.zip",
            "source-snapshot/snapshot-1/build-output-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );
        var tamperedSourcePackage = new MaterializedPackageDescriptor(
            "source package",
            PackageAvailability.AVAILABLE,
            new ArtifactReference(
                sourcePackage.manifestArtifact().path(),
                sourcePackage.manifestArtifact().type(),
                "b".repeat(64),
                sourcePackage.manifestArtifact().sizeBytes()
            ),
            sourcePackage.packageArtifact(),
            sourcePackage.producerService(),
            sourcePackage.schemaVersion(),
            sourcePackage.completeness(),
            sourcePackage.byteAccess()
        );

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(tamperedSourcePackage, buildPackage, 10_000))
        );
        assertFalse(Files.exists(workspaceRoot.resolve("joern-workspace-snapshot-1")));
    }

    @Test
    void rejectsUnsafeManifestEntriesAndWorkspaceQuotaBeforeExtractionCompletes() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output.zip",
            "source-snapshot/snapshot-1/build-output-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-device.zip",
                        "source-snapshot/snapshot-1/source-device-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java/App.java","kind":"device","sizeBytes":0}]}
                            """,
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-dot-duplicate.zip",
                        "source-snapshot/snapshot-1/source-dot-duplicate-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[
                              {"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"},
                              {"path":"src/main/java/./App.java","kind":"file","sizeBytes":13,"sha256":"%s"}
                            ]}
                            """.formatted(sha256("class App {}\n"), sha256("class App {}\n")),
                        List.of(file("src/main/java/App.java", "class App {}\n"), file("src/main/java/./App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-duplicate.zip",
                        "source-snapshot/snapshot-1/source-duplicate-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[
                              {"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"},
                              {"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}
                            ]}
                            """.formatted(sha256("class App {}\n"), sha256("class App {}\n")),
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackage(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-large.zip",
                        "source-snapshot/snapshot-1/source-large-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10
                ))
        );
        assertFalse(Files.exists(workspaceRoot.resolve("joern-workspace-snapshot-1")));
    }

    @Test
    void rejectsArchiveTraversalUnexpectedEntriesAndEntryChecksumMismatch() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output.zip",
            "source-snapshot/snapshot-1/build-output-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-traversal.zip",
                        "source-snapshot/snapshot-1/source-traversal-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}]}
                            """.formatted(sha256("class App {}\n")),
                        List.of(file("../evil.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-extra.zip",
                        "source-snapshot/snapshot-1/source-extra-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}]}
                            """.formatted(sha256("class App {}\n")),
                        List.of(
                            file("src/main/java/App.java", "class App {}\n"),
                            file("src/main/java/Extra.java", "class Extra {}\n")
                        )
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-checksum.zip",
                        "source-snapshot/snapshot-1/source-checksum-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}]}
                            """.formatted(sha256("different\n")),
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertFalse(Files.exists(workspaceRoot.resolve("joern-workspace-snapshot-1")));
    }

    @Test
    void rejectsMissingPackageBytesAndArtifactSizeMismatches() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var sourcePackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/source-missing.zip",
            "source-snapshot/snapshot-1/source-missing-manifest.json",
            "repository-analysis-service",
            "repository-analysis.v1.SourcePackage",
            List.of(file("src/main/java/App.java", "class App {}\n"))
        );
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output-missing.zip",
            "source-snapshot/snapshot-1/build-output-missing-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );
        Files.delete(cacheRoot.resolve(sourcePackage.byteAccess().retrievalReference()));

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(sourcePackage, buildPackage, 10_000))
        );

        var sizeMismatch = new MaterializedPackageDescriptor(
            "build-output package",
            PackageAvailability.AVAILABLE,
            buildPackage.manifestArtifact(),
            new ArtifactReference(
                buildPackage.packageArtifact().path(),
                buildPackage.packageArtifact().type(),
                buildPackage.packageArtifact().sha256(),
                buildPackage.packageArtifact().sizeBytes() + 1
            ),
            buildPackage.producerService(),
            buildPackage.schemaVersion(),
            buildPackage.completeness(),
            buildPackage.byteAccess()
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackage(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-size.zip",
                        "source-snapshot/snapshot-1/source-size-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    sizeMismatch,
                    10_000
                ))
        );
    }

    @Test
    void rejectsPackageCacheParentSymlinksBeforeReadingBytes() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var outsideCacheRoot = tempDir.resolve("outside-cache");
        Files.createDirectories(cacheRoot);
        Files.createDirectories(outsideCacheRoot);
        var sourcePackage = writePackage(
            outsideCacheRoot,
            "source-snapshot/snapshot-1/source.zip",
            "source-snapshot/snapshot-1/source-manifest.json",
            "repository-analysis-service",
            "repository-analysis.v1.SourcePackage",
            List.of(file("src/main/java/App.java", "class App {}\n"))
        );
        var buildPackage = writePackage(
            cacheRoot,
            "build-cache/snapshot-1/build-output.zip",
            "build-cache/snapshot-1/build-output-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );
        try {
            Files.createSymbolicLink(cacheRoot.resolve("source-snapshot"), outsideCacheRoot.resolve("source-snapshot"));
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            return;
        }

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(sourcePackage, buildPackage, 10_000))
        );
        assertFalse(Files.exists(workspaceRoot.resolve("joern-workspace-snapshot-1")));
    }

    @Test
    void rejectsMalformedManifestsBeforeExtraction() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output-valid.zip",
            "source-snapshot/snapshot-1/build-output-valid-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );

        assertRejectsSourceManifest(workspaceRoot, cacheRoot, buildPackage, "empty", "{\"entries\":[]}");
        assertRejectsSourceManifest(workspaceRoot, cacheRoot, buildPackage, "missing-entries", "{\"files\":[]}");
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "deep",
            """
                {"entries":[{"path":"a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "negative-size",
            """
                {"entries":[{"path":"src/main/java/App.java","kind":"file","sizeBytes":-1,"sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "directory-size",
            """
                {"entries":[{"path":"src/main/java","kind":"directory","sizeBytes":1}]}
                """
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "invalid-kind",
            """
                {"entries":[{"path":"src/main/java/App.java","kind":"portal","sizeBytes":13,"sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "missing-path",
            """
                {"entries":[{"kind":"file","sizeBytes":13,"sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "missing-size",
            """
                {"entries":[{"path":"src/main/java/App.java","kind":"file","sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
        assertRejectsSourceManifest(
            workspaceRoot,
            cacheRoot,
            buildPackage,
            "uri-path",
            """
                {"entries":[{"path":"https://example.test/App.java","kind":"file","sizeBytes":13,"sha256":"%s"}]}
                """.formatted(sha256("class App {}\n"))
        );
    }

    @Test
    void rejectsArchiveKindMismatchesMissingManifestEntriesAndDeclaredByteOverflow() throws Exception {
        var workspaceRoot = tempDir.resolve("workspaces");
        var cacheRoot = tempDir.resolve("package-cache");
        var buildPackage = writePackage(
            cacheRoot,
            "source-snapshot/snapshot-1/build-output-kind.zip",
            "source-snapshot/snapshot-1/build-output-kind-manifest.json",
            "build-artifact-worker-service",
            "build-artifact-worker.v1.BuildOutputPackage",
            List.of(file("build/classes/App.class", "compiled bytes"))
        );

        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-missing-entry.zip",
                        "source-snapshot/snapshot-1/source-missing-entry-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[
                              {"path":"src/main/java/App.java","kind":"file","sizeBytes":13,"sha256":"%s"},
                              {"path":"src/main/java/Missing.java","kind":"file","sizeBytes":17,"sha256":"%s"}
                            ]}
                            """.formatted(sha256("class App {}\n"), sha256("class Missing {}\n")),
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-directory-expected.zip",
                        "source-snapshot/snapshot-1/source-directory-expected-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java","kind":"directory","sizeBytes":0}]}
                            """,
                        List.of(file("src/main/java", "not a directory"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-file-expected.zip",
                        "source-snapshot/snapshot-1/source-file-expected-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java","kind":"file","sizeBytes":0,"sha256":"%s"}]}
                            """.formatted(sha256(new byte[0])),
                        List.of(directory("src/main/java"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-byte-overflow.zip",
                        "source-snapshot/snapshot-1/source-byte-overflow-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        """
                            {"entries":[{"path":"src/main/java/App.java","kind":"file","sizeBytes":1,"sha256":"%s"}]}
                            """.formatted(sha256("class App {}\n")),
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
    }

    private static MaterializeJoernWorkspaceCommand command(
        MaterializedPackageDescriptor sourcePackage,
        MaterializedPackageDescriptor buildPackage,
        long maxWorkspaceBytes
    ) {
        return new MaterializeJoernWorkspaceCommand(
            new MaterializationMetadata(
                "request-1",
                "idempotency-1",
                "joern-materialization-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                Map.of("tenant", "demo")
            ),
            List.of(new SourceRoot("src/main/java", "java")),
            sourcePackage,
            buildPackage,
            new JoernMaterializationPolicy(2, maxWorkspaceBytes, 1_000_000, 20, true, true, true, true)
        );
    }

    private static void assertRejectsSourceManifest(
        Path workspaceRoot,
        Path cacheRoot,
        MaterializedPackageDescriptor buildPackage,
        String name,
        String manifest
    ) throws Exception {
        assertThrows(
            JoernCpgArtifactException.class,
            () -> new FileSystemJoernWorkspaceMaterializer(workspaceRoot, cacheRoot)
                .materialize(command(
                    writePackageWithManifest(
                        cacheRoot,
                        "source-snapshot/snapshot-1/source-" + name + ".zip",
                        "source-snapshot/snapshot-1/source-" + name + "-manifest.json",
                        "repository-analysis-service",
                        "repository-analysis.v1.SourcePackage",
                        manifest,
                        List.of(file("src/main/java/App.java", "class App {}\n"))
                    ),
                    buildPackage,
                    10_000
                ))
        );
    }

    private static MaterializedPackageDescriptor writePackage(
        Path cacheRoot,
        String packageReference,
        String manifestReference,
        String ownerService,
        String retrievalContract,
        List<PackageEntry> entries
    ) throws Exception {
        var manifest = new StringBuilder("{\"entries\":[");
        for (var index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            if (index > 0) {
                manifest.append(',');
            }
            manifest.append("""
                %s\
                """.formatted(manifestEntry(entry)));
        }
        manifest.append("]}");
        return writePackageWithManifest(cacheRoot, packageReference, manifestReference, ownerService, retrievalContract, manifest.toString(), entries);
    }

    private static MaterializedPackageDescriptor writePackageWithManifest(
        Path cacheRoot,
        String packageReference,
        String manifestReference,
        String ownerService,
        String retrievalContract,
        String manifest,
        List<PackageEntry> entries
    ) throws Exception {
        var packageBytes = zip(entries);
        var manifestBytes = manifest.getBytes(StandardCharsets.UTF_8);
        var packagePath = cacheRoot.resolve(packageReference);
        var manifestPath = cacheRoot.resolve(manifestReference);
        Files.createDirectories(packagePath.getParent());
        Files.createDirectories(manifestPath.getParent());
        Files.write(packagePath, packageBytes);
        Files.write(manifestPath, manifestBytes);

        return new MaterializedPackageDescriptor(
            packageReference.contains("build-output") ? "build-output package" : "source package",
            PackageAvailability.AVAILABLE,
            new ArtifactReference(manifestReference, "application/json", sha256(manifestBytes), manifestBytes.length),
            new ArtifactReference(packageReference, "application/zip", sha256(packageBytes), packageBytes.length),
            ownerService,
            packageReference.contains("build-output") ? "build-output-package-descriptor-v1" : "source-package-descriptor-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(ownerService, retrievalContract, packageReference, ArtifactByteCustody.PRODUCER_RETAINED)
        );
    }

    private static PackageEntry file(String path, String content) {
        return new PackageEntry(path, content.getBytes(StandardCharsets.UTF_8), false);
    }

    private static PackageEntry directory(String path) {
        return new PackageEntry(path, new byte[0], true);
    }

    private static String manifestEntry(PackageEntry entry) {
        if (entry.directory()) {
            return "{\"path\":\"%s\",\"kind\":\"directory\",\"sizeBytes\":0}".formatted(entry.path());
        }
        return "{\"path\":\"%s\",\"kind\":\"file\",\"sizeBytes\":%d,\"sha256\":\"%s\"}"
            .formatted(entry.path(), entry.bytes().length, sha256(entry.bytes()));
    }

    private static byte[] zip(List<PackageEntry> entries) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            for (var entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.directory() ? entry.path() + "/" : entry.path()));
                if (!entry.directory()) {
                    zip.write(entry.bytes());
                }
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private record PackageEntry(String path, byte[] bytes, boolean directory) {
    }
}
