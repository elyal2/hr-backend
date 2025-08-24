package com.humanrsc.resources;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.entities.User;
import com.humanrsc.services.UserService;
import com.humanrsc.services.TenantService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.logging.Log;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.humanrsc.services.TenantContextService;
import com.humanrsc.security.JWTSecured;
import com.humanrsc.config.ConnectionPoolIntercepted;
import jakarta.annotation.security.RolesAllowed;
import static com.humanrsc.security.Permissions.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
@JWTSecured
@ConnectionPoolIntercepted
public class DebugResource {

    
    
    @Inject SecurityIdentity identity;
    @Inject JsonWebToken jwt; // claims ya validados (si hay token)
    @Inject TenantContextService tenantContextService;
    @Inject UserService userService;
    @Inject TenantService tenantService;

    @GET @Path("/me")
    @Authenticated
    public Response me() {
        Map<String, Object> out = new LinkedHashMap<>();
        
        try {
            // Información básica del usuario autenticado
            out.put("principal", safe(() -> identity.getPrincipal().getName()));
            out.put("roles", identity.getRoles());
            out.put("claims", jwtClaims(jwt));
            
            // Información del tenant actual
            String currentTenant = ThreadLocalStorage.getTenantID();
            out.put("current_tenant", currentTenant);
            
            // Crear o actualizar usuario basado en JWT
            User user = userService.createOrUpdateUserFromJWT();
            if (user != null) {
                Map<String, Object> userInfo = new LinkedHashMap<>();
                userInfo.put("id", user.getObjectID().getId());
                userInfo.put("email", user.getEmail());
                userInfo.put("firstName", user.getFirstName());
                userInfo.put("lastName", user.getLastName());
                userInfo.put("status", user.getStatus());
                userInfo.put("roles", user.getRoles());
                userInfo.put("dateCreated", user.getDateCreated());
                userInfo.put("lastLogin", user.getLastLogin());
                
                out.put("user", userInfo);
                out.put("message", "User created/updated successfully");
            }
            
            // Información del tenant
            tenantService.getCurrentTenant().ifPresent(tenant -> {
                Map<String, Object> tenantInfo = new LinkedHashMap<>();
                tenantInfo.put("id", tenant.getObjectID().getId());
                tenantInfo.put("name", tenant.getName());
                tenantInfo.put("domain", tenant.getDomain());
                tenantInfo.put("status", tenant.getStatus());
                tenantInfo.put("subscriptionPlan", tenant.getSubscriptionPlan());
                tenantInfo.put("maxUsers", tenant.getMaxUsers());
                
                out.put("tenant", tenantInfo);
            });
            
            return Response.ok(out).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Error in /me endpoint: %s", e.getMessage());
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            error.put("principal", safe(() -> identity.getPrincipal().getName()));
            error.put("current_tenant", ThreadLocalStorage.getTenantID());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(error)
                         .build();
        }
    }

