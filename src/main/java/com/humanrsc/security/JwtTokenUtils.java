package com.humanrsc.security;

import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.logging.Log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Utilidades para el manejo de tokens JWT
 */
@ApplicationScoped
public class JwtTokenUtils {

    

    @Inject JsonWebToken jwt;

    /**
     * Extrae el tenant del JWT priorizando claim namespaced; fallbacks a claim generica y dominio de email
     */
    public String extractTenantFromJWT() {
        // Namespaced first
        String tenantId = jwt.getClaim("https://hr-platform.api/tenant");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = jwt.getClaim("tenant");
        }
        if (tenantId == null || tenantId.isBlank()) {
            String email = extractEmailFromJWT();
            if (email != null && email.contains("@")) {
                String domain = email.substring(email.indexOf("@") + 1);
                tenantId = domain;
            } else {
                tenantId = "demo-tenant";
            }
        }
        // sanitize: lower-case and replace non word chars/dots by '-'
        tenantId = tenantId.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-");
        // LOG.debugf("Extracted tenant ID: %s", tenantId);
        return tenantId;
    }

    public String extractTenantDomainFromJWT() {
        String email = extractEmailFromJWT();
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf("@") + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Obtiene el email del usuario desde el JWT con varios fallbacks comunes
     */
    public String extractEmailFromJWT() {
        try {
            // Namespaced first (Action PostLogin)
            String email = jwt.getClaim("https://hr-platform.api/email");
            if (email == null) email = jwt.getClaim("email");
            if (email == null) email = jwt.getClaim("preferred_username");
            if (email == null) email = jwt.getClaim("upn");
            if (email == null) email = jwt.getClaim("mail");
            if (email == null) email = jwt.getClaim("unique_name");
            return email;
        } catch (Exception e) {
            Log.debug("No email-like claim found in JWT");
            return null;
        }
    }

    public String extractFirstNameFromJWT() {
        String first = jwt.getClaim("https://hr-platform.api/first_name");
        if (first == null) first = jwt.getClaim("given_name");
        if (first == null) first = jwt.getClaim("name");
        return first;
    }

    public String extractLastNameFromJWT() {
        String last = jwt.getClaim("https://hr-platform.api/last_name");
        if (last == null) last = jwt.getClaim("family_name");
        return last;
    }

    public String extractSubjectFromJWT() { return jwt.getSubject(); }

    public boolean hasClaim(String claimName) {
        Object claim = jwt.getClaim(claimName);
        return claim != null;
    }

    public <T> T getClaim(String claimName) { return jwt.getClaim(claimName); }

    public boolean isTokenValid() {
        try {
            if (jwt == null) return false;
            Long exp = jwt.getClaim("exp");
            if (exp == null) return false;
            return exp > System.currentTimeMillis() / 1000;
        } catch (Exception e) {
            Log.warnf("Error validating JWT token: %s", e.getMessage());
            return false;
        }
    }

    public java.time.Instant getExpirationTime() {
        try {
            Long exp = jwt.getClaim("exp");
            if (exp == null) return null;
            return java.time.Instant.ofEpochSecond(exp);
        } catch (Exception e) {
            Log.warnf("Error getting JWT expiration time: %s", e.getMessage());
            return null;
        }
    }

    public java.time.Instant getIssuedAtTime() {
        try {
            Long iat = jwt.getClaim("iat");
            if (iat == null) return null;
            return java.time.Instant.ofEpochSecond(iat);
        } catch (Exception e) {
            Log.warnf("Error getting JWT issued at time: %s", e.getMessage());
            return null;
        }
    }

    public TenantInfo extractTenantInfoWithPermission(String requiredPermission) {
        try {
            if (!isTokenValid()) {
                Log.warn("JWT token is not valid");
                return null;
            }
            String tenantId = extractTenantFromJWT();
            if (tenantId == null || tenantId.isEmpty()) {
                Log.warn("Could not extract tenant from JWT");
                return null;
            }
            if (!hasRequiredPermission(requiredPermission)) {
                Log.warnf("User does not have required permission: %s", requiredPermission);
                return null;
            }
            String userEmail = extractEmailFromJWT();
            String subject = extractSubjectFromJWT();
            return new TenantInfo(tenantId, userEmail, subject);
        } catch (Exception e) {
            Log.errorf(e, "Error extracting tenant info with permission: %s", requiredPermission);
            return null;
        }
    }

    private boolean hasRequiredPermission(String requiredPermission) {
        try {
            Object roles = jwt.getClaim("roles");
            if (roles != null) {
                if (roles instanceof String) {
                    return roles.toString().contains(requiredPermission);
                } else if (roles instanceof java.util.Collection) {
                    return ((java.util.Collection<?>) roles).stream()
                        .anyMatch(role -> role.toString().contains(requiredPermission));
                }
            }
            Object permissions = jwt.getClaim("permissions");
            if (permissions != null) {
                if (permissions instanceof String) {
                    return permissions.toString().contains(requiredPermission);
                } else if (permissions instanceof java.util.Collection) {
                    return ((java.util.Collection<?>) permissions).stream()
                        .anyMatch(perm -> perm.toString().contains(requiredPermission));
                }
            }
            Object scope = jwt.getClaim("scope");
            if (scope != null) {
                return scope.toString().contains(requiredPermission);
            }
            return false;
        } catch (Exception e) {
            Log.warnf("Error checking permission %s: %s", requiredPermission, e.getMessage());
            return false;
        }
    }

    public static class TenantInfo {
        private final String tenantId;
        private final String userEmail;
        private final String subject;
        private final long timestamp;

        public TenantInfo(String tenantId, String userEmail, String subject) {
            this.tenantId = tenantId;
            this.userEmail = userEmail;
            this.subject = subject;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTenantId() { return tenantId; }
        public String getUserEmail() { return userEmail; }
        public String getSubject() { return subject; }
        public long getTimestamp() { return timestamp; }
    }
}
