package de.burger.forensics.analytics.application.workspace.port;

import de.burger.forensics.analytics.domain.audit.AuditEvent;

public interface WorkspaceAuditPort {
    void publish(AuditEvent event);
}
