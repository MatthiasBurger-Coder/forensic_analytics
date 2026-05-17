package de.burger.forensics.analytics.services.ingestion.domain;

public record ModuleIdentity(String moduleName, String modulePath) {
    public ModuleIdentity {
        moduleName = RequiredText.require(moduleName, "moduleName");
        modulePath = RequiredText.require(modulePath, "modulePath");
    }
}
