package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

interface GitCommandRunner {
    GitCommandResult run(GitCommand command);
}
