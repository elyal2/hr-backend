package com.humanrsc.security;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.services.TenantContextService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.logging.Log;

/**
 * Interceptor para extraer el tenant del JWT y establecerlo en el contexto
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@JWTSecured
public class JWTSecurityInterceptor {

    

    @Inject
    JsonWebToken jwt;

    @Inject
    JwtTokenUtils jwtTokenUtils;

    @Inject
    TenantContextService tenantContextService;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            String tenantID = jwtTokenUtils.extractTenantFromJWT();
            if (tenantID != null && !tenantID.trim().isEmpty()) {
                // LOG.debugf("Setting tenant context: %s", tenantID);
                ThreadLocalStorage.setTenantID(tenantID);
                tenantContextService.setTenantContext(tenantID);
                // LOG.debugf("Tenant context set successfully: %s", tenantID);
            } else {
                Log.warn("No tenant found in JWT, using default");
                String defaultTenant = "demo-tenant";
                ThreadLocalStorage.setTenantID(defaultTenant);
                tenantContextService.setTenantContext(defaultTenant);
            }
            return context.proceed();
        } finally {
            try { ThreadLocalStorage.clear(); } catch (Exception ignored) {}
        }
    }
}
