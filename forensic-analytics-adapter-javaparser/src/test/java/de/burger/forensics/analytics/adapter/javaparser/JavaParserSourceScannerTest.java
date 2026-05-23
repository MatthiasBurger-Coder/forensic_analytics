package de.burger.forensics.analytics.adapter.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserSourceScannerTest {
    @TempDir
    Path tempDir;

    private final JavaParserSourceScanner scanner = new JavaParserSourceScanner();

    @Test
    void scansJavaAstMethodsAsSourceFacts() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        write(
            sourceRoot.resolve("com/example/Sample.java"),
            """
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
            """
        );

        var facts = scanner.scan(source(sourceRoot));

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
            facts.stream().map(JavaParserSourceScannerTest::projection).toList()
        );
    }

    @Test
    void logsScannerLifecycle() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        write(sourceRoot.resolve("com/example/Sample.java"), "package com.example; class Sample { void run() {} }");
        var logger = new RecordingOperationLogger();
        var loggingScanner = new JavaParserSourceScanner(
            new JavaSourceFileCollector(),
            new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)),
            logger
        );

        loggingScanner.scan(source(sourceRoot));

        assertEquals(
            List.of("started:adapter.javaparser.scan", "succeeded:adapter.javaparser.scan"),
            logger.events()
        );
    }

    @Test
    void skipsToolBuildAndDefaultTestSourceDirectoriesWhenScanningRepositoryRoot() throws Exception {
        write(tempDir.resolve("src/main/java/com/example/MainSource.java"), "package com.example; class MainSource { void run() {} }");
        write(tempDir.resolve("src/test/java/com/example/TestSource.java"), "package com.example; class TestSource { void test() {} }");
        write(tempDir.resolve("build/generated/com/example/Generated.java"), "package com.example; class Generated { void generated() {} }");
        write(tempDir.resolve(".git/hooks/Hook.java"), "class Hook { void hook() {} }");

        var facts = scanner.scan(source(tempDir));

        assertEquals(List.of("com.example.MainSource#run()"), facts.stream().map(fact -> fact.signature()).toList());
    }

    @Test
    void recordsParseErrorsAsExplicitFacts() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        write(sourceRoot.resolve("Broken.java"), "class Broken { void fail( }");

        var facts = scanner.scan(source(sourceRoot));

        assertEquals(1, facts.size());
        var fact = facts.getFirst();
        assertEquals("java-parse-error", fact.factType());
        assertEquals("Broken.java", fact.location().sourcePath());
        assertEquals("UNKNOWN", fact.location().fullyQualifiedClassName());
        assertEquals("UNKNOWN", fact.location().methodName());
        assertEquals("Broken.java", fact.signature());
        assertTrue(!fact.summary().isBlank());
    }

    @Test
    void scansDefaultPackageEnumAndRecordMethods() throws Exception {
        var sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        write(
            sourceRoot.resolve("LocalTypes.java"),
            """
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
            """
        );
        write(sourceRoot.resolve("notes.txt"), "not a Java source");

        var facts = scanner.scan(source(sourceRoot));

        assertEquals(
            List.of("LocalTypes$Mode#enabled()", "LocalTypes$Entry#label()"),
            facts.stream().map(fact -> fact.signature()).toList()
        );
        assertEquals(
            List.of("LocalTypes$Mode", "LocalTypes$Entry"),
            facts.stream().map(fact -> fact.location().fullyQualifiedClassName()).toList()
        );
    }

    @Test
    void rejectsMissingSourceRoot() {
        var source = new RepositorySource(
            new RepositoryMetadata("project", "missing", "main", "abcdef"),
            List.of(tempDir.resolve("missing").toString())
        );

        assertThrows(IllegalArgumentException.class, () -> scanner.scan(source));
    }

    @Test
    void requiresSource() {
        assertThrows(NullPointerException.class, () -> scanner.scan(null));
    }

    private static RepositorySource source(Path sourceRoot) {
        return new RepositorySource(
            new RepositoryMetadata("project", sourceRoot.toString(), "main", "abcdef"),
            List.of(sourceRoot.toString())
        );
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static FactProjection projection(de.burger.forensics.analytics.domain.source.SourceFact fact) {
        return new FactProjection(
            fact.factType(),
            fact.location().sourcePath(),
            fact.location().fullyQualifiedClassName(),
            fact.location().methodName(),
            fact.location().lineNumber(),
            fact.signature(),
            fact.summary()
        );
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

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<String> events = new ArrayList<>();

        @Override
        public void started(String operation) {
            events.add("started:" + operation);
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add("succeeded:" + operation);
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add("failed:" + operation);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
