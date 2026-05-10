# Workflow: Migrate the Full System to Java 25 and JUnit 6

**Status:** Draft
**Target branch:** `feature/java25-junit6-migration`
**Scope:** Full repository migration from Java 17 / JUnit 5 to Java 25 / JUnit 6
**Repository:** `forensics_tracing`
**Execution model:** Slice-based Codex workflow with strict verification after every slice

---

## 1. Goal

Migrate the complete Forensics Tracing system to:

```text
Java baseline: 25
JUnit baseline: 6.x stable
Gradle execution: Java 25-compatible
Test execution: Java 25 toolchain
Coverage: Java 25-compatible JaCoCo
Runtime weaving: Java 25-compatible AspectJ Weaver
Mocking: Java 25-compatible Mockito / Byte Buddy
Repository documentation: Java 25 / JUnit 6 aligned
CI: Java 25 aligned
Dependency verification: updated and strict
```

This is a breaking baseline change. After this migration, the project must no longer claim Java 17 or JUnit 5 as its active project baseline.

---

## 2. Non-goals

Do **not** perform unrelated refactoring.

Do **not** rewrite production code only because Java 25 is available.

Do **not** introduce preview features.

Do **not** add `--enable-preview` unless a later task explicitly requires preview Java language features.

Do **not** lower coverage thresholds.

Do **not** remove quality gates to make the migration pass.

Do **not** replace ArchUnit rules with weaker tests.

Do **not** introduce hidden compatibility wrappers.

Do **not** silently ignore dependency verification changes.

---

## 3. Important Migration Notes

### 3.1 JUnit 6 source imports

Most test source imports remain under:

```java
org.junit.jupiter.api.*
org.junit.jupiter.params.*
org.junit.jupiter.params.provider.*
```

JUnit 6 does **not** imply that all test imports are renamed away from Jupiter.

The migration is mainly about:

```text
- JUnit BOM version
- JUnit Platform version alignment
- deprecated or removed JUnit Platform APIs
- removed platform artifacts
- test runtime behavior
- documentation and quality gate naming
```

### 3.2 JUnit Platform versioning

JUnit 6 uses a single version number for Platform, Jupiter, and Vintage artifacts.

The repository must not keep a stale separate `junit-platform = "1.x"` version if Platform artifacts are managed by the JUnit BOM.

### 3.3 ArchUnit artifact naming

ArchUnit may still use artifact names containing `junit5`, for example:

```text
com.tngtech.archunit:archunit-junit5
```

Do not rename this dependency to a non-existing `archunit-junit6` artifact unless such an artifact is verified in Maven Central and required by the project.

The correct criterion is: ArchUnit tests must run successfully on JUnit 6 and Java 25.

### 3.4 JavaParser capability

The project scans Java source code. The Java baseline migration must therefore verify that the JavaParser version can parse Java 25 source syntax.

If the current JavaParser version is not explicitly Java 25-capable, update it to a verified Java 25-capable release and add a parser regression test.

### 3.5 Runtime instrumentation capability

This project uses runtime tracing, AspectJ weaving, Mockito, Byte Buddy, and Byteman-related code.

Java 25 migration is not complete until the following areas are verified under a Java 25 test JVM:

```text
- AspectJ load-time weaving
- MethodLoggingAspect tests
- RtTrace tests
- Mockito-based tests
- Gradle TestKit tests
- JaCoCo report generation
- JaCoCo verification
- Byteman-related compileOnly and test dependencies
```

---

## 4. Expected Files to Inspect and Update

At minimum, inspect these files:

```text
gradle/libs.versions.toml
build.gradle.kts
settings.gradle.kts
gradle/wrapper/gradle-wrapper.properties
gradle/verification-metadata.xml
AGENTS.md
QUALITY.md
README.md
Commit.md
.github/workflows/*.yml
.github/workflows/*.yaml
src/main/java/**/*.java
src/test/java/**/*.java
```

Also inspect inactive workflow files if present, for example:

```text
.github/workflows/*_not_in_use
```

Inactive files may still contain stale Java/JUnit documentation and should be either updated or explicitly marked as obsolete.

---

## 5. Slice 0 — Preflight and Current-State Verification

### Goal

Verify the repository state before changing anything.

### Commands

```bash
git status --short
git branch --show-current
git diff --stat
git diff
git diff --cached
java --version
./gradlew --version
./gradlew -q javaToolchains --console=plain || true
```

### Inspect current baseline references

