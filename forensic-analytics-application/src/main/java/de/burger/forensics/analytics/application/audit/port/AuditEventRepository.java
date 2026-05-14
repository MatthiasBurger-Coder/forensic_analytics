package de.burger.forensics.analytics.application.audit.port;

import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.List;

public interface AuditEventRepository {
    void append(AuditEvent event);

    List<AuditEvent> findByWorkspace(WorkspaceId workspaceId);
}