    @GET @Path("/token")
    @RolesAllowed({AUDIT_READ})
    public Response token() {
        AccessTokenCredential cred = identity.getCredential(AccessTokenCredential.class);
        String raw = cred != null ? cred.getToken() : null;
        if (raw == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "No token found in context")).build();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("raw", raw);
        out.put("header", decodePart(raw, 0));
        out.put("payload", decodePart(raw, 1));
        return Response.ok(out).build();
    }

    @GET @Path("/routes")
    @RolesAllowed({AUDIT_READ})
    public Response routes() {
        try {
            // Obtener información sobre las rutas disponibles
            Map<String, Object> routes = new LinkedHashMap<>();
            
            // Información básica de la aplicación
            routes.put("application", "hr-backend");
            routes.put("timestamp", new Date());
            
            // Rutas principales documentadas
            Map<String, Object> mainRoutes = new LinkedHashMap<>();
            
            // Organization routes
            Map<String, Object> orgRoutes = new LinkedHashMap<>();
            orgRoutes.put("base", "/api/organization");
            orgRoutes.put("position-categories", "/api/organization/position-categories");
            orgRoutes.put("units", "/api/organization/units");
            orgRoutes.put("positions", "/api/organization/positions");
            orgRoutes.put("employees", "/api/organization/employees");
            orgRoutes.put("assignments", "/api/organization/assignments");
            orgRoutes.put("replacements", "/api/organization/replacements");
            orgRoutes.put("salary-history", "/api/organization/salary-history");
            orgRoutes.put("stats", "/api/organization/stats");
            orgRoutes.put("chart", "/api/organization/chart");
            mainRoutes.put("organization", orgRoutes);
            
            // User routes
            Map<String, Object> userRoutes = new LinkedHashMap<>();
            userRoutes.put("base", "/users");
            userRoutes.put("by-id", "/users/{id}");
            userRoutes.put("by-email", "/users/email/{email}");
            userRoutes.put("by-status", "/users/status/{status}");
            userRoutes.put("by-role", "/users/role/{role}");
            userRoutes.put("count", "/users/count");
            mainRoutes.put("users", userRoutes);
            
            // Tenant routes
            Map<String, Object> tenantRoutes = new LinkedHashMap<>();
            tenantRoutes.put("base", "/tenants");
            tenantRoutes.put("by-id", "/tenants/{id}");
            tenantRoutes.put("by-domain", "/tenants/domain/{domain}");
            tenantRoutes.put("current", "/tenants/current");
            tenantRoutes.put("by-status", "/tenants/status/{status}");
            tenantRoutes.put("by-plan", "/tenants/plan/{plan}");
            tenantRoutes.put("stats", "/tenants/{id}/stats");
            mainRoutes.put("tenants", tenantRoutes);
            
            // Debug routes
            Map<String, Object> debugRoutes = new LinkedHashMap<>();
            debugRoutes.put("me", "/debug/me");
            debugRoutes.put("token", "/debug/token");
            debugRoutes.put("routes", "/debug/routes");
            mainRoutes.put("debug", debugRoutes);
            
            routes.put("available_routes", mainRoutes);
            
            // Información del tenant actual
            String currentTenant = ThreadLocalStorage.getTenantID();
            routes.put("current_tenant", currentTenant);
            
            return Response.ok(routes).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Error in /routes endpoint: %s", e.getMessage());
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(error)
                         .build();
        }
    }

    @GET @Path("/health")
    public Response health() {
        Map<String, Object> health = new LinkedHashMap<>();
        
        try {
            // Estado básico de la aplicación
            health.put("status", "UP");
            health.put("timestamp", new Date());
            health.put("application", "hr-backend");
            
            // Verificar tenant context
            String currentTenant = ThreadLocalStorage.getTenantID();
            health.put("tenant_context", currentTenant != null ? "OK" : "MISSING");
            health.put("current_tenant", currentTenant);
            
            // Verificar autenticación
            boolean isAuthenticated = identity != null && identity.getPrincipal() != null;
            health.put("authentication", isAuthenticated ? "OK" : "NOT_AUTHENTICATED");
            
            if (isAuthenticated) {
                health.put("principal", identity.getPrincipal().getName());
                health.put("roles", new ArrayList<>(identity.getRoles()));
            }
            
            // Verificar servicios básicos
            Map<String, Object> services = new LinkedHashMap<>();
            
            try {
                tenantService.getCurrentTenant();
                services.put("tenant_service", "OK");
            } catch (Exception e) {
                services.put("tenant_service", "ERROR: " + e.getMessage());
            }
            
            try {
                userService.countUsers();
                services.put("user_service", "OK");
            } catch (Exception e) {
                services.put("user_service", "ERROR: " + e.getMessage());
            }
            
            health.put("services", services);
            
            return Response.ok(health).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Error in health check: %s", e.getMessage());
            
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                         .entity(health)
                         .build();
        }
    }

    // --- helpers ---
    private Map<String, Object> jwtClaims(JsonWebToken jwt) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (jwt == null) return m;
        jwt.getClaimNames().forEach(n -> m.put(n, jwt.getClaim(n)));
        return m;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> decodePart(String token, int idx) {
        try {
            String[] parts = token.split("\\.");
            String json = new String(Base64.getUrlDecoder().decode(parts[idx]), StandardCharsets.UTF_8);
            return new LinkedHashMap<>(io.vertx.core.json.Json.decodeValue(json, Map.class));
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object safe(SupplierWithEx<String> s) {
        try { return s.get(); } catch (Exception e) { return null; }
    }
    
    @FunctionalInterface interface SupplierWithEx<T> { T get() throws Exception; }
}
