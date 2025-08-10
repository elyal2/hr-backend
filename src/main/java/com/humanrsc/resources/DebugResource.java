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
