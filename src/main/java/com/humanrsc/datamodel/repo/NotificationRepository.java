package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entity.
 * 
 * RLS (Row Level Security) automatically filters notifications by tenant,
 * so no explicit tenant_id filtering is needed in queries.
 */
@ApplicationScoped
public class NotificationRepository implements PanacheRepositoryBase<Notification, ObjectID> {

    /**
     * Find notifications for a specific user with pagination, sorted by creation date (newest first)
     * 
     * @param userId the user ID to filter notifications
     * @param page page number (0-indexed)
     * @param size number of results per page
     * @return list of notifications for the user
     */
    public List<Notification> findByUser(String userId, int page, int size) {
        return find("userId = ?1", Sort.by("createdAt").descending(), userId)
                .page(Page.of(page, size))
                .list();
    }

    /**
     * Find all unread notifications for a specific user, sorted by creation date (newest first)
     * 
     * @param userId the user ID to filter notifications
     * @return list of unread notifications
     */
    public List<Notification> findUnreadByUser(String userId) {
        return find("userId = ?1 and read = false", Sort.by("createdAt").descending(), userId)
                .list();
    }

    /**
     * Count unread notifications for a specific user
     * 
     * @param userId the user ID
     * @return count of unread notifications
     */
    public long countUnreadByUser(String userId) {
        return count("userId = ?1 and read = false", userId);
    }

    /**
     * Find a notification by its ObjectID
     * 
     * @param objectID the composite ID (id + tenant_id)
     * @return optional notification
     */
    public Optional<Notification> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    /**
     * Mark a notification as read
     * 
     * @param objectID the notification's composite ID
     * @return true if the notification was found and updated
     */
    @Transactional
    public boolean markAsRead(ObjectID objectID) {
        return update("read = true, readAt = ?1 where objectID = ?2", 
                     LocalDateTime.now(), objectID) > 0;
    }

    /**
     * Mark all notifications for a user as read
     * 
     * @param userId the user ID
     * @return number of notifications marked as read
     */
    @Transactional
    public int markAllAsRead(String userId) {
        return update("read = true, readAt = ?1 where userId = ?2 and read = false", 
                     LocalDateTime.now(), userId);
    }

    /**
     * Delete notifications older than a specific date (for cleanup jobs)
     * 
     * @param beforeDate delete notifications created before this date
     * @return number of deleted notifications
     */
    @Transactional
    public long deleteOlderThan(LocalDateTime beforeDate) {
        return delete("createdAt < ?1", beforeDate);
    }

    /**
     * Find notifications related to a specific entity
     * 
     * @param entityType type of entity (e.g., "Employee", "SalaryHistory")
     * @param entityId ID of the entity
     * @return list of notifications related to the entity
     */
    public List<Notification> findByEntity(String entityType, String entityId) {
        return find("entityType = ?1 and entityId = ?2", 
                   Sort.by("createdAt").descending(), 
                   entityType, entityId)
                .list();
    }

    /**
     * Count total notifications for a user
     * 
     * @param userId the user ID
     * @return total count of notifications
     */
    public long countByUser(String userId) {
        return count("userId = ?1", userId);
    }
}
