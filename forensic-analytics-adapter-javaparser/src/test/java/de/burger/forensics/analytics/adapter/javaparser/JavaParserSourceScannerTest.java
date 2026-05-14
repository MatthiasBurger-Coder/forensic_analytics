package de.burger.forensics.analytics.adapter.javaparser;

import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
                "com.example.Sample#greet(String)",
                "com.example.Sample#run()",
                "com.example.Sample$Nested#size()"
            ),
            facts.stream().map(fact -> fact.signature()).toList()
        );
        assertEquals(
            List.of("java-method", "java-method", "java-method"),
            facts.stream().map(fact -> fact.factType()).toList()
        );
        assertEquals("com/example/Sample.java", facts.getFirst().location().sourcePath());
        assertEquals("com.example.Sample", facts.getFirst().location().fullyQualifiedClassName());
        assertEquals("greet", facts.getFirst().location().methodName());
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
}