```bash
rg -n "Java 17|JDK 17|JUnit 5|junit5|java17|VERSION_17|release\.set\(17\)|JavaLanguageVersion\.of\(17\)|junit-platform|5\.13\.4|1\.11\.3|0\.8\.13|1\.9\.24" \
  AGENTS.md QUALITY.md README.md Commit.md build.gradle.kts settings.gradle.kts gradle .github src || true
```

### Inspect build/test wiring

```bash
rg -n "useJUnitPlatform|junit|jupiter|platform|archunit|mockito|byte-buddy|aspectj|jacoco|lombok|javaparser|byteman|javaLauncher|toolchain|sourceCompatibility|targetCompatibility|options\.release" \
  build.gradle.kts gradle src .github AGENTS.md QUALITY.md README.md Commit.md || true
```

### Acceptance criteria

```text
[ ] Current Java 17 references are listed.
[ ] Current JUnit 5 references are listed.
[ ] Current CI Java version is known.
[ ] Current dependency verification state is known.
[ ] No source changes were made in this slice.
```

### Stop conditions

Stop and report if:

```text
- The repository has unrelated uncommitted changes.
- The current branch is not suitable for migration work.
- The Gradle wrapper cannot run at all before the migration.
- AGENTS.md, QUALITY.md, README.md, and build.gradle.kts disagree in a way that makes the intended baseline ambiguous.
```

---

## 6. Slice 1 — Create Migration Branch

### Goal

Isolate the migration in its own branch.

### Commands

```bash
git switch -c feature/java25-junit6-migration
```

If the branch already exists:

```bash
git switch feature/java25-junit6-migration
```

### Acceptance criteria

```text
[ ] Work happens on feature/java25-junit6-migration.
[ ] No unrelated file changes are present.
```

---

## 7. Slice 2 — Update Version Catalog

### Goal

Move the dependency catalog to Java 25-compatible test and instrumentation dependencies.

### File

```text
gradle/libs.versions.toml
```

### Required changes

Update the catalog so that it uses stable Java 25-compatible versions.

Recommended target baseline:

```toml
[versions]
junit = "6.0.3"
assertj = "3.27.7"
javaparser = "3.28.0"
aspectj = "1.9.25.1"
bytebuddy = "1.18.2"
mockito = "5.23.0"
byteman = "4.0.26"
jacoco = "0.8.14"
lombok = "1.18.46"
```

Rules:

```text
- Do not use JUnit milestone or RC versions unless explicitly required.
- Remove the separate `junit-platform = "1.x"` version if the Platform launcher is managed by the JUnit BOM.
- Keep `junit-platform-launcher` versionless when imported through the JUnit BOM.
- Keep `junit-jupiter`, `junit-jupiter-api`, and `junit-jupiter-engine` versionless when imported through the JUnit BOM.
- Keep Mockito modules versionless when imported through the Mockito BOM.
- Do not invent an ArchUnit JUnit 6 artifact unless verified.
```

Expected JUnit dependency shape:

```toml
junit-bom               = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter           = { module = "org.junit.jupiter:junit-jupiter" }
junit-jupiter-api       = { module = "org.junit.jupiter:junit-jupiter-api" }
junit-jupiter-engine    = { module = "org.junit.jupiter:junit-jupiter-engine" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
```

### Verification commands

```bash
./gradlew help --dependency-verification lenient --console=plain --stacktrace
./gradlew dependencies --configuration testRuntimeClasspath --dependency-verification lenient --console=plain
```

### Acceptance criteria

```text
[ ] JUnit BOM uses JUnit 6.x stable.
[ ] No stale JUnit Platform 1.x version remains.
[ ] JaCoCo is Java 25-capable.
[ ] AspectJ Weaver is Java 25-capable.
[ ] Lombok is Java 25-capable.
[ ] Mockito and Byte Buddy versions are compatible with Java 25.
[ ] JavaParser is verified for Java 25 parsing capability.
```

---

## 8. Slice 3 — Update Gradle Java Toolchain to Java 25

### Goal

Make the project compile and test with Java 25.

### File

```text
build.gradle.kts
```

### Required changes

Replace hard-coded Java 17 values with a single Java baseline variable.

Recommended shape:

```kotlin
val javaBaseline = 25
val java25 = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(javaBaseline))
}

plugins.withType<JavaPlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaBaseline))
        sourceCompatibility = JavaVersion.toVersion(javaBaseline)
        targetCompatibility = JavaVersion.toVersion(javaBaseline)
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaBaseline)
        options.compilerArgs.addAll(listOf("-Xlint:all"))
    }
}
```

