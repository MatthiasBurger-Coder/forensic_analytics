package de.burger.forensics.analytics.application.audit;

import de.burger.forensics.analytics.application.audit.command.ListWorkspaceAuditEventsCommand;
import de.burger.forensics.analytics.domain.audit.AuditEvent;

import java.util.List;

public interface AuditLogUseCase {
    List<AuditEvent> listWorkspaceEvents(ListWorkspaceAuditEventsCommand command);
}
