package com.humanrsc.services;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Notification;
import com.humanrsc.datamodel.enums.NotificationType;
import com.humanrsc.datamodel.repo.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user notifications.
 * 
 * Notifications are created automatically by the system when significant
 * organizational events occur (employee hiring, termination, salary changes, etc.)
 * and are delivered to relevant stakeholders (typically managers and HR staff).
 */
@ApplicationScoped
public class NotificationService {

    @Inject
    NotificationRepository notificationRepository;

    /**
     * Create a new notification for a user
     * 
     * @param userId ID of the user who should receive the notification
     * @param type type of notification (event that triggered it)
     * @param title short notification title
     * @param message detailed notification message
     * @param entityType optional type of related entity (e.g., "Employee", "SalaryHistory")
     * @param entityId optional ID of related entity
     * @return the created notification
     */
    @Transactional
    public Notification createNotification(
            String userId,
            NotificationType type,
            String title,
            String message,
            String entityType,
            String entityId) {
        
        Notification notification = new Notification();
        
        String id = UUID.randomUUID().toString();
        String tenantId = ThreadLocalStorage.getTenantID();
        notification.setObjectID(ObjectID.of(id, tenantId));
        
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.persist(notification);
        return notification;
    }

    /**
     * Create a notification without entity reference
     */
    @Transactional
    public Notification createNotification(
            String userId,
            NotificationType type,
            String title,
            String message) {
        return createNotification(userId, type, title, message, null, null);
    }

    /**
     * Get paginated notifications for a user
     * 
     * @param userId user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of notifications
     */
    public List<Notification> getUserNotifications(String userId, int page, int size) {
        return notificationRepository.findByUser(userId, page, size);
    }

    /**
     * Get all unread notifications for a user
     * 
     * @param userId user ID
     * @return list of unread notifications
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findUnreadByUser(userId);
    }

    /**
     * Get count of unread notifications for a user
     * 
     * @param userId user ID
     * @return count of unread notifications
     */
    public long getUnreadCount(String userId) {
        return notificationRepository.countUnreadByUser(userId);
    }

    /**
     * Mark a notification as read
     * 
     * @param notificationId notification ID (not composite ObjectID)
     * @return true if notification was found and marked as read
     */
    @Transactional
    public boolean markAsRead(String notificationId) {
        String tenantId = ThreadLocalStorage.getTenantID();
        ObjectID objectID = ObjectID.of(notificationId, tenantId);
        
        Optional<Notification> notification = notificationRepository.findByObjectID(objectID);
        if (notification.isPresent()) {
            notification.get().markAsRead();
            return true;
        }
        return false;
    }

    /**
     * Mark all notifications for a user as read
     * 
     * @param userId user ID
     * @return number of notifications marked as read
     */
    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /**
     * Get a specific notification by ID
     * 
     * @param notificationId notification ID
     * @return optional notification
     */
    public Optional<Notification> getNotificationById(String notificationId) {
        String tenantId = ThreadLocalStorage.getTenantID();
        ObjectID objectID = ObjectID.of(notificationId, tenantId);
        return notificationRepository.findByObjectID(objectID);
    }

    /**
     * Get notifications related to a specific entity
     * 
     * @param entityType type of entity (e.g., "Employee")
     * @param entityId ID of the entity
     * @return list of related notifications
     */
    public List<Notification> getNotificationsByEntity(String entityType, String entityId) {
        return notificationRepository.findByEntity(entityType, entityId);
    }

    /**
     * Delete old notifications (cleanup job)
     * 
     * @param daysToKeep keep notifications newer than this many days
     * @return number of deleted notifications
     */
    @Transactional
    public long deleteOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        return notificationRepository.deleteOlderThan(cutoffDate);
    }

    /**
     * Get total notification count for a user
     * 
     * @param userId user ID
     * @return total count
     */
    public long getTotalCount(String userId) {
        return notificationRepository.countByUser(userId);
    }
}
