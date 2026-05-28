package de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.Providers;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.JavaSourceScannerPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.EvidenceKind;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceLocation;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.stableId;

public final class JavaParserSourceScannerAdapter implements JavaSourceScannerPort {
    private static final String PARSER_NAME = "JavaParser";
    private static final String PARSER_VERSION = "3.27.1";
    private static final Set<String> SKIPPED_DIRECTORY_NAMES = Set.of(".git", ".gradle", ".idea", "build", "target", "out");
    private static final List<List<String>> DEFAULT_EXCLUDED_SOURCE_ROOT_SEGMENTS = List.of(
        List.of("src", "test", "java"),
        List.of("src", "integrationtest", "java")
    );

    private final JavaParser javaParser;

    public JavaParserSourceScannerAdapter() {
        this(new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)));
    }

    JavaParserSourceScannerAdapter(JavaParser javaParser) {
        this.javaParser = Objects.requireNonNull(javaParser, "java parser must not be null");
    }

    @Override
    public JavaAstScanResult scan(AnalyzeSourceSnapshotCommand command) {
        var roots = command.sourceRoots().stream()
            .collect(Collectors.toUnmodifiableMap(SourceRoot::relativePath, Function.identity()));
        var facts = new ArrayList<JavaSourceFact>();
        var diagnostics = new ArrayList<JavaAstDiagnostic>();
        var counters = new ScanCounters(command.sourceFiles().size());

        command.sourceFiles().forEach(sourceFile -> scanFile(command, roots, sourceFile, facts, diagnostics, counters));
        if (command.scanPolicy().emitSymbolResolutionDiagnostics()) {
            diagnostics.add(JavaAstDiagnostic.warning(
                command.metadata().sourceSnapshotId(),
                "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                "JavaParser symbol solving is not configured in this provisional worker; unresolved symbols are reported as an analysis limitation.",
                "",
                0,
                0,
                true
            ));
        }

        var sortedFacts = facts.stream().sorted(Comparator.comparing(JavaParserSourceScannerAdapter::sortKey)).toList();
        var sortedDiagnostics = diagnostics.stream().sorted(Comparator.comparing(JavaParserSourceScannerAdapter::sortKey)).toList();
        return new JavaAstScanResult(
            sortedFacts,
            sortedDiagnostics,
            new ScanSummary(
                counters.receivedFileCount(),
                counters.parsedFileCount(),
                counters.skippedFileCount(),
                counters.parseErrorCount(),
                sortedFacts.size(),
                PARSER_NAME,
                PARSER_VERSION
            )
        );
    }

    private void scanFile(
        AnalyzeSourceSnapshotCommand command,
        Map<String, SourceRoot> roots,
        JavaSourceFile sourceFile,
        List<JavaSourceFact> facts,
        List<JavaAstDiagnostic> diagnostics,
        ScanCounters counters
    ) {
        var sourceRoot = roots.get(sourceFile.sourceRoot());
        if (sourceRoot == null) {
            counters.skipped();
            diagnostics.add(JavaAstDiagnostic.warning(
                command.metadata().sourceSnapshotId(),
                "UNKNOWN_SOURCE_ROOT",
                "Source file references a source root that is not declared.",
                sourceFile.sourcePath(),
                0,
                0,
                true
            ));
            return;
        }
        if (!"java".equals(sourceRoot.language())) {
            counters.skipped();
            diagnostics.add(JavaAstDiagnostic.warning(
                command.metadata().sourceSnapshotId(),
                "UNSUPPORTED_SOURCE_ROOT",
                "Only Java source roots are supported by the Java AST analysis worker.",
                sourceFile.sourcePath(),
                0,
                0,
                true
            ));
            return;
        }
        if (shouldSkip(sourceFile)) {
            counters.skipped();
            return;
        }
        parseSource(command.metadata().sourceSnapshotId(), sourceFile, facts, diagnostics, counters);
    }

    private void parseSource(
        SourceSnapshotId sourceSnapshotId,
        JavaSourceFile sourceFile,
        List<JavaSourceFact> facts,
        List<JavaAstDiagnostic> diagnostics,
        ScanCounters counters
    ) {
        try {
            var result = javaParser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(sourceFile.contentUtf8()));
            if (!result.getProblems().isEmpty()) {
                parseError(sourceSnapshotId, sourceFile, result.getProblems(), diagnostics, counters);
                return;
            }
            result.getResult()
                .ifPresentOrElse(
                    compilationUnit -> {
                        counters.parsed();
                        facts.addAll(sourceFacts(sourceSnapshotId, sourceFile, compilationUnit));
                    },
                    () -> parseError(sourceSnapshotId, sourceFile, result.getProblems(), diagnostics, counters)
                );
        } catch (ParseProblemException error) {
            parseError(sourceSnapshotId, sourceFile, error.getProblems(), diagnostics, counters);
        }
    }

    private static List<JavaSourceFact> sourceFacts(
        SourceSnapshotId sourceSnapshotId,
        JavaSourceFile sourceFile,
        CompilationUnit compilationUnit
    ) {
        var packageName = compilationUnit.getPackageDeclaration()
            .map(NodeWithName::getNameAsString)
            .orElse("");
        return compilationUnit.findAll(MethodDeclaration.class).stream()
            .map(method -> methodFact(sourceSnapshotId, sourceFile.sourcePath(), packageName, method))
            .toList();
    }

    private static JavaSourceFact methodFact(
        SourceSnapshotId sourceSnapshotId,
        String sourcePath,
        String packageName,
        MethodDeclaration method
    ) {
        var typeName = enclosingTypeName(method);
        var fullyQualifiedClassName = packageName.isBlank() ? typeName : packageName + "." + typeName;
        var methodName = method.getNameAsString();
        var methodSignature = fullyQualifiedClassName + "#" + method.getSignature().asString();
        var position = method.getBegin().orElseThrow();
        var factId = stableId(sourceSnapshotId.value(), "java-method", sourcePath, methodSignature, Integer.toString(position.line));
        return new JavaSourceFact(
            factId,
            "java-method",
            new SourceLocation(sourcePath, fullyQualifiedClassName, methodName, position.line, position.column),
            methodSignature,
            "AST method " + methodSignature,
            EvidenceKind.STATIC_SOURCE_FACT
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
        return parts.isEmpty() ? "UNKNOWN" : String.join("$", parts);
    }

    private static void parseError(
        SourceSnapshotId sourceSnapshotId,
        JavaSourceFile sourceFile,
        List<Problem> problems,
        List<JavaAstDiagnostic> diagnostics,
        ScanCounters counters
    ) {
        counters.parseError();
        var summary = problems.stream()
            .map(Problem::getVerboseMessage)
            .filter(message -> !message.isBlank())
            .findFirst()
            .orElse("JavaParser could not parse source file.");
        var position = problemPosition(problems);
        diagnostics.add(JavaAstDiagnostic.error(
            sourceSnapshotId,
            "JAVA_PARSE_ERROR",
            summary,
            sourceFile.sourcePath(),
            position.line(),
            position.column()
        ));
    }

    static ProblemPosition problemPosition(List<Problem> problems) {
        return problems.stream()
            .map(Problem::getLocation)
            .flatMap(java.util.Optional::stream)
            .map(tokenRange -> tokenRange.toRange().map(range -> range.begin))
            .flatMap(java.util.Optional::stream)
            .filter(Position::valid)
            .findFirst()
            .map(position -> new ProblemPosition(position.line, position.column))
            .orElse(new ProblemPosition(0, 0));
    }

    private static boolean shouldSkip(JavaSourceFile sourceFile) {
        return !sourceFile.relativePath().endsWith(".java")
            || hasSkippedSegment(sourceFile.sourcePath())
            || endsWithExcludedSourceRoot(sourceFile.sourceRoot());
    }

    private static boolean hasSkippedSegment(String sourcePath) {
        return List.of(sourcePath.split("/")).stream()
            .map(segment -> segment.toLowerCase(Locale.ROOT))
            .anyMatch(SKIPPED_DIRECTORY_NAMES::contains);
    }

    private static boolean endsWithExcludedSourceRoot(String sourceRoot) {
        var segments = List.of(sourceRoot.toLowerCase(Locale.ROOT).split("/"));
        return DEFAULT_EXCLUDED_SOURCE_ROOT_SEGMENTS.stream()
            .anyMatch(suffix -> endsWithSegments(segments, suffix));
    }

    private static boolean endsWithSegments(List<String> segments, List<String> suffix) {
        if (segments.size() < suffix.size()) {
            return false;
        }
        var start = segments.size() - suffix.size();
        for (var index = 0; index < suffix.size(); index++) {
            if (!segments.get(start + index).equals(suffix.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static String sortKey(JavaSourceFact fact) {
        return fact.location().sourcePath() + "|"
            + "%08d".formatted(fact.location().lineNumber()) + "|"
            + fact.location().fullyQualifiedClassName() + "|"
            + fact.location().methodName() + "|"
            + fact.signature();
    }

    private static String sortKey(JavaAstDiagnostic diagnostic) {
        return diagnostic.sourcePath() + "|"
            + "%08d".formatted(diagnostic.lineNumber()) + "|"
            + diagnostic.code() + "|"
            + diagnostic.message();
    }

    private static final class ScanCounters {
        private final int receivedFileCount;
        private int parsedFileCount;
        private int skippedFileCount;
        private int parseErrorCount;

        private ScanCounters(int receivedFileCount) {
            this.receivedFileCount = receivedFileCount;
        }

        private int receivedFileCount() {
            return receivedFileCount;
        }

        private int parsedFileCount() {
            return parsedFileCount;
        }

        private int skippedFileCount() {
            return skippedFileCount;
        }

        private int parseErrorCount() {
            return parseErrorCount;
        }

        private void parsed() {
            parsedFileCount++;
        }

        private void skipped() {
            skippedFileCount++;
        }

        private void parseError() {
            parseErrorCount++;
        }
    }

    record ProblemPosition(int line, int column) {
    }
}