Update all test launcher wiring:

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher.set(java25)
}
```

Rename local variables from `java17` to `java25` or generic `javaLauncher`.

### Rules

```text
- Do not add --enable-preview.
- Do not remove -Xlint:all.
- Do not weaken test logging.
- Do not remove AspectJ javaagent wiring.
- Do not disable Gradle TestKit tests.
```

### Verification commands

```bash
./gradlew clean compileJava compileTestJava --dependency-verification lenient --console=plain --stacktrace
```

### Acceptance criteria

```text
[ ] Main sources compile with --release 25.
[ ] Test sources compile with Java 25.
[ ] All references to `java17`, `VERSION_17`, and `JavaLanguageVersion.of(17)` are removed or documented as historical text only.
```

---

## 9. Slice 4 — Migrate JUnit 5 Configuration to JUnit 6

### Goal

Ensure all JUnit runtime wiring is JUnit 6-compatible.

### Files

```text
build.gradle.kts
gradle/libs.versions.toml
src/test/java/**/*.java
```

### Search for removed or risky JUnit APIs

```bash
rg -n "junit-platform-runner|junit-platform-jfr|org\.junit\.platform\.runner|@RunWith|Launcher\.execute\(|LauncherDiscoveryRequest|TestPlan|Vintage|junit-vintage|junit\.platform\.suite\.commons" src build.gradle.kts gradle || true
```

### Expected source behavior

Most tests should remain unchanged if they only use:

```text
@Test
@BeforeEach
@AfterEach
@BeforeAll
@ParameterizedTest
@MethodSource
Arguments
Assertions
```

Do not perform mechanical import rewrites unless compilation proves they are necessary.

### Verification commands

```bash
./gradlew test --dependency-verification lenient --console=plain --stacktrace
```

### Acceptance criteria

```text
[ ] Tests run on JUnit 6.
[ ] No JUnit Platform 1.x dependency remains.
[ ] No removed JUnit Platform artifact is referenced.
[ ] Existing test behavior is preserved.
[ ] ArchUnit tests still execute.
```

---

## 10. Slice 5 — Verify JavaParser Java 25 Parsing

### Goal

Prove that the scanner can parse Java 25 source syntax.

### Add or update test

Add a focused regression test under the JavaParser scanner/support test package.

Suggested test name:

```text
JavaParserJava25CompatibilityTest
```

Test scenarios:

```text
- A normal Java 25 source file parses successfully.
- A switch expression / modern syntax sample does not break scanning.
- The scanner still emits expected ScanEvent data for ordinary methods.
```

Do not add preview syntax unless preview support is explicitly enabled in the build, which is not part of this workflow.

### Verification commands

```bash
./gradlew test --tests '*JavaParserJava25CompatibilityTest' --dependency-verification lenient --console=plain --stacktrace
./gradlew test --tests '*JavaParser*' --dependency-verification lenient --console=plain --stacktrace
```

### Acceptance criteria

```text
[ ] JavaParser-related tests pass under Java 25.
[ ] Java 25 non-preview syntax does not break scanner behavior.
[ ] No preview feature dependency is introduced.
```

---

## 11. Slice 6 — Verify Runtime Instrumentation on Java 25

### Goal

Prove that runtime tracing, AspectJ weaving, and Mockito continue to work under Java 25.

### Targeted tests

Run focused tests first:

```bash
./gradlew test --tests '*MethodLoggingAspectTest' --dependency-verification lenient --console=plain --stacktrace
./gradlew test --tests '*LoggingSafetyTest' --dependency-verification lenient --console=plain --stacktrace
./gradlew test --tests '*RtTrace*' --dependency-verification lenient --console=plain --stacktrace
./gradlew test --tests '*PluginAdapterArchitectureTest' --dependency-verification lenient --console=plain --stacktrace
./gradlew test --tests '*HexagonRulesTest' --dependency-verification lenient --console=plain --stacktrace
```

### Rules

```text
- Keep AspectJ output visible enough to diagnose weaving failures.
- Do not disable tests to hide Java 25 instrumentation problems.
- Add JVM --add-opens only if a concrete failing test proves it is necessary.
- Any added --add-opens must be documented with the exact failing test and exception.
```

### Acceptance criteria

```text
[ ] AspectJ load-time weaving works under Java 25.
[ ] Mockito-based tests work under Java 25.
[ ] Runtime trace tests work under Java 25.
[ ] Architecture tests work under Java 25.
```

---

## 12. Slice 7 — Update CI to Java 25

### Goal

Make GitHub Actions run with Java 25.

### Files

```text
.github/workflows/*.yml
.github/workflows/*.yaml
.github/workflows/*_not_in_use
```

### Required active workflow update

Update active workflow setup steps from Java 17 to Java 25.

Example:

```yaml
- name: Set up JDK 25
  uses: actions/setup-java@v4
  with:
    java-version: '25'
    distribution: 'temurin'
    cache: 'gradle'
```

### Verification commands

```bash
rg -n "JDK 17|Java 17|java-version: '17'|java-version: \"17\"|setup-java" .github || true
```

### Acceptance criteria

```text
[ ] Active CI uses Java 25.
[ ] Workflow step names no longer say JDK 17.
[ ] Inactive workflow files are either updated or clearly marked obsolete.
[ ] CI still runs the full quality gate.
```

---

## 13. Slice 8 — Update Documentation and Agent Rules

### Goal

Align all repository documentation with the new baseline.

### Files

```text
AGENTS.md
QUALITY.md
README.md
Commit.md
workflow.md files, if any
```

### Required documentation updates

Replace current project baseline references:

```text
Java 17 -> Java 25
JUnit 5 -> JUnit 6
```

Update quality gate language to mention:

```text
- Java 25 toolchain
- JUnit 6 test runtime
- Java 25-compatible JaCoCo
- Java 25-compatible AspectJ Weaver
- dependency verification refresh after dependency changes
```

### Required commands to document in QUALITY.md

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

If connector parity tests exist, keep them:

```bash
./gradlew test --tests '*BtmGenerationAdapterValidationTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*BuildToolConnectorParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenJoernConfigurationParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenFullAnalysisParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenReactorAggregationTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*HexagonRulesTest' --dependency-verification strict --console=plain --stacktrace
```

### Search command

```bash
rg -n "Java 17|JDK 17|JUnit 5|junit5|java17|VERSION_17|release\.set\(17\)|JavaLanguageVersion\.of\(17\)|junit-platform =|1\.11\.3|5\.13\.4" \
  AGENTS.md QUALITY.md README.md Commit.md build.gradle.kts settings.gradle.kts gradle .github src || true
```

### Acceptance criteria

```text
[ ] AGENTS.md declares Java 25 and JUnit 6.
[ ] QUALITY.md declares Java 25 and JUnit 6 verification.
[ ] README.md no longer instructs users to run with Java 17.
[ ] Commit.md no longer states JUnit 5 as the active quality baseline.
[ ] Historical references are clearly marked as historical if kept.
```

---

## 14. Slice 9 — Refresh Dependency Verification Metadata

### Goal

Update `gradle/verification-metadata.xml` only for expected dependency changes.

### File

```text
gradle/verification-metadata.xml
```

### First run with lenient verification

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage \
  --dependency-verification lenient \
  --console=plain \
  --stacktrace
```

### Refresh metadata

Use Gradle metadata generation for the changed dependency set:

```bash
./gradlew help \
  --write-verification-metadata sha256 \
  --dependency-verification lenient \
  --console=plain
```

If test-runtime dependencies are not fully captured by `help`, run:

```bash
./gradlew clean test \
  --write-verification-metadata sha256 \
  --dependency-verification lenient \
  --console=plain \
  --stacktrace
```

### Inspect metadata diff carefully

```bash
git diff -- gradle/verification-metadata.xml
```

### Rules

```text
- Keep only expected new or changed artifacts.
- Do not add broad trust rules unless there is a verified Gradle/IDE metadata reason.
- Do not remove existing verification metadata without understanding why it disappeared.
- Do not switch off dependency verification.
```

### Strict verification

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage \
  --dependency-verification strict \
  --console=plain \
  --stacktrace
```

### Acceptance criteria

```text
[ ] Strict dependency verification passes.
[ ] Metadata diff only contains expected dependency changes.
[ ] No broad unreviewed trust rule was introduced.
```

---

## 15. Slice 10 — Full Local Quality Gate

### Goal

Verify that the full system works under Java 25 and JUnit 6.

### Commands

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage \
  --dependency-verification strict \
  --console=plain \
  --stacktrace

./gradlew validatePlugins \
  --dependency-verification strict \
  --no-daemon \
  --console=plain \
  --stacktrace
```

If Maven connector parity is part of the current repository state, also run:

```bash
./gradlew test --tests '*BtmGenerationAdapterValidationTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*BuildToolConnectorParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenJoernConfigurationParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenFullAnalysisParityTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*MavenReactorAggregationTest' --dependency-verification strict --console=plain --stacktrace
./gradlew test --tests '*HexagonRulesTest' --dependency-verification strict --console=plain --stacktrace
```

Optional SonarCloud run if token is available:

```bash
./gradlew sonar --dependency-verification strict --console=plain --stacktrace
```

### Acceptance criteria

```text
[ ] Full Gradle quality gate passes.
[ ] Plugin validation passes.
[ ] Connector parity tests pass, if present.
[ ] SonarCloud is executed if token is available, otherwise skipped and reported.
```

---

## 16. Slice 11 — Final Consistency Scan

### Goal

Make sure no stale baseline references remain.

### Commands

```bash
rg -n "Java 17|JDK 17|JUnit 5|junit5|java17|VERSION_17|release\.set\(17\)|JavaLanguageVersion\.of\(17\)|junit-platform =|1\.11\.3|5\.13\.4|0\.8\.13|1\.9\.24" \
  AGENTS.md QUALITY.md README.md Commit.md build.gradle.kts settings.gradle.kts gradle .github src || true

git status --short
git diff --stat
git diff -- AGENTS.md QUALITY.md README.md Commit.md build.gradle.kts settings.gradle.kts gradle .github src
```

### Acceptance criteria

```text
[ ] No stale active Java 17 references remain.
[ ] No stale active JUnit 5 references remain.
[ ] No stale JUnit Platform 1.x catalog version remains.
[ ] No stale Java 17 CI setup remains.
[ ] Remaining historical references are explicitly marked as historical.
```

---

## 17. Commit Requirements

### Commit message template

```text
build: migrate project baseline to Java 25 and JUnit 6

What changed:
- Updated Gradle Java toolchain, source compatibility, target compatibility, and --release to Java 25.
- Updated JUnit dependencies to JUnit 6 via the JUnit BOM.
- Removed stale separate JUnit Platform 1.x versioning.
- Updated Java 25-sensitive tooling dependencies such as JaCoCo, AspectJ, Lombok, Mockito, Byte Buddy, and JavaParser where required.
- Updated CI to use JDK 25.
- Updated AGENTS.md, QUALITY.md, README.md, and commit documentation to reflect the new baseline.
- Refreshed dependency verification metadata for expected dependency changes.

Why:
- The repository baseline was Java 17 / JUnit 5.
- The requested project baseline is Java 25 / JUnit 6.
- Java 25 requires compatible bytecode, coverage, weaving, mocking, parsing, and CI tooling.

How verified:
- ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
- ./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
- Targeted JUnit, ArchUnit, JavaParser, runtime tracing, AspectJ, and Mockito tests.
- Connector parity tests where present.

Breaking changes:
- The project now requires Java 25 for build and test execution.
- Consumers expecting a Java 17 baseline must remain on an older release or use a dedicated compatibility branch.
```

### Commit commands

```bash
git status --short
git add gradle/libs.versions.toml build.gradle.kts settings.gradle.kts gradle/verification-metadata.xml AGENTS.md QUALITY.md README.md Commit.md .github src
git status --short
git diff --cached --stat
git commit -m "build: migrate project baseline to Java 25 and JUnit 6"
```

Push:

```bash
git push -u origin feature/java25-junit6-migration
```

---

## 18. Final Definition of Done

```text
[ ] Repository builds with Java 25.
[ ] Repository tests run with JUnit 6.
[ ] JUnit Platform 1.x is removed from active version management.
[ ] Java 25 toolchain is used for compile and test tasks.
[ ] CI uses Java 25.
[ ] JaCoCo supports Java 25 bytecode.
[ ] AspectJ Weaver supports Java 25 runtime weaving.
[ ] Mockito / Byte Buddy tests pass on Java 25.
[ ] JavaParser can parse Java 25-compatible source samples.
[ ] AGENTS.md declares Java 25 / JUnit 6.
[ ] QUALITY.md declares Java 25 / JUnit 6 quality gates.
[ ] README.md setup instructions are Java 25-aligned.
[ ] Dependency verification passes in strict mode.
[ ] Full local quality gate passes.
[ ] Commit message documents what, why, how, verification, and breaking impact.
```

---

## 19. Mandatory Stop-and-Report Cases

Stop immediately and report if any of the following happens:

```text
- Java 25 toolchain cannot be resolved locally or via Foojay.
- Gradle cannot run with the configured Java 25 environment.
- JUnit 6 causes removed API failures that require architectural decisions.
- ArchUnit does not execute under JUnit 6.
- AspectJ weaving fails under Java 25.
- Mockito fails due to Java 25 bytecode or instrumentation behavior.
- JaCoCo cannot instrument or report Java 25 class files.
- JavaParser cannot parse required non-preview Java 25 source syntax.
- Dependency verification requires unexpected trust rules.
- CI workflow changes conflict with repository policy.
- Quality gates fail and cannot be fixed within this migration slice.
```

The report must include:

```text
- exact command
- exact error
- affected file
- suspected root cause
- proposed next step
- whether this blocks the migration
```
