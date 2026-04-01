package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification entity for storing user notifications about organizational events.
 * 
 * Notifications are NOT audited (@Audited is deliberately excluded) due to high volume
 * and transient nature of this data. Audit history would create unnecessary overhead
 * for data that is primarily informational and time-sensitive.
 * 
 * Row-Level Security (RLS) ensures tenant isolation at the database level.
 */
@Entity
@Table(name = "notifications", schema = "hr_app")
@Getter
@Setter
public class Notification {

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Type of entity this notification references (e.g., "Employee", "SalaryHistory", "EmployeeAssignment")
     */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /**
     * ID of the entity this notification references
     */
    @Column(name = "entity_id")
    private String entityId;

    @NotNull
    @Column(name = "read", nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (read == null) {
            read = false;
        }
    }

    /**
     * Mark this notification as read
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Check if this notification is unread
     */
    public boolean isUnread() {
        return !read;
    }
}
