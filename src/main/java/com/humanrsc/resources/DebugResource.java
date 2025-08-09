package com.humanrsc.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import org.jboss.logging.Logger;
import io.quarkus.oidc.AccessTokenCredential;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.humanrsc.services.TenantContextService;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    private static final Logger LOG = Logger.getLogger(DebugResource.class);
    
    @Inject SecurityIdentity identity;
    @Inject JsonWebToken jwt; // claims ya validados (si hay token)
    @Inject TenantContextService tenantContextService;


    @GET @Path("/me")
    public Response me() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("principal", safe(() -> identity.getPrincipal().getName()));
        out.put("roles", identity.getRoles()); // aquí verás tus permissions mapeados
        out.put("claims", jwtClaims(jwt));
        return Response.ok(out).build();
    }

    @GET @Path("/token")
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
