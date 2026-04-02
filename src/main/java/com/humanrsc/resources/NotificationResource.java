package com.humanrsc.resources;

import com.humanrsc.datamodel.entities.Notification;
import com.humanrsc.security.JWTSecured;
import com.humanrsc.security.JwtTokenUtils;
import com.humanrsc.services.NotificationService;
import com.humanrsc.config.ConnectionPoolIntercepted;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import io.quarkus.logging.Log;

import java.util.List;
import java.util.Optional;

import static com.humanrsc.security.Permissions.*;

/**
 * REST API for user notifications.
 * 
 * Endpoints allow users to:
 * - Retrieve their notifications (all or unread)
 * - Get unread count
 * - Mark notifications as read (single or all)
 * 
 * All endpoints are scoped to the authenticated user (from JWT subject).
 */
@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTSecured
@ConnectionPoolIntercepted
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @Inject
    JwtTokenUtils jwtTokenUtils;

    /**
     * Get all notifications for the current user (paginated)
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20, max: 100)
     * @return list of notifications
     */
    @GET
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response getAllNotifications(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            // Validate pagination params
            if (page < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Validation error", "Page must be >= 0"))
                        .build();
            }
            if (size <= 0 || size > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Validation error", "Size must be between 1 and 100"))
                        .build();
            }

            List<Notification> notifications = notificationService.getUserNotifications(userId, page, size);
            long totalCount = notificationService.getTotalCount(userId);
            
            return Response.ok(new PaginatedResponse<>(notifications, page, size, totalCount)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting notifications");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to retrieve notifications"))
                    .build();
        }
    }

    /**
     * Get only unread notifications for the current user
     * 
     * @return list of unread notifications
     */
    @GET
    @Path("/unread")
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response getUnreadNotifications() {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return Response.ok(notifications).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting unread notifications");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to retrieve unread notifications"))
                    .build();
        }
    }

    /**
     * Get count of unread notifications for the current user
     * 
     * @return count of unread notifications
     */
    @GET
    @Path("/unread/count")
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response getUnreadCount() {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            long count = notificationService.getUnreadCount(userId);
            return Response.ok(new CountResponse(count)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting unread count");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to retrieve unread count"))
                    .build();
        }
    }

    /**
     * Get a specific notification by ID
     * 
     * @param id notification ID
     * @return notification details
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response getNotificationById(@PathParam("id") String id) {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            Optional<Notification> notification = notificationService.getNotificationById(id);
            if (notification.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Not found", "Notification not found"))
                        .build();
            }

            // Verify the notification belongs to the requesting user
            if (!notification.get().getUserId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Access denied", "Cannot access other users' notifications"))
                        .build();
            }

            return Response.ok(notification.get()).build();
        } catch (Exception e) {
            Log.errorf(e, "Error getting notification by ID: %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to retrieve notification"))
                    .build();
        }
    }

    /**
     * Mark a specific notification as read
     * 
     * @param id notification ID
     * @return success response
     */
    @PUT
    @Path("/{id}/read")
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response markAsRead(@PathParam("id") String id) {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            // First verify the notification exists and belongs to user
            Optional<Notification> notification = notificationService.getNotificationById(id);
            if (notification.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Not found", "Notification not found"))
                        .build();
            }

            if (!notification.get().getUserId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Access denied", "Cannot modify other users' notifications"))
                        .build();
            }

            boolean success = notificationService.markAsRead(id);
            if (success) {
                return Response.ok(new MessageResponse("Notification marked as read")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Not found", "Notification not found"))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error marking notification as read: %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to mark notification as read"))
                    .build();
        }
    }

    /**
     * Mark all notifications as read for the current user
     * 
     * @return count of notifications marked as read
     */
    @PUT
    @Path("/read-all")
    @RolesAllowed({NOTIFICATIONS_READ})
    public Response markAllAsRead() {
        try {
            String userId = jwtTokenUtils.extractSubjectFromJWT();
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Authentication error", "User ID not found in token"))
                        .build();
            }

            int count = notificationService.markAllAsRead(userId);
            return Response.ok(new MarkAllReadResponse(count, 
                    String.format("Marked %d notification(s) as read", count))).build();
        } catch (Exception e) {
            Log.errorf(e, "Error marking all notifications as read");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error", "Failed to mark all notifications as read"))
                    .build();
        }
    }

    // ========== RESPONSE DTOs ==========

    /**
     * Generic error response
     */
    public static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    /**
     * Generic message response
     */
    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    /**
     * Count response
     */
    public static class CountResponse {
        public long count;

        public CountResponse(long count) {
            this.count = count;
        }
    }

    /**
     * Mark all read response
     */
    public static class MarkAllReadResponse {
        public int count;
        public String message;

        public MarkAllReadResponse(int count, String message) {
            this.count = count;
            this.message = message;
        }
    }

    /**
     * Paginated response wrapper
     */
    public static class PaginatedResponse<T> {
        public List<T> data;
        public int page;
        public int size;
        public long totalCount;
        public int totalPages;

        public PaginatedResponse(List<T> data, int page, int size, long totalCount) {
            this.data = data;
            this.page = page;
            this.size = size;
            this.totalCount = totalCount;
            this.totalPages = (int) Math.ceil((double) totalCount / size);
        }
    }
}
