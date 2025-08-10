package com.humanrsc.config;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.hibernate.Session;
import io.quarkus.logging.Log;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;

/**
 * Interceptor para establecer el contexto del tenant en cada conexión de base de datos (misma conexión JPA)
 */
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
@ConnectionPoolIntercepted
public class ConnectionPoolInterceptor {

    

    @Inject
    EntityManager entityManager;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        String tenantID = ThreadLocalStorage.getTenantID();

        if (tenantID != null && !tenantID.trim().isEmpty()) {
            try {
                Session session = entityManager.unwrap(Session.class);
                session.doWork(connection -> {
                    try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT set_config('app.current_tenant', ?, false)")) {
                        stmt.setString(1, tenantID);
                        stmt.execute();
                        // LOG.debugf("Tenant context set in JPA connection: %s", tenantID);
                    }
                });
            } catch (Exception e) {
                Log.warnf("Could not set tenant context in JPA connection: %s", e.getMessage());
            }
        }

        return context.proceed();
    }
}
