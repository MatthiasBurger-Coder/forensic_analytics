package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record ModuleIdentityCommand(String moduleName, String modulePath) {
    public ModuleIdentityCommand {
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        Objects.requireNonNull(modulePath, "modulePath must not be null");
    }
}
