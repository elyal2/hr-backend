package com.humanrsc.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@ApplicationScoped
public class TenantContextService {

    private static final Logger LOG = Logger.getLogger(TenantContextService.class);

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "app.default-tenant", defaultValue = "default")
    String defaultTenant;

    /**
     * Set tenant context for RLS
     */
    public void setTenantContext(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = defaultTenant;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT set_config('app.current_tenant', ?, false)")) {
            
            stmt.setString(1, tenantId);
            stmt.execute();

            LOG.debugf("Tenant context set: %s", tenantId);

        } catch (SQLException e) {
            LOG.errorf(e, "Error setting tenant context: %s", tenantId);
            throw new RuntimeException("Failed to set tenant context", e);
        }
    }

    /**
     * Get current tenant from DB context
     */
    public String getCurrentTenant() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT current_setting('app.current_tenant', true)")) {
            
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String tenant = rs.getString(1);
                return (tenant == null || tenant.isEmpty()) ? defaultTenant : tenant;
            }
            return defaultTenant;

        } catch (SQLException e) {
            LOG.warnf(e, "Error getting current tenant, using default: %s", defaultTenant);
            return defaultTenant;
        }
    }
}