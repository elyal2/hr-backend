package com.humanrsc.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.humanrsc.security.Permissions.*;

/**
 * Role to permission mapping used when tokens contain only role names.
 */
public final class RolePermissions {

    private RolePermissions() {}

    public static Set<String> permissionsForRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return Collections.emptySet();
        Set<String> allowed = new HashSet<>();
        for (String role : roles) {
            if (role == null) continue;
            String normalized = role.trim().toLowerCase();
            switch (normalized) {
                case "superuser":
                    // Superuser handled in interceptor as bypass, but include wide set just in case
                    addAllCoreReadWrite(allowed);
                    addPlatform(allowed);
                    break;
                case "tenant-admin":
                    addAllCoreReadWrite(allowed);
                    allowed.add(IMPORT_DATA);
                    allowed.add(EXPORT_DATA);
                    allowed.add(NOTIFY_SEND);
                    allowed.add(STATS_READ);
                    allowed.add(SETTINGS_MANAGE);
                    break;
                case "hr-manager":
                    addAllCoreReadWrite(allowed);
                    allowed.add(EXPORT_DATA);
                    allowed.add(NOTIFY_SEND);
                    allowed.add(STATS_READ);
                    break;
                case "hr-viewer":
                    addAllCoreReadOnly(allowed);
                    allowed.add(EXPORT_DATA);
                    allowed.add(STATS_READ);
                    break;
                case "payroll-manager":
                    allowed.add(READ_SALARIES);
                    allowed.add(WRITE_SALARIES);
                    allowed.add(READ_BENEFITS);
                    allowed.add(WRITE_BENEFITS);
                    allowed.add(READ_DOCUMENTS);
                    allowed.add(WRITE_DOCUMENTS);
                    allowed.add(EXPORT_DATA);
                    allowed.add(STATS_READ);
                    break;
                default:
                    break;
            }
        }
        return allowed;
    }

    private static void addAllCoreReadWrite(Set<String> allowed) {
        addAllCoreReadOnly(allowed);
        allowed.add(WRITE_PEOPLE);
        allowed.add(WRITE_POSITIONS);
        allowed.add(WRITE_ORG_UNITS);
        allowed.add(WRITE_USERS);
        allowed.add(WRITE_SALARIES);
        allowed.add(WRITE_BENEFITS);
        allowed.add(WRITE_EVALUATIONS);
        allowed.add(WRITE_GOALS);
        allowed.add(WRITE_DOCUMENTS);
    }

    private static void addAllCoreReadOnly(Set<String> allowed) {
        allowed.add(READ_PEOPLE);
        allowed.add(READ_POSITIONS);
        allowed.add(READ_ORG_UNITS);
        allowed.add(READ_USERS);
        allowed.add(READ_SALARIES);
        allowed.add(READ_BENEFITS);
        allowed.add(READ_EVALUATIONS);
        allowed.add(READ_GOALS);
        allowed.add(READ_DOCUMENTS);
    }

    private static void addPlatform(Set<String> allowed) {
        allowed.add(IMPORT_DATA);
        allowed.add(EXPORT_DATA);
        allowed.add(NOTIFY_SEND);
        allowed.add(STATS_READ);
        allowed.add(STATS_READ_GLOBAL);
        allowed.add(AUDIT_READ);
        allowed.add(SETTINGS_MANAGE);
        allowed.add(TENANT_MANAGE);
        allowed.add(MODULE_TOGGLE);
    }
}


