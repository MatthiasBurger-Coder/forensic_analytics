package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;

import java.util.List;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.sha256Hex;

final class RepositorySourceSnapshotFactory {
    private RepositorySourceSnapshotFactory() {
    }

    static SourceSnapshotId sourceSnapshotId(
        RepositoryReference repository,
        RevisionSelector revision,
        CheckoutResult checkout
    ) {
        return SourceSnapshotId.deterministic(
            repository,
            revision,
            checkout.resolvedCommit(),
            manifestSha256(repository, revision, checkout.sourceRoots(), checkout.resolvedCommit())
        );
    }

    static String manifestSha256(
        RepositoryReference repository,
        RevisionSelector revision,
        List<SourceRoot> sourceRoots,
        String resolvedCommit
    ) {
        return sha256Hex(manifestPayload(repository, revision, resolvedCommit, sourceRoots));
    }

    static String manifestPayload(
        RepositoryReference repository,
        RevisionSelector revision,
        String resolvedCommit,
        List<SourceRoot> sourceRoots
    ) {
        return String.join(
            "\n",
            repository.remoteUrl(),
            revision.branch(),
            revision.commit(),
            resolvedCommit,
            sourceRoots.toString()
        );
    }
}
