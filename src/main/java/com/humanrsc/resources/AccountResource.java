package com.humanrsc.resources;

import com.humanrsc.datamodel.entities.Account;
import com.humanrsc.services.AccountService;
import com.humanrsc.services.TenantContextService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    private static final Logger LOG = Logger.getLogger(AccountResource.class);

    @Inject
    AccountService accountService;

    @Inject
    TenantContextService tenantContextService;

    @Inject
    JsonWebToken jwt;

    /**
     * Obtener todas las cuentas del tenant actual
     */
    @GET
    @RolesAllowed({"read:accounts"})
    public Response getAllAccounts(@Context SecurityContext securityContext) {
        try {
            String tenantId = extractTenantFromJWT();
            tenantContextService.setTenantContext(tenantId);
            
            List<Account> accounts = accountService.getAllAccounts();
            
            LOG.infof("Retrieved %d accounts for tenant: %s", accounts.size(), tenantId);
            
            return Response.ok(accounts).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving accounts");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error retrieving accounts: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtener cuenta por ID
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"read:accounts"})
    public Response getAccountById(@PathParam("id") String id) {
        try {
            String tenantId = extractTenantFromJWT();
            tenantContextService.setTenantContext(tenantId);
            
            return accountService.findById(id, tenantId)
                .map(account -> Response.ok(account).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Account not found: " + id))
                    .build());
                    
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving account: %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error retrieving account: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Crear nueva cuenta
     */
    @POST
    @RolesAllowed({"write:accounts"})
    public Response createAccount(@Valid CreateAccountRequest request) {
        try {
            String tenantId = extractTenantFromJWT();
            tenantContextService.setTenantContext(tenantId);
            
            Account account = accountService.createAccount(
                request.name,
                request.surname,
                request.email,
                tenantId
            );
            
            LOG.infof("Created account: %s for tenant: %s", account.getObjectID().getId(), tenantId);
            
            return Response.status(Response.Status.CREATED)
                .entity(account)
                .build();
                
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Invalid data: " + e.getMessage()))
                .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error creating account");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error creating account: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Actualizar status de cuenta
     */
    @PUT
    @Path("/{id}/status")
    @RolesAllowed({"write:accounts"})
    public Response updateAccountStatus(@PathParam("id") String id, @Valid UpdateStatusRequest request) {
        try {
            String tenantId = extractTenantFromJWT();
            tenantContextService.setTenantContext(tenantId);
            
            Account account = accountService.updateStatus(id, tenantId, request.status);
            
            return Response.ok(account).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Account not found: " + id))
                .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error updating account status: %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error updating account: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtener estadísticas de cuentas
     */
    @GET
    @Path("/stats")
    @RolesAllowed({"read:accounts", "read:stats"})
    public Response getAccountStats() {
        try {
            String tenantId = extractTenantFromJWT();
            tenantContextService.setTenantContext(tenantId);
            
            AccountService.AccountStats stats = accountService.getAccountStats();
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving account stats");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error retrieving stats: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Extrae el tenant del JWT
     * Por ahora usamos un claim custom o fallback al default
     */
    private String extractTenantFromJWT() {
        // Intentar obtener tenant del JWT
        String tenantId = jwt.getClaim("tenant");
        
        if (tenantId == null || tenantId.isEmpty()) {
            // Fallback: usar el dominio del email o default
            String email = jwt.getClaim("email");
            if (email != null && email.contains("@")) {
                String domain = email.substring(email.indexOf("@") + 1);
                tenantId = domain.replace(".", "-");
            } else {
                tenantId = "demo-tenant"; // Default para testing
            }
        }
        
        return tenantId;
    }

    // DTOs para requests y responses
    public static class SimpleResponse {
        public String message;
        
        public SimpleResponse(String message) {
            this.message = message;
        }
    }

    public static class AuthenticatedResponse {
        public String message;
        public String userEmail;
        public String tenantId;
        public String principal;
        public long timestamp;
        
        public AuthenticatedResponse(String message, String userEmail, String tenantId, String principal) {
            this.message = message;
            this.userEmail = userEmail;
            this.tenantId = tenantId;
            this.principal = principal;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class CreateAccountRequest {
        public String name;
        public String surname;
        public String email;
    }

    public static class UpdateStatusRequest {
        public Integer status;
    }

    public static class ErrorResponse {
        public String error;
        public long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}