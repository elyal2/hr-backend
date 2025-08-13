package com.humanrsc.security;

/**
 * Centralized permission constants to avoid magic strings.
 * Format follows action:resource per security guidelines.
 */
public final class Permissions {

    private Permissions() {}

    // Core resources
    public static final String READ_PEOPLE = "read:people";
    public static final String WRITE_PEOPLE = "write:people";

    public static final String READ_POSITIONS = "read:positions";
    public static final String WRITE_POSITIONS = "write:positions";

    public static final String READ_ORG_UNITS = "read:org-units";
    public static final String WRITE_ORG_UNITS = "write:org-units";

    public static final String READ_POSITION_CATEGORIES = "read:position-categories";
    public static final String WRITE_POSITION_CATEGORIES = "write:position-categories";

    public static final String READ_ASSIGNMENTS = "read:assignments";
    public static final String WRITE_ASSIGNMENTS = "write:assignments";

    public static final String READ_REPLACEMENTS = "read:replacements";
    public static final String WRITE_REPLACEMENTS = "write:replacements";

    public static final String READ_USERS = "read:users";
    public static final String WRITE_USERS = "write:users";

    public static final String READ_SALARIES = "read:salaries";
    public static final String WRITE_SALARIES = "write:salaries";

    public static final String READ_BENEFITS = "read:benefits";
    public static final String WRITE_BENEFITS = "write:benefits";

    public static final String READ_EVALUATIONS = "read:evaluations";
    public static final String WRITE_EVALUATIONS = "write:evaluations";

    public static final String READ_GOALS = "read:goals";
    public static final String WRITE_GOALS = "write:goals";

    public static final String READ_DOCUMENTS = "read:documents";
    public static final String WRITE_DOCUMENTS = "write:documents";

    // Transversal / platform
    public static final String IMPORT_DATA = "import:data";
    public static final String EXPORT_DATA = "export:data";
    public static final String NOTIFY_SEND = "notify:send";

    public static final String STATS_READ = "stats:read"; // tenant-level stats
    public static final String STATS_READ_GLOBAL = "stats:read:global"; // platform-level

    public static final String AUDIT_READ = "audit:read";
    public static final String SETTINGS_MANAGE = "settings:manage";
    public static final String TENANT_MANAGE = "tenant:manage"; // platform-only
    public static final String MODULE_TOGGLE = "module:toggle"; // platform-only

}


