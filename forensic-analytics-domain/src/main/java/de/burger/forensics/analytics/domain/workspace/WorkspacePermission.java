package de.burger.forensics.analytics.domain.workspace;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum WorkspacePermission {
    READ_WORKSPACE,
    UPDATE_WORKSPACE,
    ARCHIVE_WORKSPACE,
    MANAGE_WORKSPACE_MEMBERS,
    CREATE_PROJECT,
    READ_PROJECT,
    UPDATE_PROJECT,
    ARCHIVE_PROJECT,
    MANAGE_PROJECT_MEMBERS,
    READ_WORKSPACE_AUDIT,
    MANAGE_SHARED_ASSETS;

    private static final Map<WorkspacePermission, Set<WorkspaceRole>> ALLOWED_ROLES = allowedRoles();

    public boolean isGrantedTo(WorkspaceRole role) {
        return ALLOWED_ROLES.get(this).contains(role);
    }

    private static Map<WorkspacePermission, Set<WorkspaceRole>> allowedRoles() {
        var roles = new EnumMap<WorkspacePermission, Set<WorkspaceRole>>(WorkspacePermission.class);
        roles.put(READ_WORKSPACE, EnumSet.allOf(WorkspaceRole.class));
        roles.put(UPDATE_WORKSPACE, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(ARCHIVE_WORKSPACE, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(MANAGE_WORKSPACE_MEMBERS, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(CREATE_PROJECT, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(READ_PROJECT, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(UPDATE_PROJECT, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(ARCHIVE_PROJECT, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(MANAGE_PROJECT_MEMBERS, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        roles.put(READ_WORKSPACE_AUDIT, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.AUDITOR));
        roles.put(MANAGE_SHARED_ASSETS, EnumSet.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        return Map.copyOf(roles);
    }
}
