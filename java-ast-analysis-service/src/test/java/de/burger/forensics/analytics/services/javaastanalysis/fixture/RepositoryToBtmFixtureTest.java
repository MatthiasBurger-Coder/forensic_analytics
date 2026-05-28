package de.burger.forensics.analytics.services.javaastanalysis.fixture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RepositoryToBtmFixtureTest {
    @Test
    void loadsServiceLocalRepositoryToBtmFixtureWithoutPrivateCoordinates() throws IOException {
        var fixture = fixture();

        assertEquals("repository-to-btm-fixture-v1", fixture.getProperty("schemaVersion"));
        assertEquals("java-ast-analysis-service", fixture.getProperty("sourceFactByteOwnerService"));
        assertEquals(
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
            fixture.getProperty("sourceFactByteRetrievalContract")
        );
        assertEquals(
            fixture.getProperty("sourceFactArtifactPath"),
            fixture.getProperty("sourceFactByteRetrievalReference")
        );
        assertEquals("UNAVAILABLE", fixture.getProperty("joernAvailability"));
        assertSafeFixtureValues(fixture);
    }

    private static Properties fixture() throws IOException {
        var properties = new Properties();
        try (var input = RepositoryToBtmFixtureTest.class.getClassLoader()
            .getResourceAsStream("repository-to-btm/v1/source-fact-artifact.properties")) {
            assertNotNull(input, "repository-to-btm fixture resource must exist");
            properties.load(input);
        }
        return properties;
    }

    private static void assertSafeFixtureValues(Properties fixture) {
        fixture.forEach((key, value) -> {
            var text = value.toString();
            var lower = text.toLowerCase(java.util.Locale.ROOT);
            assertFalse(text.startsWith("/"));
            assertFalse(text.contains("\\"));
            assertFalse(lower.startsWith("file:"));
            assertFalse(lower.contains("://"));
            assertFalse(lower.contains("secret"));
            assertFalse(lower.contains("token"));
            assertFalse(lower.contains("credential"));
        });
    }
}
