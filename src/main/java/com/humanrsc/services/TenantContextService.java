package com.humanrsc.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;
import io.quarkus.logging.Log;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.sql.PreparedStatement;

@ApplicationScoped
public class TenantContextService {

    

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "app.default-tenant", defaultValue = "default")
    String defaultTenant;

    /**
     * Set tenant context for RLS en la misma conexión JPA/Transacción
     */
    @Transactional
    public void setTenantContext(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = defaultTenant;
        }

        try {
            Session session = entityManager.unwrap(Session.class);
            String finalTenant = tenantId;
            session.doWork(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT set_config('app.current_tenant', ?, false)")) {
                    stmt.setString(1, finalTenant);
                    stmt.execute();
                }
            });
            // LOG.debugf("Tenant context set (JPA): %s", tenantId);
        } catch (Exception e) {
            Log.errorf(e, "Error setting tenant context: %s", tenantId);
            throw new RuntimeException("Failed to set tenant context", e);
        }
    }

    /**
     * Get current tenant from DB context usando la misma conexión JPA
     */
    @Transactional
    public String getCurrentTenant() {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT current_setting('app.current_tenant', true)")
                .getSingleResult();
            String tenant = result != null ? result.toString() : null;
            return (tenant == null || tenant.isEmpty()) ? defaultTenant : tenant;
        } catch (Exception e) {
            Log.warnf(e, "Error getting current tenant, using default: %s", defaultTenant);
            return defaultTenant;
        }
    }
}