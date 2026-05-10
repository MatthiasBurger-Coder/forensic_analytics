package de.burger.forensics.analytics.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliArgumentParserTest {
    private final CliArgumentParser parser = new CliArgumentParser();

    @Test
    void parsesAnalyzeCommand() {
        var command = assertInstanceOf(AnalyzeCommand.class, parser.parse(new String[] {
            "analyze",
            "--repo", "file:///workspace/project",
            "--profile", "baseline",
            "--output", "build/out",
            "--joern-mode", "docker"
        }));

        assertEquals("file:///workspace/project", command.repositoryLocation());
        assertEquals("baseline", command.profile());
        assertEquals(JoernMode.DOCKER, command.joernMode());
    }

    @Test
    void parsesHelpCommand() {
        assertInstanceOf(HelpCommand.class, parser.parse(new String[] {"--help"}));
        assertInstanceOf(HelpCommand.class, parser.parse(new String[] {"help"}));
    }

    @Test
    void rejectsInvalidAnalyzeArguments() {
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {}));
        assertThrows(CliUsageException.class, () -> parser.parse(null));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"inspect"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"analyze", "--repo", "repo"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"analyze", "--repo"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"analyze", "--repo", "--profile", "baseline"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"analyze", "--repo", "repo", "--repo", "other"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {"analyze", "--unknown", "value"}));
        assertThrows(CliUsageException.class, () -> parser.parse(new String[] {
            "analyze",
            "--repo", "repo",
            "--profile", "baseline",
            "--output", "build/out",
            "--joern-mode", "remote"
        }));
    }
}
