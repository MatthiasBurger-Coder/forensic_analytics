package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitRepositoryMetadataAdapterTest {
    private static final RepositoryReference REPOSITORY = new RepositoryReference(
        "https://example.com/acme/demo.git",
        "github",
        Map.of()
    );

    @TempDir
    private Path metadataRoot;

    @Test
    void resolvesDefaultBranchFromRemoteHeadWithoutNetwork() throws Exception {
        var runner = new MetadataRunner("ref: refs/heads/main\tHEAD\n");
        var adapter = adapter(runner);

        var metadata = adapter.resolveMetadata(REPOSITORY, new RepositoryMetadataPreviewPolicy(30));

        assertTrue(metadata.defaultBranchResolved());
        assertEquals("main", metadata.repository().defaultBranch());
        assertEquals("example.com/acme/demo", metadata.repository().repositoryKey().value());
        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("--symref")));
        assertTrue(runner.commands.stream().anyMatch(command ->
            command.arguments().contains("http.curloptResolve=example.com:443:93.184.216.34")
        ));
        assertTrue(runner.commands.stream().noneMatch(command -> command.arguments().contains("clone")));
        assertTrue(runner.commands.stream().noneMatch(command -> command.arguments().contains("submodule")));
        assertTrue(Files.list(metadataRoot).findAny().isEmpty());
    }

    @Test
    void fallsBackOnlyToVerifiedMainOrMasterBranches() throws Exception {
        var runner = new MetadataRunner("");
        runner.existingBranches.add("master");
        var adapter = adapter(runner);

        var metadata = adapter.resolveMetadata(REPOSITORY, new RepositoryMetadataPreviewPolicy(30));

        assertTrue(metadata.defaultBranchResolved());
        assertEquals("master", metadata.repository().defaultBranch());
        assertEquals("DEFAULT_BRANCH_FALLBACK", metadata.diagnostics().getFirst().code());
        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("refs/heads/main")));
        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("refs/heads/master")));
    }

    @Test
    void returnsUnresolvedMetadataWithSanitizedDiagnostic() throws Exception {
        var runner = new MetadataRunner("raw failure from /tmp/private\n");
        var adapter = adapter(runner);

        var metadata = adapter.resolveMetadata(REPOSITORY, new RepositoryMetadataPreviewPolicy(30));

        assertFalse(metadata.defaultBranchResolved());
        assertEquals("", metadata.repository().defaultBranch());
        assertEquals("DEFAULT_BRANCH_UNRESOLVED", metadata.diagnostics().getFirst().code());
        assertFalse(metadata.diagnostics().getFirst().message().contains("/tmp"));
        assertTrue(Files.list(metadataRoot).findAny().isEmpty());
    }

    private GitRepositoryMetadataAdapter adapter(MetadataRunner runner) throws Exception {
        return new GitRepositoryMetadataAdapter(
            runner,
            metadataRoot,
            new RemoteHostValidator(host -> List.of(InetAddress.getByName("93.184.216.34")))
        );
    }

    private static final class MetadataRunner implements GitCommandRunner {
        private final List<GitCommand> commands = new ArrayList<>();
        private final List<String> existingBranches = new ArrayList<>();
        private final String headOutput;

        private MetadataRunner(String headOutput) {
            this.headOutput = headOutput;
        }

        @Override
        public GitCommandResult run(GitCommand command) {
            commands.add(command);
            if (command.arguments().contains("--symref")) {
                return new GitCommandResult(headOutput.isBlank() ? 1 : 0, headOutput);
            }
            var ref = command.arguments().getLast();
            var branch = ref.substring("refs/heads/".length());
            return new GitCommandResult(existingBranches.contains(branch) ? 0 : 1, "");
        }
    }
}
