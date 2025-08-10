package com.humanrsc.resources;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.User;
import com.humanrsc.services.UserService;
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

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTSecured
@ConnectionPoolIntercepted
public class UserResource {

    

    @Inject
    UserService userService;

    @GET
    @RolesAllowed({READ_USERS})
    public Response getAllUsers(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        try {
            List<User> users = (page != null || size != null)
                    ? userService.findAllUsers(page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : userService.findAllUsers();
            return Response.ok(users).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting all users: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({READ_USERS})
    public Response getUserById(@PathParam("id") String id) {
        try {
            String tenantID = ThreadLocalStorage.getTenantID();
            ObjectID objectID = ObjectID.of(id, tenantID);
            
            Optional<User> user = userService.findById(objectID);
            if (user.isPresent()) {
                return Response.ok(user.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting user by ID %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/email/{email}")
    @RolesAllowed({READ_USERS})
    public Response getUserByEmail(@PathParam("email") String email) {
        try {
            Optional<User> user = userService.findByEmail(email);
            if (user.isPresent()) {
                return Response.ok(user.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting user by email %s: %s", email, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @POST
    @RolesAllowed({WRITE_USERS})
    public Response createUser(User user) {
        try {
            User createdUser = userService.createUser(user);
            return Response.status(Response.Status.CREATED)
                         .entity(createdUser)
                         .build();
        } catch (Exception e) {
            Log.errorf(e, "Error creating user: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({WRITE_USERS})
    public Response updateUser(@PathParam("id") String id, User user) {
        try {
            String tenantID = ThreadLocalStorage.getTenantID();
            ObjectID objectID = ObjectID.of(id, tenantID);
            
            // Verificar que el usuario existe
            Optional<User> existingUser = userService.findById(objectID);
            if (existingUser.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
            
            // Asegurar que el ObjectID no cambie
            user.setObjectID(objectID);
            
            User updatedUser = userService.updateUser(user);
            return Response.ok(updatedUser).build();
        } catch (Exception e) {
            Log.errorf(e, "Error updating user %s: %s", id, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({WRITE_USERS})
    public Response deleteUser(@PathParam("id") String id) {
        try {
            String tenantID = ThreadLocalStorage.getTenantID();
            ObjectID objectID = ObjectID.of(id, tenantID);
            
            boolean deleted = userService.deleteUser(objectID);
            if (deleted) {
                return Response.ok(Map.of("message", "User deleted successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error deleting user %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}/activate")
    @RolesAllowed({WRITE_USERS})
    public Response activateUser(@PathParam("id") String id) {
        try {
            String tenantID = ThreadLocalStorage.getTenantID();
            ObjectID objectID = ObjectID.of(id, tenantID);
            
            boolean activated = userService.activateUser(objectID);
            if (activated) {
                return Response.ok(Map.of("message", "User activated successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error activating user %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @PUT
    @Path("/{id}/suspend")
    @RolesAllowed({WRITE_USERS})
    public Response suspendUser(@PathParam("id") String id) {
        try {
            String tenantID = ThreadLocalStorage.getTenantID();
            ObjectID objectID = ObjectID.of(id, tenantID);
            
            boolean suspended = userService.suspendUser(objectID);
            if (suspended) {
                return Response.ok(Map.of("message", "User suspended successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "User not found"))
                             .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error suspending user %s: %s", id, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/status/{status}")
    @RolesAllowed({READ_USERS})
    public Response getUsersByStatus(@PathParam("status") String status,
                                     @QueryParam("page") Integer page,
                                     @QueryParam("size") Integer size) {
        try {
            List<User> users = (page != null || size != null)
                    ? userService.findByStatus(status, page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : userService.findByStatus(status);
            return Response.ok(users).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting users by status %s: %s", status, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/role/{role}")
    @RolesAllowed({READ_USERS})
    public Response getUsersByRole(@PathParam("role") String role,
                                   @QueryParam("page") Integer page,
                                   @QueryParam("size") Integer size) {
        try {
            List<User> users = (page != null || size != null)
                    ? userService.findByRole(role, page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE)
                    : userService.findByRole(role);
            return Response.ok(users).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting users by role %s: %s", role, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }

    @GET
    @Path("/count")
    @RolesAllowed({READ_USERS})
    public Response getUserCount() {
        try {
            long count = userService.countUsers();
            return Response.ok(Map.of("count", count)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting user count: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }
}
