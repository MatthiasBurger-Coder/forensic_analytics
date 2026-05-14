package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.audit.port.AuditEventRepository;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryAuditEventRepository implements AuditEventRepository, WorkspaceAuditPort {
    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(AuditEvent event) {
        append(event);
    }

    @Override
    public void append(AuditEvent event) {
        events.add(Objects.requireNonNull(event, "event must not be null"));
    }

    @Override
    public List<AuditEvent> findByWorkspace(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return events.stream()
            .filter(event -> event.workspaceId().equals(workspaceId))
            .sorted(Comparator
                .comparing(AuditEvent::occurredAt)
                .thenComparing(AuditEvent::action)
                .thenComparing(AuditEvent::targetType)
                .thenComparing(AuditEvent::targetId))
            .toList();
    }
}
