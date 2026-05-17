package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

public record GitCommandResult(int exitCode, String output) {
    public GitCommandResult {
        output = output == null ? "" : output;
    }

    public String trimmedOutput() {
        return output.trim();
    }
}
