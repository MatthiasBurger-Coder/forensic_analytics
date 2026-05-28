package de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser;

import com.github.javaparser.Problem;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanPolicy;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserSourceScannerAdapterTest {
    private final JavaParserSourceScannerAdapter scanner = new JavaParserSourceScannerAdapter();

    @Test
    void scansJavaAstMethodsAsStaticSourceFactsWithDeterministicLocations() {
        var source = """
            package com.example;

            final class Sample {
                String greet(String name) {
                    return "hello " + name;
                }

                void run() {
                }

                static final class Nested {
                    int size() {
                        return 1;
                    }
                }
            }
            """;

        var result = scanner.scan(command(List.of(file("src/main/java", "com/example/Sample.java", source)), false));

        assertEquals(
            List.of(
                "com.example.Sample#greet(String)",
                "com.example.Sample#run()",
                "com.example.Sample$Nested#size()"
            ),
            result.sourceFacts().stream().map(fact -> fact.signature()).toList()
        );
        assertEquals("java-method", result.sourceFacts().getFirst().factType());
        assertEquals("src/main/java/com/example/Sample.java", result.sourceFacts().getFirst().location().sourcePath());
        assertEquals("com.example.Sample", result.sourceFacts().getFirst().location().fullyQualifiedClassName());
        assertEquals("greet", result.sourceFacts().getFirst().location().methodName());
        assertTrue(result.sourceFacts().stream().allMatch(fact -> fact.factId().startsWith("java-source-fact:")));
        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
    }

    @Test
    void scansDefaultPackageEnumAndRecordMethods() {
        var source = """
            final class LocalTypes {
                enum Mode {
                    ACTIVE;

                    boolean enabled() {
                        return true;
                    }
                }

                record Entry(String name) {
                    String label() {
                        return name;
                    }
                }
            }
            """;

        var result = scanner.scan(command(List.of(file("src/main/java", "LocalTypes.java", source)), false));

        assertEquals(
            List.of("LocalTypes$Mode#enabled()", "LocalTypes$Entry#label()"),
            result.sourceFacts().stream().map(fact -> fact.signature()).toList()
        );
    }

    @Test
    void recordsParseErrorsAndSymbolResolutionLimitationsAsDiagnostics() {
        var source = """
            class Broken {
                void fail( }
            }
            """;

        var result = scanner.scan(command(List.of(file("src/main/java", "Broken.java", source)), true));

        assertEquals(0, result.sourceFacts().size());
        assertEquals(1, result.summary().parseErrorCount());
        assertEquals(2, result.diagnostics().size());
        assertEquals(List.of("JAVA_PARSE_ERROR", "SYMBOL_RESOLUTION_NOT_CONFIGURED"), result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
        assertEquals("src/main/java/Broken.java", result.diagnostics().getFirst().sourcePath());
        assertEquals(2, result.diagnostics().getFirst().lineNumber());
        assertTrue(result.diagnostics().getFirst().columnNumber() > 0);
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
    }

    @Test
    void usesUnknownDiagnosticPositionWhenParserProblemHasNoLocation() {
        var position = JavaParserSourceScannerAdapter.problemPosition(List.of(new Problem("parser failure", null, null)));

        assertEquals(new JavaParserSourceScannerAdapter.ProblemPosition(0, 0), position);
    }

    @Test
    void skipsToolBuildTestAndNonJavaInputs() {
        var result = scanner.scan(command(List.of(
            file("src/main/java", "com/example/MainSource.java", "package com.example; class MainSource { void run() {} }"),
            file("src/test/java", "com/example/TestSource.java", "package com.example; class TestSource { void test() {} }"),
            file("src/main/java", "build/generated/Generated.java", "class Generated { void generated() {} }"),
            file("src/main/java", "notes.txt", "not Java")
        ), false));

        assertEquals(List.of("com.example.MainSource#run()"), result.sourceFacts().stream().map(fact -> fact.signature()).toList());
        assertEquals(3, result.summary().skippedFileCount());
    }

    private static AnalyzeSourceSnapshotCommand command(List<JavaSourceFile> sourceFiles, boolean symbolDiagnostics) {
        return new AnalyzeSourceSnapshotCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "java-ast-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "java-ast-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new ScanPolicy(100, 100_000, 60, symbolDiagnostics),
            List.of(new SourceRoot("src/main/java", "java"), new SourceRoot("src/test/java", "java")),
            sourceFiles
        );
    }

    private static JavaSourceFile file(String root, String path, String content) {
        return new JavaSourceFile(root, path, content, sha256(content), content.getBytes(StandardCharsets.UTF_8).length);
    }
}
