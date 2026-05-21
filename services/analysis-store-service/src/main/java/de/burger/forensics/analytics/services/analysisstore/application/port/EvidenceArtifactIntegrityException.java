package de.burger.forensics.analytics.services.analysisstore.application.port;

public final class EvidenceArtifactIntegrityException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    private final String diagnosticCode;

    public EvidenceArtifactIntegrityException(String diagnosticCode, String message) {
        super(message);
        if (diagnosticCode == null || diagnosticCode.isBlank()) {
            throw new IllegalArgumentException("diagnosticCode must not be blank");
        }
        this.diagnosticCode = diagnosticCode.strip();
    }

    public String diagnosticCode() {
        return diagnosticCode;
    }
}
