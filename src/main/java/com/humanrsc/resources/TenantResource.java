package com.humanrsc.resources;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Tenant;
import com.humanrsc.services.TenantService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import com.humanrsc.security.JWTSecured;
import com.humanrsc.config.ConnectionPoolIntercepted;
import jakarta.annotation.security.RolesAllowed;
import static com.humanrsc.security.Permissions.*;

import java.util.List;
import static com.humanrsc.config.ConfigDefaults.DEFAULT_PAGE;
import static com.humanrsc.config.ConfigDefaults.DEFAULT_SIZE;
import java.util.Map;
import java.util.Optional;

@Path("/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTSecured
@ConnectionPoolIntercepted
public class TenantResource {

    

    @Inject
    TenantService tenantService;

    @GET
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getAllTenants(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        try {
            List<Tenant> tenants = (page != null || size != null)
                    ? tenantService.findAllTenants(page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : tenantService.findAllTenants();
            return Response.ok(tenants).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting all tenants: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getTenantById(@PathParam("id") String id) {
        try {
            Optional<Tenant> tenant = tenantService.findById(id);
            if (tenant.isPresent()) {
                return Response.ok(tenant.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenant by ID %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/domain/{domain}")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getTenantByDomain(@PathParam("domain") String domain) {
        try {
            Optional<Tenant> tenant = tenantService.findByDomain(domain);
            if (tenant.isPresent()) {
                return Response.ok(tenant.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenant by domain %s: %s", domain, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @POST
    @RolesAllowed({TENANT_MANAGE})
    public Response createTenant(Tenant tenant) {
        try {
            Tenant createdTenant = tenantService.createTenant(tenant);
            return Response.status(Response.Status.CREATED)
                         .entity(createdTenant)
                         .build();
        } catch (Exception e) {
            Log.errorf(e, "Error creating tenant: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({TENANT_MANAGE, SETTINGS_MANAGE})
    public Response updateTenant(@PathParam("id") String id, Tenant tenant) {
        try {
            // Verificar que el tenant existe
            Optional<Tenant> existingTenant = tenantService.findById(id);
            if (existingTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
            
            // Asegurar que el ObjectID no cambie
            tenant.setObjectID(existingTenant.get().getObjectID());
            
            Tenant updatedTenant = tenantService.updateTenant(tenant);
            return Response.ok(updatedTenant).build();
        } catch (Exception e) {
            Log.errorf(e, "Error updating tenant %s: %s", id, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}/activate")
    @RolesAllowed({TENANT_MANAGE})
    public Response activateTenant(@PathParam("id") String id) {
        try {
            Optional<Tenant> existingTenant = tenantService.findById(id);
            if (existingTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
            
            ObjectID objectID = existingTenant.get().getObjectID();
            boolean activated = tenantService.activateTenant(objectID);
            if (activated) {
                return Response.ok(Map.of("message", "Tenant activated successfully")).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity(Map.of("error", "Failed to activate tenant"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error activating tenant %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}/suspend")
    @RolesAllowed({TENANT_MANAGE})
    public Response suspendTenant(@PathParam("id") String id) {
        try {
            Optional<Tenant> existingTenant = tenantService.findById(id);
            if (existingTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
            
            ObjectID objectID = existingTenant.get().getObjectID();
            boolean suspended = tenantService.suspendTenant(objectID);
            if (suspended) {
                return Response.ok(Map.of("message", "Tenant suspended successfully")).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity(Map.of("error", "Failed to suspend tenant"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error suspending tenant %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}/deactivate")
    @RolesAllowed({TENANT_MANAGE})
    public Response deactivateTenant(@PathParam("id") String id) {
        try {
            Optional<Tenant> existingTenant = tenantService.findById(id);
            if (existingTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
            
            ObjectID objectID = existingTenant.get().getObjectID();
            boolean deactivated = tenantService.deactivateTenant(objectID);
            if (deactivated) {
                return Response.ok(Map.of("message", "Tenant deactivated successfully")).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity(Map.of("error", "Failed to deactivate tenant"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error deactivating tenant %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/status/{status}")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getTenantsByStatus(@PathParam("status") String status,
                                       @QueryParam("page") Integer page,
                                       @QueryParam("size") Integer size) {
        try {
            List<Tenant> tenants = (page != null || size != null)
                    ? tenantService.findByStatus(status, page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : tenantService.findByStatus(status);
            return Response.ok(tenants).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenants by status %s: %s", status, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/plan/{plan}")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getTenantsBySubscriptionPlan(@PathParam("plan") String plan,
                                                 @QueryParam("page") Integer page,
                                                 @QueryParam("size") Integer size) {
        try {
            List<Tenant> tenants = (page != null || size != null)
                    ? tenantService.findBySubscriptionPlan(plan, page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : tenantService.findBySubscriptionPlan(plan);
            return Response.ok(tenants).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenants by subscription plan %s: %s", plan, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/{id}/stats")
    @RolesAllowed({TENANT_MANAGE, STATS_READ})
    public Response getTenantStats(@PathParam("id") String id) {
        try {
            Optional<Tenant> existingTenant = tenantService.findById(id);
            if (existingTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Tenant not found"))
                             .build();
            }
            
            ObjectID objectID = existingTenant.get().getObjectID();
            TenantService.TenantStats stats = tenantService.getTenantStats(objectID);
            return Response.ok(stats).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenant stats for %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/current")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getCurrentTenant() {
        try {
            Optional<Tenant> currentTenant = tenantService.getCurrentTenant();
            if (currentTenant.isPresent()) {
                return Response.ok(currentTenant.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Current tenant not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting current tenant: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/current/active")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response isCurrentTenantActive() {
        try {
            boolean isActive = tenantService.isCurrentTenantActive();
            return Response.ok(Map.of("active", isActive)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error checking if current tenant is active: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/current/can-create-users")
    @RolesAllowed({TENANT_MANAGE, READ_USERS, SETTINGS_MANAGE})
    public Response canCreateMoreUsers() {
        try {
            boolean canCreate = tenantService.canCreateMoreUsers();
            return Response.ok(Map.of("canCreateMoreUsers", canCreate)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error checking if tenant can create more users: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/count/status/{status}")
    @RolesAllowed({TENANT_MANAGE, READ_USERS})
    public Response getTenantCountByStatus(@PathParam("status") String status) {
        try {
            long count = tenantService.countByStatus(status);
            return Response.ok(Map.of("count", count)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting tenant count by status %s: %s", status, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }
}
