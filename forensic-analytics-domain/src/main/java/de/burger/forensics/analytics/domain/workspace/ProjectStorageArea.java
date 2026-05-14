package de.burger.forensics.analytics.domain.workspace;

public enum ProjectStorageArea {
    EVIDENCE_ORIGINAL("evidence_original"),
    EVIDENCE_PROCESSED("evidence_processed"),
    ANALYSIS_RESULTS("analysis_results"),
    REPORTS("reports"),
    LOGS("logs");

    private final String directoryName;

    ProjectStorageArea(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }
}
