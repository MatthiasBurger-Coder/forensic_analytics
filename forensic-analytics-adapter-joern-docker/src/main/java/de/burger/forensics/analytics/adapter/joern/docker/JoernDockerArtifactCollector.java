package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;

public interface JoernDockerArtifactCollector {
    List<ArtifactReference> collect(JoernDockerArtifactPaths paths);
}
