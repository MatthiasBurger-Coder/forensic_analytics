package de.burger.forensics.analytics.services.javaastanalysis.application;

import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.JavaSourceScannerPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;

public final class JavaAstAnalysisApplicationService {
    private final JavaSourceScannerPort scanner;
    private final AstResultArtifactWriterPort artifactWriter;

    public JavaAstAnalysisApplicationService(JavaSourceScannerPort scanner, AstResultArtifactWriterPort artifactWriter) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.artifactWriter = Objects.requireNonNull(artifactWriter, "artifact writer must not be null");
    }

    public AnalyzeSourceSnapshotResult analyze(AnalyzeSourceSnapshotCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        verifyBounds(verifiedCommand);
        verifyChecksums(verifiedCommand);
        var scan = scanWithTimeout(verifiedCommand);
        var artifact = artifactWriter.write(
            verifiedCommand.metadata(),
            scan.sourceFacts(),
            scan.diagnostics(),
            scan.summary()
        );
        return new AnalyzeSourceSnapshotResult(
            verifiedCommand.metadata(),
            scan.completeness(),
            artifact,
            scan.summary(),
            scan.diagnostics()
        );
    }

    private JavaAstScanResult scanWithTimeout(AnalyzeSourceSnapshotCommand command) {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> scanner.scan(command));
            return future.get(command.scanPolicy().timeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException error) {
            throw new JavaAstAnalysisTimeoutException("Java AST analysis timed out after "
                + command.scanPolicy().timeoutSeconds() + " seconds.");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new JavaAstAnalysisTimeoutException("Java AST analysis was interrupted.");
        } catch (ExecutionException error) {
            if (error.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Java AST analysis failed.", error.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void verifyBounds(AnalyzeSourceSnapshotCommand command) {
        if (command.sourceFiles().size() > command.scanPolicy().maxFiles()) {
            throw new IllegalArgumentException("source file count exceeds scan policy");
        }
        var totalBytes = command.sourceFiles().stream()
            .mapToLong(JavaSourceFile::actualSizeBytes)
            .sum();
        if (totalBytes > command.scanPolicy().maxSourceBytes()) {
            throw new IllegalArgumentException("source byte count exceeds scan policy");
        }
    }

    private static void verifyChecksums(AnalyzeSourceSnapshotCommand command) {
        command.sourceFiles().stream()
            .filter(sourceFile -> !sourceFile.sha256().isBlank())
            .filter(sourceFile -> !sourceFile.sha256().equalsIgnoreCase(sha256(sourceFile.contentUtf8())))
            .findFirst()
            .ifPresent(sourceFile -> {
                throw new IllegalArgumentException("source file checksum mismatch for " + sourceFile.sourcePath());
            });
    }
}
