package de.burger.forensics.analytics.services.javaparseranalysis.adapter.out.javaparser;

import com.github.javaparser.Problem;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.EvidenceKind;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ScanPolicy;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.sha256;
import static de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.stableId;
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
                new FactProjection(
                    "java-method",
                    "com/example/Sample.java",
                    "com.example.Sample",
                    "greet",
                    4,
                    "com.example.Sample#greet(String)",
                    "AST method com.example.Sample#greet(String)"
                ),
                new FactProjection(
                    "java-method",
                    "com/example/Sample.java",
                    "com.example.Sample",
                    "run",
                    8,
                    "com.example.Sample#run()",
                    "AST method com.example.Sample#run()"
                ),
                new FactProjection(
                    "java-method",
                    "com/example/Sample.java",
                    "com.example.Sample$Nested",
                    "size",
                    12,
                    "com.example.Sample$Nested#size()",
                    "AST method com.example.Sample$Nested#size()"
                )
            ),
            result.sourceFacts().stream().map(JavaParserSourceScannerAdapterTest::legacyProjection).toList()
        );
        assertEquals(List.of("src/main/java", "src/main/java", "src/main/java"), result.sourceFacts().stream().map(JavaSourceFact::sourceRoot).toList());
        assertEquals(List.of(EvidenceKind.STATIC_SOURCE_FACT, EvidenceKind.STATIC_SOURCE_FACT, EvidenceKind.STATIC_SOURCE_FACT), result.sourceFacts().stream()
            .map(JavaSourceFact::evidenceKind)
            .toList());
        assertEquals(
            List.of(
                stableId("snapshot-1", "java-method", "src/main/java/com/example/Sample.java", "com.example.Sample#greet(String)", "4"),
                stableId("snapshot-1", "java-method", "src/main/java/com/example/Sample.java", "com.example.Sample#run()", "8"),
                stableId("snapshot-1", "java-method", "src/main/java/com/example/Sample.java", "com.example.Sample$Nested#size()", "12")
            ),
            result.sourceFacts().stream().map(JavaSourceFact::factId).toList()
        );
        assertEquals("java-method", result.sourceFacts().getFirst().factType());
        assertEquals("src/main/java", result.sourceFacts().getFirst().sourceRoot());
        assertEquals("src/main/java/com/example/Sample.java", result.sourceFacts().getFirst().location().sourcePath());
        assertEquals("com.example.Sample", result.sourceFacts().getFirst().location().fullyQualifiedClassName());
        assertEquals("greet", result.sourceFacts().getFirst().location().methodName());
        assertTrue(result.sourceFacts().stream().allMatch(fact -> fact.factId().startsWith("java-source-fact:")));
        assertEquals(List.of("SYMBOL_RESOLUTION_NOT_CONFIGURED"), result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
    }

    @Test
    void ordersFactsDeterministicallyAcrossInputOrder() {
        var alpha = file("src/main/java", "a/A.java", "package a; class A { void alpha() {} }");
        var beta = file("src/main/java", "b/B.java", "package b; class B { void beta() {} }");

        var ordered = scanner.scan(command(List.of(alpha, beta), false));
        var reordered = scanner.scan(command(List.of(beta, alpha), false));

        assertEquals(
            List.of("a.A#alpha()", "b.B#beta()"),
            ordered.sourceFacts().stream().map(JavaSourceFact::signature).toList()
        );
        assertEquals(
            ordered.sourceFacts().stream().map(JavaSourceFact::factId).toList(),
            reordered.sourceFacts().stream().map(JavaSourceFact::factId).toList()
        );
        assertEquals(
            ordered.sourceFacts().stream().map(JavaParserSourceScannerAdapterTest::legacyProjection).toList(),
            reordered.sourceFacts().stream().map(JavaParserSourceScannerAdapterTest::legacyProjection).toList()
        );
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

        var result = scanner.scan(command(List.of(file("src/main/java", "Broken.java", source)), false));

        assertEquals(0, result.sourceFacts().size());
        assertTrue(result.sourceFacts().stream().noneMatch(fact -> "java-parse-error".equals(fact.factType())));
        assertEquals(1, result.summary().parseErrorCount());
        assertEquals(2, result.diagnostics().size());
        assertEquals(
            List.of("JAVA_PARSE_ERROR", "SYMBOL_RESOLUTION_NOT_CONFIGURED"),
            result.diagnostics().stream().map(diagnostic -> diagnostic.code()).sorted().toList()
        );
        var parseError = result.diagnostics().stream()
            .filter(diagnostic -> "JAVA_PARSE_ERROR".equals(diagnostic.code()))
            .findFirst()
            .orElseThrow();
        assertEquals("src/main/java/Broken.java", parseError.sourcePath());
        assertEquals(2, parseError.lineNumber());
        assertTrue(parseError.columnNumber() > 0);
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
    }

    @Test
    void keepsUnresolvedSymbolsAsCompletenessAffectingDiagnosticsWithoutInventingRelationships() {
        var source = """
            package com.example;

            final class UsesMissingType {
                MissingType run(MissingType input) {
                    return input;
                }
            }
            """;

        var result = scanner.scan(command(List.of(file("src/main/java", "com/example/UsesMissingType.java", source)), false));

        assertEquals(List.of("com.example.UsesMissingType#run(MissingType)"), result.sourceFacts().stream().map(fact -> fact.signature()).toList());
        assertEquals(List.of("java-method"), result.sourceFacts().stream().map(fact -> fact.factType()).toList());
        assertEquals(List.of("SYMBOL_RESOLUTION_NOT_CONFIGURED"), result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
        var diagnostic = result.diagnostics().getFirst();
        assertEquals(DiagnosticSeverity.WARNING, diagnostic.severity());
        assertEquals("", diagnostic.sourcePath());
        assertEquals(0, diagnostic.lineNumber());
        assertEquals(0, diagnostic.columnNumber());
        assertTrue(diagnostic.affectsCompleteness());
        assertEquals(List.of(EvidenceKind.STATIC_SOURCE_FACT), result.sourceFacts().stream().map(JavaSourceFact::evidenceKind).toList());
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
                "java-parser-analysis-service-test",
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

    private static FactProjection legacyProjection(JavaSourceFact fact) {
        return new FactProjection(
            fact.factType(),
            sourceRootRelativePath(fact),
            fact.location().fullyQualifiedClassName(),
            fact.location().methodName(),
            fact.location().lineNumber(),
            fact.signature(),
            fact.summary()
        );
    }

    private static String sourceRootRelativePath(JavaSourceFact fact) {
        return fact.location().sourcePath().substring(fact.sourceRoot().length() + 1);
    }

    private record FactProjection(
        String factType,
        String sourcePath,
        String fullyQualifiedClassName,
        String methodName,
        int lineNumber,
        String signature,
        String summary
    ) {
    }
}
