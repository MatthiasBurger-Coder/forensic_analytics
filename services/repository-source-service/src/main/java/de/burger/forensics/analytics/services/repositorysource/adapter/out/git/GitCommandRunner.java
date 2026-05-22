package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

interface GitCommandRunner {
    GitCommandResult run(GitCommand command);
}
