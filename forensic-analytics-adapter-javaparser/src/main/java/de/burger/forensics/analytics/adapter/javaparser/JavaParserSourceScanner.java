package de.burger.forensics.analytics.adapter.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.burger.forensics.analytics.application.analysis.port.SourceScannerPort;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class JavaParserSourceScanner implements SourceScannerPort {
    private final JavaSourceFileCollector sourceFileCollector;
    private final JavaParser javaParser;

    public JavaParserSourceScanner() {
        this(new JavaSourceFileCollector(), new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)));
    }

    JavaParserSourceScanner(JavaSourceFileCollector sourceFileCollector, JavaParser javaParser) {
        this.sourceFileCollector = Objects.requireNonNull(sourceFileCollector, "sourceFileCollector must not be null");
        this.javaParser = Objects.requireNonNull(javaParser, "javaParser must not be null");
    }

    @Override
    public List<SourceFact> scan(RepositorySource source) {
        Objects.requireNonNull(source, "source must not be null");
        return source.sourceRoots().stream()
            .map(Path::of)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .flatMap(sourceRoot -> sourceFileCollector.collect(sourceRoot).stream()
                .flatMap(sourceFile -> scanFile(sourceRoot, sourceFile).stream()))
            .sorted(Comparator.comparing(JavaParserSourceScanner::sortKey))
            .toList();
    }

    private List<SourceFact> scanFile(Path sourceRoot, Path sourceFile) {
        try {
            var result = javaParser.parse(sourceFile);
            if (!result.getProblems().isEmpty()) {
                return parseErrorFact(sourceRoot, sourceFile, result.getProblems());
            }
            return result.getResult()
                .map(compilationUnit -> sourceFacts(sourceRoot, sourceFile, compilationUnit))
                .orElseGet(() -> parseErrorFact(sourceRoot, sourceFile, result.getProblems()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read Java source file " + sourceFile + ".", exception);
        } catch (ParseProblemException exception) {
            return parseErrorFact(sourceRoot, sourceFile, exception.getProblems());
        }
    }

    private static List<SourceFact> sourceFacts(Path sourceRoot, Path sourceFile, CompilationUnit compilationUnit) {
        var packageName = compilationUnit.getPackageDeclaration()
            .map(NodeWithName::getNameAsString)
            .orElse("");
        var relativeSourcePath = relativeSourcePath(sourceRoot, sourceFile);
        return compilationUnit.findAll(MethodDeclaration.class).stream()
            .map(method -> methodFact(relativeSourcePath, packageName, method))
            .toList();
    }

    private static SourceFact methodFact(String sourcePath, String packageName, MethodDeclaration method) {
        var typeName = enclosingTypeName(method);
        var fullyQualifiedClassName = packageName.isBlank() ? typeName : packageName + "." + typeName;
        var methodName = method.getNameAsString();
        var methodSignature = fullyQualifiedClassName + "#" + method.getSignature().asString();
        var lineNumber = method.getBegin().map(position -> position.line).orElse(1);
        return new SourceFact(
            "java-method",
            new SourceLocation(sourcePath, fullyQualifiedClassName, methodName, lineNumber),
            methodSignature,
            "AST method " + methodSignature
        );
    }

    private static String enclosingTypeName(MethodDeclaration method) {
        var parts = new LinkedList<String>();
        Node current = method.getParentNode().orElse(null);
        while (current != null) {
            switch (current) {
                case ClassOrInterfaceDeclaration declaration -> parts.addFirst(declaration.getNameAsString());
                case EnumDeclaration declaration -> parts.addFirst(declaration.getNameAsString());
                case RecordDeclaration declaration -> parts.addFirst(declaration.getNameAsString());
                default -> {
                }
            }
            current = current.getParentNode().orElse(null);
        }
        if (parts.isEmpty()) {
            return "UNKNOWN";
        }
        return String.join("$", parts);
    }

    private static List<SourceFact> parseErrorFact(Path sourceRoot, Path sourceFile, List<Problem> problems) {
        var relativeSourcePath = relativeSourcePath(sourceRoot, sourceFile);
        var summary = problems.stream()
            .map(Problem::getVerboseMessage)
            .filter(message -> !message.isBlank())
            .findFirst()
            .orElse("JavaParser could not parse source file.");
        return List.of(new SourceFact(
            "java-parse-error",
            new SourceLocation(relativeSourcePath, "UNKNOWN", "UNKNOWN", 1),
            relativeSourcePath,
            summary
        ));
    }

    private static String relativeSourcePath(Path sourceRoot, Path sourceFile) {
        return sourceRoot.relativize(sourceFile.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String sortKey(SourceFact fact) {
        return fact.location().sourcePath() + "|"
            + "%08d".formatted(fact.location().lineNumber()) + "|"
            + fact.location().fullyQualifiedClassName() + "|"
            + fact.location().methodName() + "|"
            + fact.signature();
    }
}
